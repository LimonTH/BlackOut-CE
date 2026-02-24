package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Category;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.ParentCategory;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TabGui extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<ColorMode> selectorMode = this.sgGeneral.enumSetting("Selector Mode", ColorMode.Custom, ".");
    private final Setting<BlackOutColor> selectorColor = this.sgGeneral
            .colorSetting("Selector Color", new BlackOutColor(125, 125, 125, 255), "Base color for the selector", () -> this.selectorMode.get() != ColorMode.Rainbow);
    private final Setting<Double> waveSpeed = this.sgGeneral
            .doubleSetting("Wave Speed", 2.0, 0.0, 10.0, 0.1, "Speed for the wave effect", () -> this.selectorMode.get() == ColorMode.Wave);
    private final Setting<BlackOutColor> waveColor = this.sgGeneral
            .colorSetting("Wave Color", new BlackOutColor(125, 125, 125, 255), "Color For The Wave", () -> this.selectorMode.get() == ColorMode.Wave);
    private final Setting<Double> saturation = this.sgGeneral
            .doubleSetting("Rainbow Saturation", 0.8, 0.0, 1.0, 0.1, ".", () -> this.selectorMode.get() == ColorMode.Rainbow);
    public final Setting<BlackOutColor> textDisabled = this.sgGeneral.colorSetting("Disabled Text", new BlackOutColor(150, 150, 150, 255), ".");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, null);
    private final Setting<Integer> bloomIntensity = this.sgGeneral.intSetting("Selector Bloom Intensity", 1, 0, 2, 1, ".");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Enabled Text");
    private final Map<Module, MutableDouble> moduleMap = new HashMap<>();
    private int selectedModule = 0;
    private int selectedParentId = 0;
    private int selectedChildId = 0;
    private ParentCategory selectedParent = null;
    private SubCategory selectedChild = null;
    private int i = 0;
    private float progress = 0.0F;
    private int opened = 0;

    public TabGui() {
        super("Tab GUI", ".");

        for (Module module : Managers.MODULES.getModules()) {
            this.moduleMap.put(module, new MutableDouble(module.enabled ? 1.0 : 0.0));
        }

        this.setSize(75.0F, (BlackOut.FONT.getHeight() + 10.0F) * ParentCategory.categories.size());
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.stack.push();
            this.selectedParent = ParentCategory.categories.get(this.selectedParentId);
            this.selectedChild = this.getChild(this.selectedChildId);
            if (this.opened > -1) {
                this.renderParents(0.0F);
            }

            if (this.opened > 0) {
                this.renderChildren(90.0F);
            }

            if (this.opened > 1) {
                this.renderModules(180.0F, this.frameTime * 2.0F);
            }

            this.stack.pop();
        }
    }

    private void renderParents(float x) {
        this.progress = 0.0F;
        this.i = 0;
        this.renderBG(ParentCategory.categories.size(), x);
        ParentCategory.categories.forEach(cat -> {
            this.renderCategory(cat, x, this.selectedParentId);
            this.i++;
            this.progress = this.progress + (BlackOut.FONT.getHeight() + 10.0F);
        });
    }

    private void renderChildren(float x) {
        this.progress = 0.0F;
        this.i = 0;

        for (SubCategory cat : SubCategory.categories) {
            if (cat.parent() == this.selectedParent) {
                this.i++;
            }
        }

        this.renderBG(this.i, x);
        this.i = 0;
        SubCategory.categories.forEach(catx -> {
            if (catx.parent() == this.selectedParent) {
                this.renderCategory(catx, x, this.selectedChildId);
                this.i++;
                this.progress = this.progress + (BlackOut.FONT.getHeight() + 10.0F);
            }
        });
    }

    private void renderModules(float x, double frameTime) {
        this.progress = 0.0F;
        this.i = 0;

        for (Entry<Module, MutableDouble> entry : this.moduleMap.entrySet()) {
            if (entry.getKey().category == this.selectedChild) {
                this.i++;
            }
        }

        this.renderBG(this.i, x);
        this.i = 0;
        this.moduleMap.forEach((module, d) -> {
            if (module.category == this.selectedChild) {
                this.renderModule(module, d.getValue(), x);
                d.setValue(MathHelper.clamp(d.getValue() + (module.enabled ? frameTime : -frameTime), 0.0, 1.0));
                this.i++;
                this.progress = this.progress + (BlackOut.FONT.getHeight() + 10.0F);
            }
        });
    }

    private void renderCategory(Category category, float x, int sel) {
        float y = this.progress + (BlackOut.FONT.getHeight() + 10.0F) / 2.0F;
        if (sel == this.i) {
            this.renderSelector(x + 4.0F, y);
        }

        BlackOut.FONT.text(this.stack, category.name(), 1.0F, x + 9.0F, y, this.textColor.getTextColor().getColor(), false, true);
    }

    private void renderModule(Module module, double delta, float x) {
        float y = this.progress + (BlackOut.FONT.getHeight() + 10.0F) / 2.0F;
        if (this.selectedModule == this.i) {
            this.renderSelector(x + 4.0F, y);
        }

        float test = (float) (2.0 * delta);
        float prevAlpha = Renderer.getAlpha();
        Renderer.setAlpha((float) delta);
        this.textColor.render(this.stack, module.getDisplayName(), 1.0F, x + 9.0F + test, y, false, true);
        Renderer.setAlpha(prevAlpha);
        Renderer.setAlpha((float) (1.0 - delta));
        BlackOut.FONT.text(this.stack, module.getDisplayName(), 1.0F, x + 9.0F + test, y, this.textDisabled.get().getColor(), false, true);
        Renderer.setAlpha(prevAlpha);
    }

    private void renderSelector(float x, float y) {
        Color color = Color.WHITE;
        switch (this.selectorMode.get()) {
            case Custom:
                color = this.selectorColor.get().getColor();
                break;
            case Rainbow:
                color = new Color(ColorUtils.getRainbow(4.0F, this.saturation.get().floatValue(), 1.0F, 150L));
                break;
            case Wave:
                color = ColorUtils.getWave(this.selectorColor.get().getColor(), this.waveColor.get().getColor(), this.waveSpeed.get(), 1.0, 1);
        }

        RenderUtils.rounded(
                this.stack,
                x,
                y - BlackOut.FONT.getHeight() / 2.0F - 1.5F,
                0.3F,
                BlackOut.FONT.getHeight() + 2.0F,
                1.0F,
                this.bloomIntensity.get(),
                color.getRGB(),
                color.getRGB()
        );
    }

    private void renderBG(float height, float x) {
        float length = (BlackOut.FONT.getHeight() + 10.0F) * height;
        RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(x + 1.0F, 0.0F, 75.0F, length, 3.0F, 10));
        Renderer.onHUDBlur();
        this.background.render(this.stack, x + 1.0F, 0.0F, 75.0F, length, 3.0F, 3.0F);
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.pressed) {
            switch (event.key) {
                case 262:
                    if (this.opened == 2) {
                        Module module = this.getModule(this.selectedModule);
                        if (module != null) {
                            module.toggle();
                        }
                    }

                    this.opened = Math.min(this.opened + 1, 2);
                    break;
                case 263:
                    switch (this.opened) {
                        case 0:
                            this.selectedParentId = 0;
                            break;
                        case 1:
                            this.selectedChildId = 0;
                            break;
                        case 2:
                            this.selectedModule = 0;
                    }

                    this.opened = Math.max(this.opened - 1, -1);
                    break;
                case 264:
                    switch (this.opened) {
                        case 0:
                            this.selectedParentId++;
                            if (this.selectedParentId >= ParentCategory.categories.size()) {
                                this.selectedParentId = 0;
                            }

                            return;
                        case 1:
                            this.selectedChildId++;
                            if (this.selectedChildId >= this.getChildAmount()) {
                                this.selectedChildId = 0;
                            }

                            return;
                        case 2:
                            this.selectedModule++;
                            if (this.selectedModule >= this.getModuleAmount()) {
                                this.selectedModule = 0;
                            }

                            return;
                        default:
                            return;
                    }
                case 265:
                    switch (this.opened) {
                        case 0:
                            this.selectedParentId--;
                            if (this.selectedParentId < 0) {
                                this.selectedParentId = ParentCategory.categories.size() - 1;
                            }
                            break;
                        case 1:
                            this.selectedChildId--;
                            if (this.selectedChildId < 0) {
                                this.selectedChildId = this.getChildAmount() - 1;
                            }
                            break;
                        case 2:
                            this.selectedModule--;
                            if (this.selectedModule < 0) {
                                this.selectedModule = this.getModuleAmount() - 1;
                            }
                    }
            }
        }
    }

    private int getChildAmount() {
        this.i = 0;
        SubCategory.categories.forEach(cat -> {
            if (cat.parent() == this.selectedParent) {
                this.i++;
            }
        });
        return this.i;
    }

    private int getModuleAmount() {
        this.i = 0;
        this.moduleMap.forEach((module, d) -> {
            if (module.category == this.selectedChild) {
                this.i++;
            }
        });
        return this.i;
    }

    private Module getModule(int index) {
        this.i = 0;

        for (Entry<Module, MutableDouble> entry : this.moduleMap.entrySet()) {
            Module module = entry.getKey();
            if (module.category == this.selectedChild && this.i++ == index) {
                return module;
            }
        }

        return null;
    }

    private SubCategory getChild(int index) {
        this.i = 0;

        for (SubCategory cat : SubCategory.categories) {
            if (cat.parent() == this.selectedParent && this.i++ == index) {
                return cat;
            }
        }

        return null;
    }

    public enum ColorMode {
        Rainbow,
        Custom,
        Wave
    }
}
