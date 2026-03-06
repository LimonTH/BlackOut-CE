# Полный анализ проблем SwitchMode и внесенные исправления

## Выявленные проблемы

### 1. Несинхронность Managers.PACKET.slot
**Механизм:**
- `Managers.PACKET.slot` обновляется при отправке `UpdateSelectedSlotC2SPacket` в `PacketManager.onSent()`
- Серверные пакеты `UpdateSelectedSlotS2CPacket` могут быть отменены через `ignoreSetSlot.contains(packet.getSlot())`
- Если серверный пакет отменен, клиентское состояние рассинхронизируется с серверным

**Эффект:**
- `InvUtils.syncSlot()` проверяет `if (i != Managers.PACKET.slot)` и не отправляет пакет при равенстве
- Но `Managers.PACKET.slot` может быть устаревшим (не отражать реальный серверный слот)
- Результат: необходимый пакет не отправляется, но метод `swap()` возвращает `true`

### 2. Ложные успешные переключения
**В SwitchMode.swap():**
```java
public boolean swap(int slot) {
    return switch (this) {
        case Silent, Normal -> {
            InvUtils.swap(slot);
            yield true;
        }
        // ...
    };
}
```

**Проблема:** Методы `invSwap()`, `pickSwap()` и другие в `InvUtils` возвращают `boolean`, но в `SwitchMode` были обернуты в блоки `{ ... yield true; }`, что всегда возвращало `true`, даже если вызов метода возвращал `false`.

### 3. Конфликт глобального состояния
Статические переменные в `InvUtils`:
- `public static int pickSlot = -1;`
- `public static int prevSlot = -1;`
- `private static int[] slots;`

Если несколько модулей используют SwitchMode одновременно, они перезаписывают эти переменные, вызывая непредсказуемое поведение при вызове `swapBack()`.

### 4. Использование устаревшего слота в invSwap
**В оригинальном InvUtils.invSwap():**
```java
public static boolean invSwap(int slot) {
    if (slot < 0 || slot >= 36) return false;
    clickSlot(slot, Managers.PACKET.slot, SlotActionType.SWAP);
    slots = new int[]{slot, Managers.PACKET.slot};
    return true;
}
```

`Managers.PACKET.slot` может быть неактуальным, что приводит к обмену с неправильным слотом.

### 5. Некорректная логика в AutoMine
**В AutoMine.endMining():**
```java
boolean switched = false;
if (!holding && slot != -1 && slot != currentSlot) {
    switched = this.pickaxeSwitch.get().swap(slot);
    // Если swap не отправил пакет (возврат true, но пакета нет), отправляем вручную
    if (!switched && this.pickaxeSwitch.get() != SwitchMode.Disabled) {
        Managers.PACKET.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        switched = true;
    }
}
```

**Проблема:** Условие `if (!switched && this.pickaxeSwitch.get() != SwitchMode.Disabled)` проверяет, вернул ли `swap()` false. Но `swap()` мог вернуть `true` даже если пакет не был отправлен. В этом случае пакет не отправляется, но `switched = true`.

## Внесенные исправления

### 1. Исправление SwitchMode.java
**Изменения:**
- Убраны блоки `{ ... yield true; }` для методов `invSwap()`, `pickSwap()` и их instant-версий
- Теперь методы напрямую возвращают результат вызова соответствующих методов `InvUtils`

**Было:**
```java
public boolean swap(int slot) {
    return switch (this) {
        case Silent, Normal -> InvUtils.swap(slot);
        case InvSwitch -> {
            InvUtils.invSwap(slot);
            yield true; // invSwap не возвращает boolean
        }
        // ...
    };
}
```

**Стало:**
```java
public boolean swap(int slot) {
    return switch (this) {
        case Silent, Normal -> InvUtils.swap(slot);
        case InvSwitch -> InvUtils.invSwap(slot);
        case PickSilent -> InvUtils.pickSwap(slot);
        default -> false;
    };
}
```

### 2. Исправление InvUtils для использования актуального слота
**В InvUtils.invSwap() и invSwapInstantly():**
- Заменено использование `Managers.PACKET.slot` на `BlackOut.mc.player.getInventory().selectedSlot`
- Добавлено обновление `Managers.PACKET.slot`, если он рассинхронизирован

**Было:**
```java
public static boolean invSwap(int slot) {
    if (slot < 0 || slot >= 36) return false;
    clickSlot(slot, Managers.PACKET.slot, SlotActionType.SWAP);
    slots = new int[]{slot, Managers.PACKET.slot};
    return true;
}
```

