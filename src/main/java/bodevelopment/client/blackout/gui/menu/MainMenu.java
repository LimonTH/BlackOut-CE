package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.helpers.ScrollHelper;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.SoundUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.Random;

public class MainMenu {
    public static final int EMPTY_COLOR = new Color(0, 0, 0, 0).getRGB();
    private static final MainMenu INSTANCE = new MainMenu();
    public final String[] buttonNames = new String[]{"Singleplayer", "Multiplayer", "AltManager", "Options", "Quit"};
    private final String splashText = this.getSplash();
    private final MatrixStack stack = new MatrixStack();
    private final TextField textField = new TextField();
    private final ClickGui clickGui = Managers.CLICK_GUI.CLICK_GUI;
    private TitleScreen titleScreen;
    private float windowHeight;
    private float scale;
    private boolean isClickStartedHere = false;
    private float progress = 0.0F;
    private float mx;
    private float my;
    private boolean altManagerOpen = false;
    private long altManagerTime = 0L;
    private final Runnable[] runnables = new Runnable[]{
            () -> {
                Managers.ALT.switchToOriginal();
                BlackOut.mc.setScreen(new SelectWorldScreen(this.titleScreen));
            },
            () -> {
                Managers.ALT.switchToSelected();
                BlackOut.mc.setScreen(new MultiplayerScreen(this.titleScreen));
            },
            () -> {
                if (this.altManagerOpen) {
                    this.closeAltManager();
                } else {
                    this.openAltManager();
                }
            },
            () -> BlackOut.mc.execute(() -> BlackOut.mc.setScreen(new OptionsScreen(this.titleScreen, BlackOut.mc.options))),
            BlackOut.mc::scheduleStop
    };
    private float delta;
    private float altLength = 0.0F;
    private final ScrollHelper scroll = new ScrollHelper(
            0.5F,
            5.5F,
            () -> 0.0F,
            () -> Math.max(this.altLength - 600.0F, 0.0F)
    ).limit(3.0F);
    private boolean playedStartup = false;

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
        if (!this.playedStartup) { SoundUtils.play(1.0F, 10.0F, "startup"); this.playedStartup = true; }
        this.scroll.update(Math.min(BlackOut.mc.getRenderTickCounter().getLastFrameDuration(), 0.016F));
        this.delta = delta / 20.0F;
        this.updateWindowData();

        float guiAlpha = (float) Math.sqrt(ClickGui.popUpDelta);
        boolean isGuiOpen = this.clickGui.isOpen() || guiAlpha > 0.01F;
        float progress = this.getSwitchProgress();
        float menuAlpha = MathHelper.clamp(MathHelper.getLerpProgress(progress, 0.4F, 0.0F), 0.0F, 1.0F);
        float altAlpha = MathHelper.clamp(MathHelper.getLerpProgress(progress, 0.6F, 1.0F), 0.0F, 1.0F);
        float tIntensity = (float) (1.0 - AnimUtils.easeInOutCubic(Math.min(Math.abs(progress - 0.5F), 0.5F) / 0.5F));

        this.startRender(this.scale);

        // СЛОЙ 1: БЛЮР
        float totalBlurRadius = 0.0F;
        if (tIntensity > 0.01F || altAlpha > 0.01F) {
            totalBlurRadius = (tIntensity * 12.0F) + (altAlpha * 6.0F);
        }

        if (guiAlpha > 0.01F) {
            totalBlurRadius = Math.max(totalBlurRadius, guiAlpha * 10.0F);
        }

        if (totalBlurRadius > 0.1F) {
            this.stack.push();
            RenderUtils.loadBlur("master_blur", (int) totalBlurRadius);
            RenderUtils.drawLoadedBlur("master_blur", this.stack, r ->
                    r.quadShape(-2000, -windowHeight, 4000, windowHeight * 2, 0, 1, 1, 1, 1));
            this.stack.pop();
        }

        // СЛОЙ 2: ЗАТЕМНЕНИЕ
        float brightness = 1.0F - (tIntensity * 0.7F) - (altAlpha * 0.3F);
        brightness = MathHelper.clamp(brightness, 0.2F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);

        // СЛОЙ 3: КОНТЕНТ
        float renderMx = isGuiOpen ? -1000.0F : this.mx;
        float renderMy = isGuiOpen ? -1000.0F : this.my;

        if (menuAlpha > 0.0F) {
            Renderer.setAlpha(menuAlpha);
            MainMenuSettings.getInstance().getRenderer().render(this.stack, this.windowHeight, renderMx, renderMy, this.splashText);
        }

