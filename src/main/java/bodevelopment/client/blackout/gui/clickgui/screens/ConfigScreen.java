package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

public class ConfigScreen extends ClickGuiScreen {
    private static final int lineColor = new Color(50, 50, 50, 255).getRGB();
    private static final String[] iconNames = new String[]{"Combat", "Movement", "Visual", "Misc", "Legit", "Settings", "HUD", "Binds"};
    private static final TextureRenderer[] icons = new TextureRenderer[]{
            BOTextures.getCombatIconRenderer(),
            BOTextures.getMovementIconRenderer(),
            BOTextures.getVisualIconRenderer(),
            BOTextures.getMiscIconRenderer(),
            BOTextures.getGhostIconRenderer(),
            BOTextures.getSettingsIconRenderer(),
            BOTextures.getHudIconRenderer(),
            BOTextures.getBindsIconRenderer()
    };

    private final Map<String, MutableDouble> configs = new HashMap<>();
    private final TextField textField = new TextField();
    private final int id = SelectedComponent.nextId();
    private final List<CloudConfig> cloudConfigs = new ArrayList<>();
    private float prevLength = 300.0F;
    private long prevUpdate = 0L;
    private boolean first;
    private boolean typing = false;
    private boolean isCloud = false;

    public ConfigScreen() {
        super("Configs", 800.0F, 500.0F, true);
    }

    @Override
    protected float getLength() {
        return this.prevLength;
    }

    @Override
    public void render() {
        if (System.currentTimeMillis() - this.prevUpdate > 500L) {
            this.updateConfigs();
        }

        this.updateDelete();
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.renderConfigs();
        this.renderText();
        this.renderBottomBG();
        this.renderBottom();
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (this.my > this.height - 100.0F || this.my < 0.0) return;

        if (state && button == 0) {
            if (this.textField.click(0, true) && this.typing) {
                SelectedComponent.setId(this.id);
            } else {
                double offsetY = this.my + this.scroll.get() - 50.0;

                for (Entry<String, MutableDouble> config : this.configs.entrySet()) {
                    double offsetX = this.mx - 215.0;

                    if (offsetX * offsetX + offsetY * offsetY < 200.0) {
                        this.duplicate(config.getKey());
                        return;
                    }

                    offsetX -= 35.0;
                    if (offsetX * offsetX + offsetY * offsetY < 200.0) {
                        this.delete(config.getKey());
                        return;
                    }

                    for (int i = 0; i < 8; i++) {
                        offsetX -= 65.0;
                        if (offsetX * offsetX + offsetY * offsetY < 1000.0) {
                            this.set(config.getKey(), i);
                            return;
                        }
                    }
                    offsetY -= 70.0;
                }

                double offsetXx = this.mx - this.width / 2.0F;
                double dy = offsetY * offsetY;
                if (offsetXx * offsetXx + dy <= 2500.0) { // Радиус 50
                    if (offsetXx > 0.0) this.clickedCloud();
                    else this.clickedAdd();
                }

                offsetY -= 70.0;

                for (CloudConfig config : this.cloudConfigs) {
                    if (Math.abs(offsetY) < 35.0 && config.content().isDone()) {
                        this.downloadConfig(config);
                        this.updateConfigs();
                        return;
                    }
                    offsetY -= 70.0;
                }
            }
        }
    }

    @Override
    public void onKey(int key, boolean state) {
        if (this.typing && state) {
            if (key == 257) { // Enter
                if (this.isCloud) this.requestCloudConfigs(this.textField.getContent());
                else this.addConfig(this.textField.getContent());

                this.typing = false;
                SelectedComponent.reset();
            } else if (key == 256) { // Escape
                this.typing = false;
                SelectedComponent.reset();
            } else {
                this.textField.type(key, true);
            }
        }
    }

    private void clickedCloud() {
        this.typing = true;
        this.isCloud = true;
        this.textField.clear();
        this.textField.setContent("KassuK1/blackout-configs");
    }

    private void clickedAdd() {
        this.typing = true;
        this.isCloud = false;
        this.textField.clear();
    }

    private void addConfig(String name) {
        if (!name.isBlank() && !name.contains("/") && !name.contains("\\")) {
            FileUtils.addFile("configs", name + ".json");
            this.updateConfigs();
        }
    }

    private void downloadConfig(CloudConfig config) {
        File file = this.getNewConfigFile(config.name());
        if (file != null && !file.exists()) {
            try {
                FileUtils.addFile(file);
                FileUtils.write(file, config.content().get());
            } catch (Exception e) {
                BOLogger.error("Error downloading configs from the cloud", e);
            }
        }
    }

    private void requestCloudConfigs(String repo) {
        if (!repo.contains("/")) return;
        this.cloudConfigs.clear();
        CompletableFuture.runAsync(() -> {
            try (InputStream stream = URI.create("https://raw.githubusercontent.com/" + repo + "/main/configs.txt").toURL().openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                reader.lines().forEach(line -> readLine(repo, line));
                for (CloudConfig config : this.cloudConfigs) readConfig(config);
            } catch (IOException e) {
                BOLogger.error("Error requesting configs from the cloud", e);
            }
        });
    }

