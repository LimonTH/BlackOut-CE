package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.helpers.ScrollHelper;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.util.SoundUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;

public class AltManagerScreen extends Screen {
    private final Screen parent;
    private final TextField textField = new TextField();
    private float windowHeight;
    private float scale;
    private float mx;
    private float my;
    private float progress = 0.0F;
    private float delta;
    private float altLength = 0.0F;
    private boolean isExiting = false;

    private final ScrollHelper scroll = new ScrollHelper(
            0.5F,
            5.5F,
            () -> 0.0F,
            () -> Math.max(this.altLength - 600.0F, 0.0F)
    ).limit(3.0F);

    public AltManagerScreen(Screen parent) {
        super(Text.of("Alt Manager"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTick) {
        MatrixStack stack = context.getMatrices();
        this.updateWindowData(mouseX, mouseY);

        float frameDuration = Math.min(BlackOut.mc.getRenderTickCounter().getLastFrameDuration(), 0.016F);
        this.scroll.update(frameDuration);

        this.delta = frameDuration;

        if (this.isExiting) {
            MainMenu.globalFade = Math.max(0.0F, MainMenu.globalFade - this.delta * 3.0F);
            if (MainMenu.globalFade <= 0.0F) {
                BlackOut.mc.setScreen(this.parent);
                return;
            }
        } else {
            MainMenu.globalFade = Math.min(1.0F, MainMenu.globalFade + this.delta * 3.0F);
        }

        stack.push();
        RenderUtils.unGuiScale(stack);

        int screenW = BlackOut.mc.getWindow().getWidth();
        int screenH = BlackOut.mc.getWindow().getHeight();

        MainMenuSettings.getInstance().getRenderer().renderBackground(stack, screenW, screenH, this.mx, this.my);

        stack.translate(screenW / 2.0F, screenH / 2.0F, 0.0F);
        stack.scale(this.scale, this.scale, this.scale);

        this.renderAltManagerTitle(stack);
        this.renderCurrentSession(stack);
        this.renderTextField(stack);
        this.renderAccounts(stack);

        stack.pop();

        if (MainMenu.globalFade < 1.0F) {
            int alpha = (int) ((1.0F - MainMenu.globalFade) * 255.0F);
            int blackColor = (alpha << 24);

            stack.push();
            RenderUtils.unGuiScale(stack);
            RenderUtils.quad(stack, 0, 0, screenW, screenH, blackColor);
            stack.pop();
        }
    }

    private void renderTextField(MatrixStack stack) {
        if (!this.textField.isEmpty()) {
            this.progress = Math.min(this.progress + this.delta, 1.0F);
        } else {
            this.progress = Math.max(this.progress - this.delta, 0.0F);
        }

        this.textField.render(
                stack,
                4.0F,
                this.mx,
                this.my,
                -200.0F,
                400.0F,
                400.0F,
                0.0F,
                24.0F,
                48.0F,
                new Color(255, 255, 255, (int) Math.floor(this.progress * 255.0F)),
                new Color(0, 0, 0, (int) Math.floor(this.progress * 30.0F))
        );
    }

    private void renderCurrentSession(MatrixStack stack) {
        Managers.ALT.currentSession.render(stack, -940.0F, this.windowHeight / 2.0F - 65.0F - 60.0F, this.delta);
    }

    private void renderAccounts(MatrixStack stack) {
        this.altLength = -90.0F;
        stack.push();
        stack.translate(0.0F, -this.scroll.get(), 0.0F);
        stack.translate(-250.0F, this.windowHeight / -2.0F + 200.0F, 0.0F);

        Managers.ALT.getAccounts().forEach(account -> {
            account.render(stack, 0.0F, 0.0F, this.delta);
            float offset = 155.0F;
            stack.translate(0.0F, offset, 0.0F);
            this.altLength += offset;
        });

        stack.pop();
    }

    private void renderAltManagerTitle(MatrixStack stack) {
        BlackOut.BOLD_FONT.text(stack, "Alt Manager", 8.5F, 0.0F, this.windowHeight / -2.0F + 100.0F, Color.WHITE, true, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MainMenu.globalFade < 0.99F) return false;

        this.updateWindowData(0, 0);
        this.clickAltManager(button, true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.updateWindowData(0, 0);
        this.clickAltManager(button, false);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    private void clickAltManager(int button, boolean pressed) {
        if (this.textField.click(button, pressed)) return;

        float startX = -250.0F;
        float startY = this.windowHeight / -2.0F + 200.0F;
        float currentY = startY - this.scroll.get();

        for (Account account : new ArrayList<>(Managers.ALT.getAccounts())) {
            float relX = this.mx - startX;
            float relY = this.my - currentY;

            if (relY >= -10.0F && relY <= 150.0F && relX >= -10.0F && relX <= 510.0F) {
                if (this.clickAccount(account, relX, relY, button, pressed)) {
                    return;
                }
            }
            currentY += 155.0F;
        }

        float sessionX = -940.0F;
        float sessionY = this.windowHeight / 2.0F - 65.0F - 60.0F;
        this.clickAccount(Managers.ALT.currentSession, this.mx - sessionX, this.my - sessionY, button, pressed);
    }

    private boolean clickAccount(Account account, float x, float y, int button, boolean pressed) {
        Account.AccountClickResult result = account.onClick(x, y, button, pressed);
        if (result != Account.AccountClickResult.Nothing) {
            this.handleAltClick(account, result);
            return true;
        }
        return false;
    }

    private void handleAltClick(Account account, Account.AccountClickResult result) {
        if (result == Account.AccountClickResult.Nothing) return;

        switch (result) {
            case Select -> Managers.ALT.set(account);
            case Refresh -> account.refresh();
            case Delete -> {
                Managers.ALT.remove(account);
                this.altLength = Managers.ALT.getAccounts().size() * 155.0F - 90.0F;
            }
        }
        SoundUtils.play(1.0F, 3.0F, "menubutton");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.isExiting = true;
            return true;
        } else if (keyCode == 257) {
            if (!this.textField.isEmpty()) {
                Managers.ALT.add(new Account(this.textField.getContent()));
                this.textField.clear();
                SoundUtils.play(1.0F, 1.0F, "menubutton");
                return true;
            }
        }
        this.textField.type(keyCode, true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.textField.type(keyCode, false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scroll.add((float) verticalAmount * 3.0F);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateWindowData(double ignoredX, double ignoredY) {
        double physicalWidth = BlackOut.mc.getWindow().getWidth();
        double physicalHeight = BlackOut.mc.getWindow().getHeight();

        this.scale = (float) (physicalWidth / 2000.0F);
        this.windowHeight = (float) (physicalHeight / physicalWidth * 2000.0F);

        double logicalX = BlackOut.mc.mouse.getX();
        double logicalY = BlackOut.mc.mouse.getY();

        this.mx = (float) ((logicalX - physicalWidth / 2.0) / this.scale);
        this.my = (float) ((logicalY - physicalHeight / 2.0) / this.scale);
    }
}