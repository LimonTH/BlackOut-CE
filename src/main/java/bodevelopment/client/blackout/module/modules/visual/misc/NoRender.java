package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.RegistryNames;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;

import java.util.List;

public class NoRender extends Module {
    private static NoRender INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> wallOverlay = this.sgGeneral.booleanSetting("Wall Overlay", true, "Doesn't cover your whole screen while inside a wall.");
    public final Setting<Boolean> waterOverlay = this.sgGeneral.booleanSetting("Water Overlay", true, "Doesn't render water overlay.");
    public final Setting<Boolean> fireOverlay = this.sgGeneral.booleanSetting("Fire Overlay", true, "Doesn't render fire overlay.");
    public final Setting<Boolean> effectOverlay = this.sgGeneral.booleanSetting("Effect Overlay", true, "Doesn't render effect overlay.");
    public final Setting<Boolean> totem = this.sgGeneral.booleanSetting("Totem", true, "Doesn't render totem of undying after popping.");
    public final Setting<Boolean> pumpkin = this.sgGeneral.booleanSetting("Pumpkin Overlay", true, "Doesn't render the pumpkin overlay.");
    public final Setting<Boolean> crystalBase = this.sgGeneral.booleanSetting("Crystal Base", true, "Doesn't render the bedrock slab under end crystals.");
    private final SettingGroup sgItems = this.addGroup("Items");
    public final Setting<Boolean> helmet = this.sgItems.booleanSetting("Helmet", false, ".");
    public final Setting<Boolean> chestplate = this.sgItems.booleanSetting("Chestplate", false, ".");
    public final Setting<Boolean> leggings = this.sgItems.booleanSetting("Leggings", false, ".");
    public final Setting<Boolean> boots = this.sgItems.booleanSetting("Boots", false, ".");
    public final Setting<Boolean> left = this.sgItems.booleanSetting("Left Hand", false, ".");
    public final Setting<Boolean> right = this.sgItems.booleanSetting("Right Hand", false, ".");
    private final Setting<List<ParticleType<?>>> particles = this.sgGeneral.registrySetting("Particles", ".", Registries.PARTICLE_TYPE, RegistryNames::get);

    public NoRender() {
        super("No Render", "Doesn't render some stuff.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static NoRender getInstance() {
        return INSTANCE;
    }

    public boolean shouldNoRender(ParticleType<?> particleType) {
        return this.particles.get().contains(particleType);
    }

    public boolean ignoreArmor(EquipmentSlot slot) {
        return (switch (slot) {
            case FEET -> this.boots;
            case LEGS -> this.leggings;
            case CHEST -> this.chestplate;
            default -> this.helmet;
        }).get();
    }

    public boolean ignoreHand(Arm arm) {
        return (arm == Arm.RIGHT ? this.right : this.left).get();
    }
}