    private void readConfig(CloudConfig cloudConfig) throws IOException {
        try (InputStream configStream = URI.create(String.format("https://raw.githubusercontent.com/%s/main/configs/%s.json", cloudConfig.repo(), cloudConfig.name())).toURL().openStream()) {
            cloudConfig.content().complete(new String(configStream.readAllBytes()));
        }
    }

    private void readLine(String repo, String line) {
        String[] strings = line.split(":");
        if (strings.length >= 2) {
            String name = strings[0];
            String desc = strings[1].replace("\"", "");
            this.cloudConfigs.add(new CloudConfig(name, desc, repo, new CompletableFuture<>()));
        }
    }

    private void updateDelete() {
        double dx = (this.mx - 250.0) * (this.mx - 250.0);
        double offsetY = this.my + this.scroll.get() - 50.0;

        for (MutableDouble val : this.configs.values()) {
            if (offsetY * offsetY + dx < 200.0) val.add(this.frameTime);
            else val.setValue(Math.max(val.getValue() - this.frameTime * 5.0F, 0.0));
            offsetY -= 70.0;
        }
    }

    private void set(String config, int i) {
        Managers.CONFIG.saveAll();
        Managers.CONFIG.writeCurrent();
        Managers.CONFIG.getConfigs()[i] = config;
        Managers.CONFIG.set();
        Managers.CONFIG.readConfigs();
    }

    private void duplicate(String config) {
        File newFile = this.getNewConfigFile(config);
        File fromFile = FileUtils.getFile("configs", config + ".json");
        if (newFile != null && fromFile.exists()) {
            FileUtils.addFile(newFile);
            FileUtils.write(newFile, FileUtils.readString(fromFile));
            this.updateConfigs();
        }
    }

    private File getNewConfigFile(String name) {
        File f = FileUtils.getFile("configs", name + ".json");
        if (!f.exists()) return f;
        int i = 1;
        while (i < 100) {
            f = FileUtils.getFile("configs", name + "_" + i + ".json");
            if (!f.exists()) return f;
            i++;
        }
        return null;
    }

    private void delete(String config) {
        if (this.configs.get(config).getValue() >= 5.0) {
            for (String active : Managers.CONFIG.getConfigs()) if (active.equals(config)) return;
            File f = FileUtils.getFile("configs", config + ".json");
            if (f.delete()) this.updateConfigs();
        }
    }

    private void renderText() {
        this.textField.setActive(this.typing);
        if (this.typing) {
            this.textField.render(this.stack, 2.0F, this.mx, this.my, this.width / 2.0F - 250.0F, this.height - 150.0F, 500.0F, 0.0F, 20.0F, 15.0F, Color.WHITE, GuiColorUtils.bg2);
        }
    }

    private void renderConfigs() {
        this.prevLength = (this.configs.size() + this.cloudConfigs.size()) * 70 + 165;
        this.stack.push();
        this.stack.translate(0.0F, 15.0F - this.scroll.get(), 0.0F);

        this.first = true;
        this.configs.forEach(this::renderConfig);

        this.renderAdd();

        this.stack.translate(0.0F, 70.0F, 0.0F);
        this.first = true;
        this.cloudConfigs.forEach(this::renderCloudConfig);

        this.stack.pop();
    }

    private void renderAdd() {
        this.stack.push();
        this.stack.translate(this.width / 2.0F, 30.0F, 0.0F);
        RenderUtils.roundedLeft(this.stack, -50.0F, 0.0F, 50.0F, 0.0F, 20.0F, 15.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);
        RenderUtils.roundedRight(this.stack, 0.0F, 0.0F, 50.0F, 0.0F, 20.0F, 15.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);
        BOTextures.getCloudIconRenderer().quad(this.stack, 15.0F, -20.0F, 40.0F, 40.0F);
        BOTextures.getPlusIconRenderer().quad(this.stack, -55.0F, -20.0F, 40.0F, 40.0F);
        RenderUtils.line(this.stack, 0.0F, -20.0F, 0.0F, 20.0F, lineColor);
        this.stack.pop();
    }

    private void renderBottomBG() {
        RenderUtils.roundedBottom(this.stack, 0.0F, this.height - 105.0F, this.width, 65.0F, 10.0F, 0.0F, GuiColorUtils.bg2.getRGB(), 0);
        RenderUtils.topFade(this.stack, -10.0F, this.height - 125.0F, this.width + 20.0F, 20.0F, GuiColorUtils.bg2.getRGB());
        RenderUtils.line(this.stack, -10.0F, this.height - 105.0F, this.width + 10.0F, this.height - 105.0F, lineColor);
    }

