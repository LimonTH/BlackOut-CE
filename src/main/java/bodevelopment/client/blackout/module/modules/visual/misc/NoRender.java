package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.RegistryNames;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import java.util.List;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;

public class NoRender extends Module {
    private static NoRender INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgWorld = this.addGroup("World");
    private final SettingGroup sgItems = this.addGroup("Items");

    public final Setting<Boolean> wallOverlay = this.sgGeneral.booleanSetting("Block Overlays", true, "Prevents visual obstruction when the camera is inside solid blocks.");
    public final Setting<Boolean> waterOverlay = this.sgGeneral.booleanSetting("Submersion Overlay", true, "Suppresses the blue texture overlay when submerged in water.");
    public final Setting<Boolean> fireOverlay = this.sgGeneral.booleanSetting("Combustion Overlay", true, "Suppresses the flame animation overlay when the player is on fire.");
    public final Setting<Boolean> portalOverlay = this.sgGeneral.booleanSetting("Portal Overlay", true, "Suppresses the spinning nether-portal distortion overlay.");
    public final Setting<Boolean> effectOverlay = this.sgGeneral.booleanSetting("Status Overlays", true, "Disables visual HUD effects caused by status conditions like nausea or blindness.");
    public final Setting<Boolean> vignette = this.sgGeneral.booleanSetting("Vignette", false, "Removes the dark corner vignette drawn around the screen.");
    public final Setting<Boolean> bossBar = this.sgGeneral.booleanSetting("Boss Bar", false, "Hides the boss health bar HUD element.");
    public final Setting<Boolean> totem = this.sgGeneral.booleanSetting("Totem Animation", true, "Suppresses the Totem of Undying animation that normally covers the screen upon use.");
    public final Setting<Boolean> pumpkin = this.sgGeneral.booleanSetting("Pumpkin Mask", true, "Removes the carved pumpkin vignette from the HUD.");
    public final Setting<Boolean> noBobbing = this.sgGeneral.booleanSetting("No Bobbing", true, "Prevents the camera from bobbing while moving.");
    public final Setting<Boolean> noHurtCam = this.sgGeneral.booleanSetting("No Hurt Cam", true, "Removes the camera shake when taking damage.");

    public final Setting<Boolean> fog = this.sgWorld.booleanSetting("Fog", false, "Removes distance and void fog from the world.");
    public final Setting<Boolean> clouds = this.sgWorld.booleanSetting("Clouds", false, "Disables cloud rendering.");
    public final Setting<Boolean> weather = this.sgWorld.booleanSetting("Weather", false, "Suppresses rain and snow particle rendering.");
    public final Setting<Boolean> shadows = this.sgWorld.booleanSetting("Entity Shadows", false, "Removes the circular drop-shadow beneath entities.");
    public final Setting<Boolean> crystalBase = this.sgWorld.booleanSetting("Crystal Pedestal", true, "Prevents the rendering of the obsidian/bedrock base underneath End Crystals.");
    public final Setting<Boolean> beaconBeam = this.sgWorld.booleanSetting("Beacon Beam", false, "Hides the vertical beam emitted by beacons.");
    public final Setting<Boolean> skybox = this.sgWorld.booleanSetting("Skybox", false, "Disables sky and celestial body rendering (sun, moon, stars).");
    private final Setting<List<ParticleType<?>>> particles = this.sgWorld.registrySetting("Particle Filter", "A list of specific particle types that will not be rendered in the game world.", BuiltInRegistries.PARTICLE_TYPE, RegistryNames::get);

    public final Setting<Boolean> enchantGlint = this.sgItems.booleanSetting("Enchantment Glint", false, "Removes the shimmering glint effect on enchanted items and armor.");
    public final Setting<Boolean> helmet  = this.sgItems.booleanSetting("Cranial Armor", false, "Hides the helmet model on entities.");
    public final Setting<Boolean> chestplate = this.sgItems.booleanSetting("Torso Armor", false, "Hides the chestplate model on entities.");
    public final Setting<Boolean> leggings = this.sgItems.booleanSetting("Leg Armor", false, "Hides the legging models on entities.");
    public final Setting<Boolean> boots = this.sgItems.booleanSetting("Footwear", false, "Hides the boot models on entities.");
    public final Setting<Boolean> left = this.sgItems.booleanSetting("Off-Hand Model", false, "Stops the left hand/item from rendering in first and third person.");
    public final Setting<Boolean> right = this.sgItems.booleanSetting("Main-Hand Model", false, "Stops the right hand/item from rendering in first and third person.");

    public NoRender() {
        super("No Render", "Selectively disables the rendering of various overlays, entity parts, and environmental particles to increase visibility and performance.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static NoRender getInstance() { return INSTANCE; }

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

    public boolean ignoreHand(HumanoidArm arm) {
        return (arm == HumanoidArm.RIGHT ? this.right : this.left).get();
    }
}