**Стало:**
```java
public static boolean invSwap(int slot) {
    if (slot < 0 || slot >= 36) return false;
    // Используем текущий выбранный слот игрока
    int currentSlot = BlackOut.mc.player.getInventory().selectedSlot;
    clickSlot(slot, currentSlot, SlotActionType.SWAP);
    slots = new int[]{slot, currentSlot};
    // Обновляем Managers.PACKET.slot, если он рассинхронизирован
    if (Managers.PACKET.slot != currentSlot) {
        Managers.PACKET.slot = currentSlot;
    }
    return true;
}
```

### 3. Исправление syncSlot() в InvUtils
**Уже было исправлено ранее:**
- `syncSlot()` теперь возвращает `boolean`
- Всегда отправляет пакет при изменении слота
- Немедленно обновляет `Managers.PACKET.slot` после отправки

### 4. AutoMine уже был исправлен
Код AutoMine уже содержит правильную логику обработки возвращаемого значения `swap()`.

## Оставшиеся проблемы и рекомендации

### 1. Проблема глобальных статических переменных
**Текущее состояние:**
- `pickSlot`, `prevSlot`, `slots` - статические переменные, общие для всех модулей
- Конфликт при одновременном использовании нескольких модулей

**Рекомендации по исправлению:**

**Вариант A: Контекст на основе идентификатора модуля**
```java
public class SwitchContext {
    private static final Map<String, Context> contexts = new HashMap<>();
    
    private static class Context {
        int pickSlot = -1;
        int prevSlot = -1;
        int[] slots = null;
    }
    
    public static Context getContext(String moduleId) {
        return contexts.computeIfAbsent(moduleId, k -> new Context());
    }
}
```

**Вариант B: ThreadLocal с учетом модулей**
```java
public class InvUtils {
    private static final ThreadLocal<Map<String, Context>> threadContext = 
        ThreadLocal.withInitial(HashMap::new);
    
    public static boolean swap(String moduleId, int to) {
        Context ctx = getContext(moduleId);
        ctx.prevSlot = BlackOut.mc.player.getInventory().selectedSlot;
        // ...
    }
}
```

**Вариант C: Возвращаемый контекст**
```java
public class SwapResult {
    public final boolean success;
    public final Runnable swapBack;
    
    public static SwapResult swap(int slot, SwitchMode mode) {
        // ... выполнить swap
        return new SwapResult(true, () -> mode.swapBack());
    }
}
```

### 2. Проблема ignoreSetSlot в PickSilent режиме
**В InvUtils.sendPick():**
```java
if (Simulation.getInstance().pickSwitch()) {
    int hbSlot = BlackOut.mc.player.getInventory().getSwappableHotbarSlot();
    Managers.PACKET.ignoreSetSlot.replace(hbSlot, 0.3);
    // ...
}
```

`ignoreSetSlot.replace(hbSlot, 0.3)` приводит к игнорированию серверных пакетов об обновлении слота `hbSlot` в течение 0.3 секунды. Это может вызвать задержки в других модулях.

**Рекомендация:** Рассмотреть уменьшение времени или добавление проверки, что слот не используется другими модулями.

### 3. Улучшение логики в модулях
**Рекомендация:** Во всех модулях, использующих SwitchMode, добавить проверку:
```java
boolean packetSent = switchMode.swap(slot);
boolean actuallySwitched = packetSent || slot == BlackOut.mc.player.getInventory().selectedSlot;
```

## Тестовые сценарии

1. **Тест 1:** Одиночный модуль использует Silent режим
   - Проверить отправку `UpdateSelectedSlotC2SPacket`
   - Проверить обновление `Managers.PACKET.slot`
   - Проверить корректный `swapBack()`

2. **Тест 2:** Два модуля одновременно используют разные SwitchMode
   - AutoMine с InvSwitch и ElytraFly с Silent
   - Проверить отсутствие конфликтов в глобальных переменных

3. **Тест 3:** Переключение с последующим немедленным swapBack()
   - Проверить, что `prevSlot` корректно сбрасывается

4. **Тест 4:** Использование PickSilent с включенной симуляцией
   - Проверить работу `ignoreSetSlot`

5. **Тест 5:** Комбинация Normal и Silent режимов
   - Проверить, что Normal режим не мешает Silent

## Выводы

Основные проблемы SwitchMode были исправлены:

1. ✅ **Исправлены возвращаемые значения** - методы `swap()` теперь корректно возвращают `boolean`
2. ✅ **Исправлено использование актуального слота** - `invSwap()` использует текущий выбранный слот игрока
3. ✅ **Улучшена синхронизация** - `syncSlot()` обновляет `Managers.PACKET.slot` сразу после отправки пакета
4. ⚠️ **Проблема глобальных переменных** - требует дальнейшего рефакторинга
5. ✅ **Логика модулей** - AutoMine содержит правильную обработку возвращаемых значений

Рекомендуется провести тестирование всех режимов SwitchMode в реальных условиях и рассмотреть рефакторинг для решения проблемы глобальных переменных.