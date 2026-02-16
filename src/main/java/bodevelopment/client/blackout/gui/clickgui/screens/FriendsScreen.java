package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.FriendsManager;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.awt.*;

public class FriendsScreen extends ClickGuiScreen {
    // Выносим высоту строки в константу для удобства (было 60)
    private static final float ITEM_HEIGHT = 75.0F;
    private boolean first;

    public FriendsScreen() {
        super("Friends", 800.0F, 500.0F, true);
    }

    @Override
    protected float getLength() {
        // Учитываем новую высоту строки
        return Managers.FRIENDS.getFriends().size() * ITEM_HEIGHT + 40.0F;
    }

    @Override
    public void render() {
        // 1. Рисуем фон (уже внутри трансформации ClickGuiScreen)
        // height - 40, потому что ClickGuiScreen уже добавил 40 к общей высоте
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        // --- СПИСОК (Уже под Scissor из ClickGuiScreen.onRender) ---
        this.stack.push();

        // 10.0F - отступ от шапки. Скролл двигает список.
        this.stack.translate(0.0F, 10.0F - this.scroll.get(), 0.0F);

        this.first = true;
        if (Managers.FRIENDS.getFriends().isEmpty()) {
            BlackOut.FONT.text(this.stack, "No friends added yet", 2.0F, width / 2.0F, 30.0F, Color.GRAY, true, true);
        } else {
            Managers.FRIENDS.getFriends().forEach(this::renderFriend);
        }

        this.stack.pop();
    }

    private void renderFriend(FriendsManager.Friend friend) {
        // Линия-разделитель (рисуется ПЕРЕД другом, если он не первый)
        if (!this.first) {
            RenderUtils.line(this.stack, 20.0F, 0.0F, this.width - 20.0F, 0.0F, new Color(255, 255, 255, 12).getRGB());
        }
        this.first = false;

        this.stack.push();

        // Голова
        renderFriendHead(friend);

        // Текст (Имя и UUID)
        BlackOut.FONT.text(this.stack, friend.getName(), 2.0F, 85.0F, 22.0F, Color.WHITE, false, true);
        String subText = (friend.getUuid() != null) ? friend.getUuid().toString().substring(0, 18) + "..." : "Added manually";
        BlackOut.FONT.text(this.stack, subText, 1.3F, 85.0F, 45.0F, new Color(140, 140, 140), false, true);

        // Кнопка удаления
        // ВАЖНО: Координаты для MX/MY проверяем в ЛОКАЛЬНОМ пространстве окна (ClickGuiScreen уже все посчитал)
        // Нам нужно знать Y этой строки относительно верха контента (0.0)
        int index = Managers.FRIENDS.getFriends().indexOf(friend);
        float itemTopY = (ITEM_HEIGHT * index) + 10.0F - scroll.get(); // 10.0 - это наш translate из render()

        if (mx > width - 110 && mx < width - 10 && my > itemTopY && my < itemTopY + ITEM_HEIGHT) {
            RenderUtils.rounded(this.stack, width - 100, 20, 85, 35, 7, 0, new Color(255, 50, 50, 35).getRGB(), 0);
            BlackOut.FONT.text(this.stack, "REMOVE", 1.5F, width - 57.0F, 37.0F, Color.RED, true, true);
        }
        this.stack.pop();

        // Смещаем матрицу вниз для следующего элемента
        this.stack.translate(0.0F, ITEM_HEIGHT, 0.0F);
    }

    private void renderFriendHead(FriendsManager.Friend friend) {
        // 1. Получаем объект PlayerSkin (в новых версиях это обертка над текстурами)
        // Мы используем DefaultSkinHelper как запасной вариант, если скин еще не прогружен
        Identifier skin = DefaultSkinHelper.getTexture();

        if (friend.getUuid() != null) {
            // Пытаемся получить информацию о скине из кэша игровых профилей клиента
            var skinTextures = BlackOut.mc.getSkinProvider().getSkinTextures(new GameProfile(friend.getUuid(), friend.getName()));

            // Если текстура уже скачана и готова - используем её
            if (skinTextures != null && skinTextures.texture() != null) {
                skin = skinTextures.texture();
            }
        }

        // 2. Рисуем фон
        RenderUtils.rounded(this.stack, 15.0F, 12.0F, 50.0F, 50.0F, 12.0F, 0, GuiColorUtils.bg1.getRGB(), 0);

        // 3. Получаем ID текстуры
        int glId = BlackOut.mc.getTextureManager().getTexture(skin).getGlId();

        // 4. Отрисовка (Лицо: 0.125 -> 0.250)
        TextureRenderer.renderFitRounded(this.stack, 18.0F, 15.0F, 44.0F, 44.0F,
                0.125F, 0.125F, 0.250F, 0.250F, 10.0F, 20, glId);

        // 5. Отрисовка второго слоя (Шлем/Волосы/Аксессуары)
        // Без этого слоя многие скины выглядят как "женские" или "лысые",
        // так как основной объем волос часто на втором слое.
        TextureRenderer.renderFitRounded(this.stack, 18.0F, 15.0F, 44.0F, 44.0F,
                0.625F, 0.125F, 0.750F, 0.250F, 10.0F, 20, glId);
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            // Обновленная логика клика с учетом ITEM_HEIGHT
            if (my > 0 && my < height - 40) {
                float startY = 25.0F - this.scroll.get();
                for (FriendsManager.Friend friend : Managers.FRIENDS.getFriends()) {
                    if (mx > width - 110 && mx < width - 10 && my > startY && my < startY + ITEM_HEIGHT) {
                        Managers.FRIENDS.remove(friend.getName());
                        return;
                    }
                    startY += ITEM_HEIGHT;
                }
            }
        }
    }
}