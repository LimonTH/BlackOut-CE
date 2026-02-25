package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.GuiRenderUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.RenderUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchScreen extends ClickGuiScreen {
    private final TextField textField = new TextField();
    private List<Module> results = new ArrayList<>();

    private final float RADIUS = 6.0F;
    private final float SHADOW = 2.0F;
    private final float SHADER_PADDING = RADIUS + SHADOW;

    public SearchScreen(int key) {
        super("Search", 400, 500, true);
        this.textField.setActive(true);
        this.onKey(key, true);
    }

    @Override
    public void render() {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float fieldHeight = 45.0F * fs;
        float entryHeight = 40.0F * fs;
        float spacing = 4.0F * fs;

        float moduleGap = (SHADER_PADDING * 2) + spacing;

        float startY = 20.0F + fieldHeight;
        float yPos = startY + moduleGap - this.scroll.get();

        for (Module module : results) {
            if (yPos > this.height) break;

            if (yPos + entryHeight > 0) {
                boolean hovered = this.mx > 20.0F && this.mx < this.width - 20.0F && this.my > yPos && this.my < yPos + entryHeight;
                int bgColor = hovered ? ColorUtils.withAlpha(GuiColorUtils.bg1.getRGB(), 180) : ColorUtils.withAlpha(GuiColorUtils.bg1.getRGB(), 100);

                RenderUtils.rounded(this.stack, 20.0F, yPos, this.width - 40.0F, entryHeight, RADIUS, SHADOW, bgColor, ColorUtils.SHADOW100I);

                int textColor = module.enabled ? GuiRenderUtils.getGuiColors(1.0F).getRGB() : Color.LIGHT_GRAY.getRGB();
                BlackOut.FONT.text(this.stack, module.getDisplayName(), 1.8F * fs, 35.0F, yPos + (entryHeight / 2.0F), textColor, false, true);

                String category = module.category.name();
                float catScale = 1.3F * fs;
                float catWidth = BlackOut.FONT.getWidth(category) * catScale;
                BlackOut.FONT.text(this.stack, category, catScale, this.width - 35.0F - catWidth, yPos + (entryHeight / 2.0F), Color.GRAY.getRGB(), false, true);
            }

            yPos += entryHeight + moduleGap;
        }

        this.textField.render(this.stack, 1.8F * fs, this.mx, this.my, 20.0F, 20.0F, this.width - 40.0F, fieldHeight, 5.0F, 2.0F, Color.WHITE, GuiColorUtils.bg1);
    }

    @Override
    protected float getLength() {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float fieldHeight = 45.0F * fs;
        float entryHeight = 40.0F * fs;
        float spacing = 4.0F * fs;

        return (35.0F + fieldHeight) + results.size() * (entryHeight + (SHADER_PADDING * 2) + spacing) + 20.0F;
    }

    @Override
    public boolean handleKey(int key, boolean state) {
        this.onKey(key, state);
        return true;
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state) {
            if (button == 0 && this.textField.click(button, state)) return;

            float fs = GuiSettings.getInstance().fontScale.get().floatValue();
            float entryHeight = 40.0F * fs;
            float spacing = 4.0F * fs;

            float yPos = (20.0F + 45.0F * fs) + ((SHADER_PADDING * 2) + 4.0F * fs) - this.scroll.get();

            for (Module module : results) {
                if (this.mx > 20.0F && this.mx < this.width - 20.0F && this.my > yPos && this.my < yPos + entryHeight) {
                    if (button == 0) {
                        module.toggle();
                        Managers.CONFIG.saveModule(module);
                    } else if (button == 1) {
                        ClickGui gui = Managers.CLICK_GUI.CLICK_GUI;
                        ClickGui.selectedCategory = module.category;
                        gui.moduleComponents.forEach(c -> c.opened = (c.module == module));
                        gui.updateModuleLengthManual();
                        gui.scrollToModule(module);
                        gui.setScreen(null);
                    }
                    return;
                }
                yPos += entryHeight + (SHADER_PADDING * 2) + spacing;
            }
        }
    }

    @Override
    public void onKey(int key, boolean state) {
        if (state) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                SelectedComponent.reset();
                Managers.CLICK_GUI.CLICK_GUI.setScreen(null);
                return;
            }

            this.textField.type(key, true);
            this.updateResults();
        } else {
            this.textField.type(key, false);
        }
    }
    public void updateResults() {
        String query = this.textField.getContent().toLowerCase();
        if (query.isEmpty()) {
            this.results = new ArrayList<>();
            return;
        }

        this.results = Managers.MODULES.getModules().stream()
                .filter(m -> m.getDisplayName().toLowerCase().contains(query) || m.category.name().toLowerCase().contains(query))
                .sorted((m1, m2) -> {
                    boolean m1Exact = m1.getDisplayName().toLowerCase().startsWith(query);
                    boolean m2Exact = m2.getDisplayName().toLowerCase().startsWith(query);
                    if (m1Exact && !m2Exact) return -1;
                    if (!m1Exact && m2Exact) return 1;
                    return m1.getDisplayName().compareToIgnoreCase(m2.getDisplayName());
                })
                .limit(20)
                .collect(Collectors.toList());
    }

    public TextField getTextField() {
        return textField;
    }
}