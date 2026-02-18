package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.SoundUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.Random;

public class MainMenu {
    public static final int EMPTY_COLOR = new Color(0, 0, 0, 0).getRGB();
    private static final MainMenu INSTANCE = new MainMenu();
    public final String[] buttonNames = new String[]{"Singleplayer", "Multiplayer", "AltManager", "Options", "Quit"};
    private final String splashText = this.getSplash();
    private final MatrixStack stack = new MatrixStack();
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

    private final Runnable[] runnables = new Runnable[]{
            () -> {
                Managers.ALT.switchToOriginal();
                this.startExit(new SelectWorldScreen(this.titleScreen));
            },
            () -> {
                Managers.ALT.switchToSelected();
                this.startExit(new MultiplayerScreen(this.titleScreen));
            },
            () -> this.startExit(new AltManagerScreen(this.titleScreen)),
            () -> this.startExit(new OptionsScreen(this.titleScreen, BlackOut.mc.options)),
            BlackOut.mc::scheduleStop
    };

    private void startExit(Screen screen) {
        screenToSet = screen;
        isExiting = true;
    }

    public static void init() {
        BlackOut.EVENT_BUS.subscribe(INSTANCE, () -> !(BlackOut.mc.currentScreen instanceof TitleScreen));
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

        MainMenuSettings.getInstance().getRenderer().render(this.stack, this.windowHeight, renderMx, renderMy, this.splashText);

        if (guiAlpha > 0.01F) {
            this.stack.push();
            float bigW = 2000.0F;
            float bigH = this.windowHeight;

            RenderUtils.loadBlur("gui_blur", (int) (guiAlpha * 10.0F));
            RenderUtils.drawLoadedBlur("gui_blur", this.stack, renderer ->
                    renderer.quadShape(-bigW, -bigH, bigW * 2.0F, bigH * 2.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F)
            );

            RenderUtils.quad(this.stack, -bigW, -bigH, bigW * 2.0F, bigH * 2.0F, new Color(0, 0, 0, (int) (guiAlpha * 130)).getRGB());
            this.stack.pop();
        }

        this.endRender();

        if (globalFade < 1.0F) {
            int alpha = (int) ((1.0F - globalFade) * 255.0F);
            int blackColor = (alpha << 24);
            float screenW = (float) BlackOut.mc.getWindow().getWidth();
            float screenH = (float) BlackOut.mc.getWindow().getHeight();

            stack.push();
            RenderUtils.unGuiScale(stack);
            RenderUtils.quad(stack, 0, 0, screenW, screenH, blackColor);
            stack.pop();
        }

        if (isGuiOpen) {
            this.clickGui.render(new DrawContext(BlackOut.mc, BlackOut.mc.getBufferBuilders().getEntityVertexConsumers()), mouseX, mouseY, delta);
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
                this.openLink("https://github.com/LimonTH/Blackout-CE");
                break;
            case 1:
                this.openLink("https://discord.com/invite/mmWz9Dz4Y9");
                break;
            case 2:
                this.openLink("https://www.youtube.com/@BlackOutDevelopment");
        }
    }

    private void openLink(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);

            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(uri);
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("win")) {
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", url);
                } else {
                    pb = new ProcessBuilder("xdg-open", url);
                }
                pb.start();
            }
        } catch (Exception e) {
            BOLogger.error("Could not open link: " + url, e);
        }
    }

    private void updateWindowData() {
        double physicalWidth = BlackOut.mc.getWindow().getWidth();
        double physicalHeight = BlackOut.mc.getWindow().getHeight();

        this.scale = (float) (physicalWidth / 2000.0F);
        this.windowHeight = (float) (physicalHeight / physicalWidth * 2000.0F);

        double logicalX = BlackOut.mc.mouse.getX();
        double logicalY = BlackOut.mc.mouse.getY();

        this.mx = (float) ((logicalX - physicalWidth / 2.0) / this.scale);
        this.my = (float) ((logicalY - physicalHeight / 2.0) / this.scale);
    }

    private void startRender(float scale) {
        this.stack.push();
        RenderUtils.unGuiScale(this.stack);

        int screenW = BlackOut.mc.getWindow().getWidth();
        int screenH = BlackOut.mc.getWindow().getHeight();

        MainMenuSettings.getInstance()
                .getRenderer()
                .renderBackground(this.stack, screenW, screenH, this.mx, this.my);

        this.stack.translate(screenW / 2.0F, screenH / 2.0F, 0.0F);
        this.stack.scale(scale, scale, scale);
    }

    private void endRender() {
        this.stack.pop();
    }

    private String getSplash() {
        String[] splashTexts = new String[]{"BlackOut: Because default is boring"};
        return splashTexts[new Random().nextInt(0, splashTexts.length)];
    }

    @Event
    public void onMouse(MouseButtonEvent buttonEvent) {
        if (BlackOut.mc.currentScreen instanceof TitleScreen && (this.clickGui.isOpen() || ClickGui.popUpDelta > 0.1F)) {
            this.updateWindowData();
            buttonEvent.cancel();
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
                    if (this.clickGui.openedScreen != null) {
                        this.clickGui.setScreen(null);
                        return;
                    }
                    this.clickGui.setOpen(false);
                    this.clickGui.toggleTime = System.currentTimeMillis();
                    return;
                }
                if (event.key == 344) {
                    this.clickGui.setOpen(false);
                    this.clickGui.toggleTime = System.currentTimeMillis();
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

    public MatrixStack getMatrixStack() {
        return this.stack;
    }

    public boolean isOpenedMenu() {
        return this.clickGui.isOpen();
    }

    public boolean isExiting() {
        return isExiting;
    }
}