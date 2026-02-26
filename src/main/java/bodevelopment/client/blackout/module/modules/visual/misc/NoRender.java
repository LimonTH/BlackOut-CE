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
    private final SettingGroup sgItems = this.addGroup("Items");

    public final Setting<Boolean> wallOverlay = this.sgGeneral.booleanSetting("Block Overlays", true, "Prevents visual obstruction when the camera is inside solid blocks.");
    public final Setting<Boolean> waterOverlay = this.sgGeneral.booleanSetting("Submersion Overlay", true, "Suppresses the blue texture overlay when submerged in water.");
    public final Setting<Boolean> fireOverlay = this.sgGeneral.booleanSetting("Combustion Overlay", true, "Suppresses the flame animation overlay when the player is on fire.");
    public final Setting<Boolean> effectOverlay = this.sgGeneral.booleanSetting("Status Overlays", true, "Disables visual HUD effects caused by status conditions like nausea or blindness.");
    public final Setting<Boolean> totem = this.sgGeneral.booleanSetting("Totem Animation", true, "Suppresses the Totem of Undying animation that normally covers the screen upon use.");
    public final Setting<Boolean> pumpkin = this.sgGeneral.booleanSetting("Pumpkin Mask", true, "Removes the carved pumpkin vignette from the HUD.");
    public final Setting<Boolean> crystalBase = this.sgGeneral.booleanSetting("Crystal Pedestal", true, "Prevents the rendering of the obsidian/bedrock base underneath End Crystals.");

    public final Setting<Boolean> helmet = this.sgItems.booleanSetting("Cranial Armor", false, "Hides the helmet model on entities.");
    public final Setting<Boolean> chestplate = this.sgItems.booleanSetting("Torso Armor", false, "Hides the chestplate model on entities.");
    public final Setting<Boolean> leggings = this.sgItems.booleanSetting("Leg Armor", false, "Hides the legging models on entities.");
    public final Setting<Boolean> boots = this.sgItems.booleanSetting("Footwear", false, "Hides the boot models on entities.");
    public final Setting<Boolean> left = this.sgItems.booleanSetting("Off-Hand Model", false, "Stops the left hand/item from rendering in first and third person.");
    public final Setting<Boolean> right = this.sgItems.booleanSetting("Main-Hand Model", false, "Stops the right hand/item from rendering in first and third person.");
    private final Setting<List<ParticleType<?>>> particles = this.sgGeneral.registrySetting("Particle Filter", "A list of specific particle types that will not be rendered in the game world.", Registries.PARTICLE_TYPE, RegistryNames::get);

    public NoRender() {
        super("No Render", "Selectively disables the rendering of various overlays, entity parts, and environmental particles to increase visibility and performance.", SubCategory.MISC_VISUAL, true);
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