        if (altAlpha > 0.01F) {
            Renderer.setAlpha(altAlpha);
            this.renderAltManager();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        this.endRender();

        if (isGuiOpen) {
            this.clickGui.render(new net.minecraft.client.gui.DrawContext(BlackOut.mc, BlackOut.mc.getBufferBuilders().getEntityVertexConsumers()), mouseX, mouseY, delta);
        }
    }

    private void renderAltManager() {
        this.renderAltManagerTitle();
        this.renderCurrentSession();
        this.renderTextField();
        this.renderAccounts();
    }

    private void renderTextField() {
        if (!this.textField.isEmpty()) {
            this.progress = Math.min(this.progress + this.delta, 1.0F);
        } else {
            this.progress = Math.max(this.progress - this.delta, 0.0F);
        }

        this.textField
                .render(
                        this.stack,
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

    private void renderCurrentSession() {
        Managers.ALT.currentSession.render(this.stack, -940.0F, this.windowHeight / 2.0F - 65.0F - 60.0F, this.delta);
    }

    private void renderAccounts() {
        // TODO: возможно сделать конфигурируемые параметры менеджера аккаунтов
        // MainMenuSettings mainMenuSettings = MainMenuSettings.getInstance();
        this.altLength = -90.0F;
        this.stack.push();
        this.stack.translate(0.0F, -this.scroll.get(), 0.0F);
        this.stack.translate(-250.0F, this.windowHeight / -2.0F + 200.0F, 0.0F);
        Managers.ALT.getAccounts().forEach(account -> {
            account.render(this.stack, 0.0F, 0.0F, this.delta);
            float amogus = 155.0F;
            this.stack.translate(0.0F, amogus, 0.0F);
            this.altLength += amogus;
        });
        this.stack.pop();
    }

    private void renderAltManagerTitle() {
        BlackOut.BOLD_FONT.text(this.stack, "Alt Manager", 8.5F, 0.0F, this.windowHeight / -2.0F + 100.0F, Color.WHITE, true, true);
    }

    public float getSwitchProgress() {
        float f = Math.min((float) (System.currentTimeMillis() - this.altManagerTime) / 600.0F, 1.0F);
        return this.altManagerOpen ? f : 1.0F - f;
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

    private void clickAltManager(int button, boolean pressed) {
        if (!this.textField.click(button, pressed)) {

            float startX = -250.0F;
            float startY = this.windowHeight / -2.0F + 200.0F;
            float currentY = startY - this.scroll.get();
            for (Account account : new java.util.ArrayList<>(Managers.ALT.getAccounts())) {

                float relX = this.mx - startX;
                float relY = this.my - currentY;

                if (this.clickAccount(account, relX, relY, button, pressed)) {
                    return;
                }

                currentY += 155.0F;
            }
            float sessionX = -940.0F;
            float sessionY = this.windowHeight / 2.0F - 65.0F - 60.0F;
            this.clickAccount(Managers.ALT.currentSession, this.mx - sessionX, this.my - sessionY, button, pressed);
        }
    }

    private boolean clickAccount(Account account, float x, float y, int button, boolean pressed) {
        Account.AccountClickResult result = account.onClick(x, y, button, pressed);
        if (result != Account.AccountClickResult.Nothing) {
            this.handleAltClick(account, result);
            return true;
        } else {
            return false;
        }
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
        String[] splashTexts = new String[]{
                // --- ОСНОВНОЙ ВАЙБ ---
                "The best in the business",
                "The real opp stoppa",
                "Sponsored by Columbian cartels",
                "The GOAT assistance software",
                "Recommended by 9/10 dentists",
                "Made in Finland",
                "Innit bruv",
                "Based & red-pilled",
                "Bravo 6 blacking-out",
                "A shark in the water",
                "Gaslight, Gatekeep, Girlboss",
                "Your FPS is my snack",
                "Keyboard warrior approved",
                "Hyper-threaded performance",
                "Zero days since last blackout",
                "Better than your average cheat",
                "Calculated risk, maximum reward",
                "Lag is just a state of mind",
                "Stay frosty",
                "The final boss of clients",
                "Actually built different",
                "I can see your house from here",
                "Log4J was just a warmup",
                "Your admin is my fanboy",
                "Synthesized for greatness",
                "Don't cry because it happened, laugh because it's over",
                "The dark side has cookies",
                "Skidding is for amateurs",
                "Optimization is not a crime",
                "More power than a nuclear reactor",
                "Sleep is for the weak",
                "Coded in a basement, used in the sky",
                "Reject modernity, embrace BlackOut",
                "I don't hack, I just have a better gaming chair",
                "Unpatchable spirit",
                "The silent predator",
                "Digital adrenaline",
                "Your firewall is a suggestion",
                "Absolute dominance",
                "Hiding in plain sight",
                "I'm not saying I'm Batman, but...",
                "Your antivirus is just a suggestion",
                "Kernel-level charisma",
                "Mainlining caffeine and bytecode",
                "The cake is a lie, but this client isn't",
                "Sending your packets to the shadow realm",
                "I survived the 2b2t queue",
                "100% organic, grass-fed code",
                "Your base belongs to us",
                "Unscheduled rapid disassembly of opponents",
                "Error 404: Mercy not found",
                "Move fast and break things (especially admins)",
                "Quantum-entangled hitboxes",
                "More features than a Swiss Army Knife",
                "Injecting happiness since 2026",
                "Wait, people play vanilla?",
                "Technoblade never dies",
                "Subscribed to chaos",
                "You provide the salt, we provide the pepper",
                "BlackOut: Because default is boring",

                // --- RUSSIAN VIBE ---
                "From Russia with hacks",
                "Born in the snow, raised in the code",
                "Hardbass in the headset",
                "Cheeki breeki iv damke!",
                "Cyberpunk in a Khrushchevka",
                "Made in Russia, polished in Finland",
                "Optimized for Siberian temperatures",
                "Run on vodka and electricity",
                "Hack the world, drink the tea",
                "Soviet engineering inside",
                "Do svidaniya, your base",
                "Gop-stop in your chunk",
                "Russian hacktivism is my hobby",
                "Not just a client, it's a lifestyle",
                "Bear-powered packet injection",
                "Anarchy in my DNA",
                "Siberian packet delivery service",
                "Khrushchevka-based development",

                // --- ЛЕГЕНДАРНЫЕ СЕРВЕРА (BALANCE) ---
                "NewPlaces: Where legends are forged",
                "NewPlaces is my playground",
                "FitMC told me about this",
                "Popbob's favorite tool",
                "Bedrock is just a suggestion",
                "Crystal PvP enthusiast",
                "Spawn is just a warm-up",
                "Breaking the economy, one dupe at a time",
                "Total anarchy, total control",

                // --- ТЕХНО-ТРЕШ ---
                "Compiling hatred into bytecode",
                "Memory leak? No, it's a feature",
                "Run as Administrator",
                "Harder than a sudo rm -rf",
                "I speak fluent Assembly",
                "127.0.0.1 is where the heart is",
                "Ping is a choice"
        };
        return splashTexts[new Random().nextInt(0, splashTexts.length)];
    }

    private void openAltManager() {
        this.altManagerOpen = true;
        this.altManagerTime = System.currentTimeMillis();
    }

    private void closeAltManager() {
        this.altManagerOpen = false;
        this.altManagerTime = System.currentTimeMillis();
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
        float switchProgress = this.getSwitchProgress();
        if (switchProgress == 0.0F) {
            this.clickMenu(buttonEvent.button, buttonEvent.pressed);
        }

        if (switchProgress == 1.0F) {
            this.clickAltManager(buttonEvent.button, buttonEvent.pressed);
        }
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
            return;
        }

        if (!event.pressed) return;

        if (event.key == 256) {
            if (this.getSwitchProgress() > 0.5F) {
                this.closeAltManager();
                return;
            }
        } else if (event.key == 257) {
            if (this.altManagerOpen && !this.textField.isEmpty()) {
                Managers.ALT.add(new Account(this.textField.getContent()));
                this.textField.clear();
                SoundUtils.play(1.0F, 1.0F, "menubutton");
                return;
            }
        }
        if (this.altManagerOpen) {
            this.textField.type(event.key, event.pressed);
        }
    }

    @Event
    public void onScroll(MouseScrollEvent event) {
        if (this.clickGui.isOpen()) {
            event.cancel();
            this.clickGui.onScroll(event);
            return;
        }
        if (this.altManagerOpen) {
            this.scroll.add((float) event.vertical * 3.0F);
        }
    }

    public float getWindowHeight() {
        return this.windowHeight;
    }

    public MatrixStack getMatrixStack() {
        return this.stack;
    }

    public boolean isOpenedMenu() {
        return clickGui.isOpen();
    }
}
