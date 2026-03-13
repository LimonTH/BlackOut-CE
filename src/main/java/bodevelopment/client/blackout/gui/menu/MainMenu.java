package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.util.FileUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.SoundUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public class MainMenu {
    public static final int EMPTY_COLOR = new Color(0, 0, 0, 0).getRGB();
    private static final MainMenu INSTANCE = new MainMenu();
    public final String[] buttonNames = new String[]{"Singleplayer", "Multiplayer", "AltManager", "Options", "Quit"};
    private final PoseStack stack = new PoseStack();
    private final ClickGui clickGui = Managers.CLICK_GUI.CLICK_GUI;
    private TitleScreen titleScreen;
    private float windowHeight;
    private float scale;
    private boolean isClickStartedHere = false;
    private float mx;
    private float my;
    public static float globalFade = 0.0F;
    private static Screen screenToSet = null;
    private static boolean isExiting = false;
    private float delta;
    private boolean playedStartup = false;

    private String currentSplash = "";
    private String nextSplash = "";
    private long lastSplashChange = System.currentTimeMillis();
    private float splashProgress = 1.0F;
    private static final long SPLASH_DELAY = 10000L;

    public final String[] SPLASHES = {
            "The best in the business",
            "The real opp stoppa",
            "Sponsored by Columbian cartels",
            "The GOAT assistance software",
            "Recommended by 9/10 dentists",
            "Made in Finland, resumed in Russia",
            "Innit bruv",
            "Based & red-pilled",
            "Bravo 6 blacking-out",
            "A shark in the water",
            "Gaslight, Gatekeep, Girlboss",
            "Your FPS is my snack",
            "Keyboard warrior approved",
            "Hyper-threaded performance",
            "Zero days since last blackout",
            "Better than your average cheat"
    };

    private final Runnable[] runnables = new Runnable[]{
            () -> {
                Managers.ALT.switchToOriginal();
                this.startExit(new SelectWorldScreen(this.titleScreen));
            },
            () -> {
                Managers.ALT.switchToSelected();
                this.startExit(new JoinMultiplayerScreen(this.titleScreen));
            },
            () -> this.startExit(new AltManagerScreen(this.titleScreen)),
            () -> this.startExit(new OptionsScreen(this.titleScreen, BlackOut.mc.options)),
            BlackOut.mc::stop
    };

    private void startExit(Screen screen) {
        screenToSet = screen;
        isExiting = true;
    }

    public static void init() {
        BlackOut.EVENT_BUS.subscribe(INSTANCE, () -> !(BlackOut.mc.screen instanceof TitleScreen));
    }

    public static MainMenu getInstance() {
        return INSTANCE;
    }

    public void set(TitleScreen screen) {
        this.titleScreen = screen;
    }

    public void render(int mouseX, int mouseY, float delta) {
        if (!this.playedStartup) {
            SoundUtils.play(1.0F, 10.0F, "startup");
            this.playedStartup = true;
        }

        this.updateWindowData();
        this.delta = delta / 20.0F;
        this.updateSplash(this.delta);
        if (isExiting) {
            globalFade = Math.max(0.0F, globalFade - this.delta * 3.0F);
            if (globalFade <= 0.0F && screenToSet != null) {
                BlackOut.mc.setScreen(screenToSet);
                isExiting = false;
                screenToSet = null;
                return;
            }
        } else {
            globalFade = Math.min(1.0F, globalFade + this.delta * 3.0F);
        }

        float guiAlpha = (float) Math.sqrt(ClickGui.popUpDelta);
        boolean isGuiOpen = this.clickGui.isOpen() || guiAlpha > 0.01F;

        this.startRender(this.scale);

        float renderMx = (isGuiOpen || isExiting || globalFade < 0.99F) ? -5000.0F : this.mx;
        float renderMy = (isGuiOpen || isExiting || globalFade < 0.99F) ? -5000.0F : this.my;

        MainMenuSettings.getInstance().getRenderer().render(
                this.stack,
                this.windowHeight,
                renderMx,
                renderMy,
                this.currentSplash,
                this.nextSplash,
                this.splashProgress
        );

        if (guiAlpha > 0.01F) {
            this.stack.pushPose();
            float bigW = 2000.0F;
            float bigH = this.windowHeight;

            RenderUtils.loadBlur("gui_blur", (int) (guiAlpha * 10.0F));
            RenderUtils.drawLoadedBlur("gui_blur", this.stack, renderer ->
                    renderer.quadShape(-bigW, -bigH, bigW * 2.0F, bigH * 2.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F)
            );

            RenderUtils.quad(this.stack, -bigW, -bigH, bigW * 2.0F, bigH * 2.0F, new Color(0, 0, 0, (int) (guiAlpha * 130)).getRGB());
            this.stack.popPose();
        }

        this.endRender();

        if (globalFade < 1.0F) {
            int alpha = (int) ((1.0F - globalFade) * 255.0F);
            int blackColor = (alpha << 24);
            float screenW = (float) BlackOut.mc.getWindow().getScreenWidth();
            float screenH = (float) BlackOut.mc.getWindow().getScreenHeight();

            stack.pushPose();
            RenderUtils.unGuiScale(stack);
            RenderUtils.quad(stack, 0, 0, screenW, screenH, blackColor);
            stack.popPose();
        }

        if (isGuiOpen) {
            this.clickGui.render(new GuiGraphics(BlackOut.mc, BlackOut.mc.renderBuffers().bufferSource()), mouseX, mouseY, delta);
        }
    }

    private void clickMenu(int button, boolean pressed) {
        if (button != 0) return;

        if (pressed) {
            int hoverIndex = MainMenuSettings.getInstance().getRenderer().onClick(this.mx, this.my);

            if (hoverIndex >= 0 || (this.my >= this.windowHeight / 2.0F - 54.0F && this.my <= this.windowHeight - 12.0F)) {
                this.isClickStartedHere = true;
            }
        } else {
            if (!this.isClickStartedHere) return;
            this.isClickStartedHere = false;

            int i = MainMenuSettings.getInstance().getRenderer().onClick(this.mx, this.my);

            if (i == 6) {
                this.clickGui.toggleTime = System.currentTimeMillis();
                this.clickGui.setOpen(!this.clickGui.isOpen());
                if (this.clickGui.isOpen()) {
                    this.clickGui.initGui();
                }
                SoundUtils.play(1.0F, 3.0F, "menubutton");
                return;
            }

            if (i >= 0 && i < this.runnables.length) {
                SoundUtils.play(1.0F, 3.0F, "menubutton");
                this.runnables[i].run();
            } else {
                float offset = this.mx + 986.0F;
                if (this.my >= this.windowHeight / 2.0F - 54.0F && this.my <= this.windowHeight - 12.0F) {
                    for (int j = 0; j < 3; j++) {
                        if (offset >= -10 + j * 54 && offset <= 32 + j * 54) {
                            this.onClickIconButton(j);
                            SoundUtils.play(1.0F, 3.0F, "menubutton");
                        }
                    }
                }
            }
        }
    }

    private void onClickIconButton(int i) {
        switch (i) {
            case 0:
                FileUtils.openLink("https://github.com/LimonTH/Blackout-CE");
                break;
            case 1:
                FileUtils.openLink("https://discordapp.com/users/866910380097929217"); /* "https://discord.com/invite/mmWz9Dz4Y9" */
                break;
            case 2:
                FileUtils.openLink("https://www.youtube.com/watch?v=aWJpcxjk5DQ");
        }
    }

    private void updateWindowData() {
        double physicalWidth = BlackOut.mc.getWindow().getScreenWidth();
        double physicalHeight = BlackOut.mc.getWindow().getScreenHeight();

        this.scale = (float) (physicalWidth / 2000.0F);
        this.windowHeight = (float) (physicalHeight / physicalWidth * 2000.0F);

        double logicalX = BlackOut.mc.mouseHandler.xpos();
        double logicalY = BlackOut.mc.mouseHandler.ypos();

        this.mx = (float) ((logicalX - physicalWidth / 2.0) / this.scale);
        this.my = (float) ((logicalY - physicalHeight / 2.0) / this.scale);
    }

    private void startRender(float scale) {
        this.stack.pushPose();
        RenderUtils.unGuiScale(this.stack);

        int screenW = BlackOut.mc.getWindow().getScreenWidth();
        int screenH = BlackOut.mc.getWindow().getScreenHeight();

        MainMenuSettings.getInstance()
                .getRenderer()
                .renderBackground(this.stack, screenW, screenH, this.mx, this.my);

        this.stack.translate(screenW / 2.0F, screenH / 2.0F, 0.0F);
        this.stack.scale(scale, scale, scale);
    }

    private void updateSplash(float delta) {
        long now = System.currentTimeMillis();

        if (currentSplash.isEmpty()) {
            currentSplash = SPLASHES[new Random().nextInt(SPLASHES.length)];
        }

        if (now - lastSplashChange > SPLASH_DELAY && splashProgress >= 1.0F) {
            nextSplash = SPLASHES[new Random().nextInt(SPLASHES.length)];
            lastSplashChange = now;
            splashProgress = 0.0F;
        }

        if (splashProgress < 1.0F) {
            splashProgress = Math.min(1.0F, splashProgress + delta * 2.0F);
        }

        if (splashProgress >= 1.0F && !nextSplash.isEmpty()) {
            currentSplash = nextSplash;
            nextSplash = "";
        }
    }

    private void endRender() {
        this.stack.popPose();
    }

    @Event
    public void onMouse(MouseButtonEvent buttonEvent) {
        if (BlackOut.mc.screen instanceof TitleScreen && (this.clickGui.isOpen() || ClickGui.popUpDelta > 0.1F)) {
            this.updateWindowData();
            this.clickGui.onClick(buttonEvent);
            return;
        }

        this.updateWindowData();
        this.clickMenu(buttonEvent.button, buttonEvent.pressed);
    }

    @Event
    public void onKey(KeyEvent event) {
        if (this.clickGui.isOpen()) {
            if (event.pressed) {
                if (event.key == 256) {
                    if (SelectedComponent.isSelected()) {
                        SelectedComponent.reset();
                        event.cancel();
                        return;
                    }
                    if (this.clickGui.openedScreen != null) {
                        this.clickGui.setScreen(null);
                        event.cancel();
                        return;
                    }
                    this.clickGui.setOpen(false);
                    this.clickGui.toggleTime = System.currentTimeMillis();
                    event.cancel();
                    return;
                }

                if (event.key == 344) {
                    this.clickGui.setOpen(false);
                    this.clickGui.toggleTime = System.currentTimeMillis();
                    event.cancel();
                    return;
                }
            }

            event.cancel();
            this.clickGui.onKey(event);
            return;
        }

        if (event.pressed && event.key == 344) {
            this.clickGui.toggleTime = System.currentTimeMillis();
            this.clickGui.setOpen(true);
            if (this.clickGui.isOpen()) {
                this.clickGui.initGui();
            }
            SoundUtils.play(1.0F, 3.0F, "menubutton");
        }
    }

    @Event
    public void onScroll(MouseScrollEvent event) {
        if (this.clickGui.isOpen()) {
            event.cancel();
            this.clickGui.onScroll(event);
        }
    }

    public float getWindowHeight() {
        return this.windowHeight;
    }

    public PoseStack getMatrixStack() {
        return this.stack;
    }

    public boolean isOpenedMenu() {
        return this.clickGui.isOpen();
    }

    public boolean isExiting() {
        return isExiting;
    }
}