    private void renderBottom() {
        this.stack.push();
        this.stack.translate(250.0F, this.height - 70.0F, 0.0F);

        for (int i = 0; i < 8; i++) {
            this.stack.translate(65.0F, 0.0F, 0.0F);

            icons[i].quad(this.stack, -20.0F, -30.0F, 40.0F, 40.0F);

            BlackOut.FONT.text(this.stack, iconNames[i], 1.6F, 0.0F, 18.0F, Color.WHITE, true, true);
        }
        this.stack.pop();
    }

    private void renderCloudConfig(CloudConfig config) {
        if (!this.first) RenderUtils.line(this.stack, -10.0F, 0.0F, this.width + 10.0F, 0.0F, lineColor);
        this.first = false;

        int color = (config.content().isDone() ? Color.WHITE : Color.GRAY).getRGB();
        BOTextures.getCloud2IconRenderer().quad(this.stack, 10.0F, 15.0F, 40.0F, 40.0F, color);
        BlackOut.FONT.text(this.stack, config.name(), 2.5F, 70.0F, 24.0F, color, false, true);
        BlackOut.FONT.text(this.stack, config.description(), 1.8F, 70.0F, 46.0F, Color.GRAY, false, true);
        this.stack.translate(0.0F, 70.0F, 0.0F);
    }

    private void renderConfig(String name, MutableDouble mutableDouble) {
        if (!this.first) RenderUtils.line(this.stack, -10.0F, 0.0F, this.width + 10.0F, 0.0F, lineColor);
        this.first = false;

        this.stack.push();
        this.stack.translate(250.0F, 35.0F, 0.0F);
        boolean inUse = false;
        for (int i = 0; i < 8; i++) {
            this.stack.translate(65.0F, 0.0F, 0.0F);
            if (Managers.CONFIG.getConfigs()[i].equals(name)) {
                inUse = true;
                RenderUtils.rounded(this.stack, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, Color.WHITE.getRGB(), Color.WHITE.getRGB());
            }
            RenderUtils.roundedShadow(this.stack, 0.0F, 0.0F, 0.0F, 0.0F, 10.0F, 10.0F, ColorUtils.SHADOW100I);
        }
        this.stack.pop();

        BOTextures.getCopyIconRenderer().quad(this.stack, 200.0F, 20.0F, 30.0F, 30.0F, Color.WHITE.getRGB());
        double time = mutableDouble.getValue();

        if (!inUse) {
            BOTextures.getTrashIconRenderer().quad(this.stack, 235.0F, 20.0F, 30.0F, 30.0F, trashColor(time));
            if (time > 0.1)
                BlackOut.FONT.text(this.stack, String.format("%.1f", 5.0 - time), 2.0F, 250.0F, 35.0F, trashTextColor(time), true, true);
            (time >= 5.0 ? BOTextures.getLockOpenIconRenderer() : BOTextures.getLockIconRenderer()).quad(this.stack, 235.0F, 20.0F, 30.0F, 30.0F, lockColor(time));
        } else {
            BOTextures.getTrashIconRenderer().quad(this.stack, 235.0F, 20.0F, 30.0F, 30.0F, new Color(255, 0, 0, 50).getRGB());
        }

        BlackOut.FONT.text(this.stack, name, 2.5F, 10.0F, 35.0F, Color.WHITE, false, true);
        this.stack.translate(0.0F, 70.0F, 0.0F);
    }

    private int trashTextColor(double time) {
        float a = time <= 1.0 ? (float) Math.sqrt(time) : (time >= 4.5 ? 1f - (float) Math.sqrt((time - 4.5) * 2) : 1f);
        return ColorUtils.withAlpha(Color.WHITE.getRGB(), (int) (MathHelper.clamp(a, 0, 1) * 255));
    }

    private int lockColor(double time) {
        float a = time <= 0.7 ? 0 : (time <= 1.2 ? (float) Math.sqrt(time - 0.7) / 2f : (time >= 5.0 ? 1f : 0.35f));
        return ColorUtils.withAlpha(Color.WHITE.getRGB(), (int) (MathHelper.clamp(a, 0, 1) * 255));
    }

    private int trashColor(double time) {
        float a = time <= 1.0 ? 1f - (float) Math.sqrt(time) : 0f;
        return ColorUtils.withAlpha(Color.WHITE.getRGB(), (int) (MathHelper.clamp(a, 0, 1) * 255));
    }

    private void updateConfigs() {
        this.prevUpdate = System.currentTimeMillis();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(FileUtils.getFile("configs").toPath(), p -> p.toString().endsWith(".json"))) {
            List<String> foundNames = new ArrayList<>();

            stream.forEach(p -> {
                String name = p.getFileName().toString().replace(".json", "");
                foundNames.add(name);
                if (!configs.containsKey(name)) {
                    configs.put(name, new MutableDouble(0.0));
                }
            });

            configs.keySet().removeIf(k -> !foundNames.contains(k));
        } catch (IOException e) {
            BOLogger.error("Error updating configs", e);
        }
    }

    private record CloudConfig(String name, String description, String repo, CompletableFuture<String> content) {
    }
}