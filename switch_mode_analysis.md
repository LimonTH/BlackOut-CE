# Анализ проблем SwitchMode

## Обнаруженные проблемы

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

**В InvUtils.swap():**
```java
public static void swap(int to) {
    prevSlot = BlackOut.mc.player.getInventory().selectedSlot;
    BlackOut.mc.player.getInventory().selectedSlot = to;
    syncSlot(false);
}
```

**В InvUtils.syncSlot():**
```java
private static void syncSlot(boolean instant) {
    int i = BlackOut.mc.player.getInventory().selectedSlot;
    if (i != Managers.PACKET.slot) {
        if (instant) {
            Managers.PACKET.sendInstantly(new UpdateSelectedSlotC2SPacket(i));
        } else {
            Managers.PACKET.sendPacket(new UpdateSelectedSlotC2SPacket(i));
        }
    }
}
```

**Проблема:** Если `i == Managers.PACKET.slot` (из-за рассинхронизации), пакет не отправляется, но метод возвращает `true`. Модуль считает переключение успешным, хотя сервер не получил пакет.

### 3. Конфликт глобального состояния
Статические переменные в `InvUtils`:
- `public static int pickSlot = -1;`
- `public static int prevSlot = -1;`
- `private static int[] slots;`

Если несколько модулей используют SwitchMode одновременно, они перезаписывают эти переменные, вызывая непредсказуемое поведение при вызове `swapBack()`.

### 4. Проблемы с PickSilent режимом
**В InvUtils.sendPick():**
```java
if (Simulation.getInstance().pickSwitch()) {
    int hbSlot = BlackOut.mc.player.getInventory().getSwappableHotbarSlot();
    Managers.PACKET.ignoreSetSlot.replace(hbSlot, 0.3);
    // ... симуляция обмена
}
```

`ignoreSetSlot.replace(hbSlot, 0.3)` приводит к игнорированию серверных пакетов об обновлении слота `hbSlot` в течение 0.3 секунды. Это может вызвать задержки в других модулях, пытающихся переключить тот же слот.

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

**Проблема:** Условие `if (!switched && this.pickaxeSwitch.get() != SwitchMode.Disabled)` проверяет, вернул ли `swap()` false. Но `swap()` может вернуть `true` даже если пакет не был отправлен (из-за проверки в `syncSlot`). В этом случае пакет не отправляется, но `switched = true`.

### 6. Проблемы с invSwap
**В InvUtils.invSwap():**
```java
public static void invSwap(int slot) {
    clickSlot(slot, Managers.PACKET.slot, SlotActionType.SWAP);
    slots = new int[]{slot, Managers.PACKET.slot};
}
```

`Managers.PACKET.slot` может быть неактуальным, что приводит к обмену с неправильным слотом.

## Предлагаемые исправления

### Исправление 1: Улучшить синхронизацию Managers.PACKET.slot
**Изменения в PacketManager:**
- Отслеживать не только отправленные, но и подтвержденные сервером слоты
- Добавить поле `confirmedSlot`, которое обновляется при получении `UpdateSelectedSlotS2CPacket` (если пакет не отменен)

**Изменения в InvUtils.syncSlot():**
- Всегда отправлять пакет, если режим не Disabled
- Или возвращать false, если пакет не был отправлен

### Исправление 2: Изменить логику swap() и swapInstantly()
**Вариант A:** Всегда отправлять пакет для режимов Silent, Normal, InvSwitch, PickSilent
**Вариант B:** Возвращать boolean, указывающий, был ли отправлен пакет

**Предпочтительно:** Изменить `syncSlot()` чтобы он возвращал boolean:
```java
private static boolean syncSlot(boolean instant) {
    int i = BlackOut.mc.player.getInventory().selectedSlot;
    if (i != Managers.PACKET.slot) {
        // ... отправка пакета
        Managers.PACKET.slot = i; // Обновить сразу после отправки
        return true;
    }
    return false;
}
```

### Исправление 3: Исправить глобальное состояние
**Вариант A:** Сделать переменные экземплярными в новом классе `SwitchContext`
**Вариант B:** Использовать ThreadLocal для изоляции контекстов
**Вариант C:** Передать состояние через возвращаемое значение swap()

### Исправление 4: Исправить логику в AutoMine
Изменить проверку:
```java
boolean packetSent = this.pickaxeSwitch.get().swap(slot);
boolean switched = packetSent || slot == BlackOut.mc.player.getInventory().selectedSlot;
```

### Исправление 5: Улучшить invSwap
Кэшировать `Managers.PACKET.slot` перед вызовом или добавить проверку актуальности.

## Приоритеты исправлений

1. **Критично:** Исправить логику отправки пакетов в `syncSlot()` - всегда отправлять для активных режимов
2. **Высоко:** Исправить возвращаемые значения `swap()` - должен отражать факт отправки пакета
3. **Средне:** Решить проблему глобального состояния
4. **Низко:** Оптимизировать `invSwap` и `pickSwap`

## Тестовые сценарии

1. **Тест 1:** Одиночный модуль использует Silent режим
2. **Тест 2:** Два модуля одновременно используют разные SwitchMode
3. **Тест 3:** Переключение с последующим немедленным swapBack()
4. **Тест 4:** Использование с включенной симуляцией
5. **Тест 5:** Комбинация Normal и Silent режимов в разных модулях