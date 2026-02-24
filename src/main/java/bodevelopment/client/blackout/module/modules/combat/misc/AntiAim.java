package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.RotationUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;

public class AntiAim extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgIgnore = this.addGroup("Ignore");

    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Custom,
            "The movement pattern of your head (Spin, Random, or Fixed).");
    private final Setting<YawMode> yawMode = this.sgGeneral.enumSetting("Yaw Mode", YawMode.Normal,
            "How the horizontal rotation is calculated (Absolute or Relative).", () -> this.mode.get() != Mode.Spin && this.mode.get() != Mode.Enemy);
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Range", 20.0, 0.0, 500.0, 5.0,
            "The distance within which the module will track enemies for rotations.", () -> this.mode.get() == Mode.Enemy);
    private final Setting<Double> spinSpeed = this.sgGeneral.doubleSetting("Spin Speed", 5.0, 0.0, 100.0, 1.0,
            "How fast your character spins (only for Spin mode).", () -> this.mode.get() == Mode.Spin);
    private final Setting<Double> csgoYawMin = this.sgGeneral.doubleSetting("CSGO Yaw Min", -180.0, -180.0, 180.0, 1.0,
            "Minimum random horizontal angle for CSGO jitter mode.", () -> this.mode.get() == Mode.CSGO);
    private final Setting<Double> csgoYawMax = this.sgGeneral.doubleSetting("CSGO Yaw Max", 180.0, -180.0, 180.0, 1.0,
            "Maximum random horizontal angle for CSGO jitter mode.", () -> this.mode.get() == Mode.CSGO);
    private final Setting<Double> csgoPitchMin = this.sgGeneral.doubleSetting("CSGO Pitch Min", -90.0, -90.0, 90.0, 1.0,
            "Minimum random vertical angle.", () -> this.mode.get() == Mode.CSGO);
    private final Setting<Double> csgoPitchMax = this.sgGeneral.doubleSetting("CSGO Pitch Max", 90.0, -90.0, 90.0, 1.0,
            "Maximum random vertical angle.", () -> this.mode.get() == Mode.CSGO);
    private final Setting<Double> csgoSpeed = this.sgGeneral.doubleSetting("CSGO Speed", 5.0, 0.0, 50.0, 1.0,
            "Frequency of rotation updates per second.", () -> this.mode.get() == Mode.CSGO);
    private final Setting<Double> customYaw = this.sgGeneral.doubleSetting("Yaw", 45.0, -180.0, 180.0, 1.0,
            "Fixed horizontal angle for Custom mode.", () -> this.mode.get() == Mode.Custom);
    private final Setting<Double> customPitch = this.sgGeneral.doubleSetting("Pitch", 90.0, -90.0, 90.0, 1.0,
            "Fixed vertical angle for Custom mode.", () -> this.mode.get() == Mode.Custom);

    private final Setting<IgnoreMode> iExp = this.sgIgnore.enumSetting("Ignore Experience", IgnoreMode.Down,
            "Rotation behavior while throwing XP bottles.");
    private final Setting<IgnoreMode> iPearl = this.sgIgnore.enumSetting("Ignore Pearl", IgnoreMode.FullIgnore,
            "Disables Anti-Aim when throwing pearls to prevent failed teleports.");
    private final Setting<IgnoreMode> iBow = this.sgIgnore.enumSetting("Ignore Bow", IgnoreMode.FullIgnore,
            "Disables Anti-Aim while using a bow to allow aiming.");
    private final Setting<IgnoreMode> iPotion = this.sgIgnore.enumSetting("Ignore Potion", IgnoreMode.FullIgnore,
            "Rotation behavior while throwing potions.");

    private double spinYaw;
    private long prevCsgo = 0L;
    private double csgoYaw = 0.0;
    private double csgoPitch = 0.0;

    public AntiAim() {
        super("Anti Aim", "Spoofs your rotations on the server to make you harder to hit or just to look funny.", SubCategory.MISC_COMBAT, true);
    }

    @Override
    public void onEnable() {
        this.spinYaw = 0.0;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        double yaw = 0.0;
        double pitch = 0.0;
        switch (this.mode.get()) {
            case Enemy:
                AbstractClientPlayerEntity target = this.getEnemy(this.range.get());
                if (target != null) {
                    yaw = RotationUtils.getYaw(target);
                } else {
                    yaw = BlackOut.mc.player.getYaw();
                }
                break;
            case Spin:
                this.spinYaw = this.spinYaw + this.spinSpeed.get();
                yaw = this.spinYaw;
                pitch = 0.0;
                break;
            case CSGO:
                if (System.currentTimeMillis() > this.prevCsgo + 1000.0 / this.csgoSpeed.get()) {
                    this.csgoYaw = MathHelper.lerp(ThreadLocalRandom.current().nextDouble(), this.csgoYawMin.get(), this.csgoYawMax.get());
                    this.csgoPitch = MathHelper.lerp(ThreadLocalRandom.current().nextDouble(), this.csgoPitchMin.get(), this.csgoPitchMax.get());
                    this.prevCsgo = System.currentTimeMillis();
                }

                yaw = this.csgoYaw;
                pitch = this.csgoPitch;
                break;
            case Custom:
                yaw = this.customYaw.get();
                pitch = this.customPitch.get();
        }

        if (this.mode.get() != Mode.Spin && this.mode.get() != Mode.Enemy) {
            switch (this.yawMode.get()) {
                case RelativeOwn:
                    yaw += BlackOut.mc.player.getYaw();
                    break;
                case RelativeEnemy:
                    AbstractClientPlayerEntity target = this.getEnemy(0.0);
                    if (target != null) {
                        yaw += RotationUtils.getYaw(target);
                    }
            }
        }

        IgnoreMode ignoreMode = this.getIgnore();
        if (ignoreMode == IgnoreMode.FullIgnore || ignoreMode == IgnoreMode.IgnoreYaw) {
            yaw = BlackOut.mc.player.getYaw();
        }

        switch (ignoreMode) {
            case FullIgnore:
            case IgnorePitch:
                pitch = BlackOut.mc.player.getPitch();
                break;
            case Down:
                pitch = 90.0;
                break;
            case Up:
                pitch = -90.0;
        }

        this.rotate((float) yaw, (float) pitch, RotationType.InstantOther, "");
    }

    private AbstractClientPlayerEntity getEnemy(double r) {
        AbstractClientPlayerEntity target = null;
        double dist = 1000.0;

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(player) && !(player.getHealth() <= 0.0F)) {
                double d = BlackOut.mc.player.distanceTo(player);
                if ((!(d > r) || !(r > 0.0)) && d < dist) {
                    target = player;
                    dist = d;
                }
            }
        }

        return target;
    }

    private IgnoreMode getIgnore() {
        IgnoreMode ignoreMode = this.getIgnore(BlackOut.mc.player.getMainHandStack().getItem());
        if (ignoreMode == IgnoreMode.Disabled) {
            ignoreMode = this.getIgnore(BlackOut.mc.player.getOffHandStack().getItem());
        }

        return ignoreMode;
    }

    private IgnoreMode getIgnore(Item item) {
        if (item == Items.EXPERIENCE_BOTTLE) {
            return this.iExp.get();
        } else if (item == Items.ENDER_PEARL) {
            return this.iPearl.get();
        } else if (item == Items.BOW || item == Items.CROSSBOW) {
            return this.iBow.get();
        } else {
            return item instanceof ThrowablePotionItem ? this.iPotion.get() : IgnoreMode.Disabled;
        }
    }

    public enum IgnoreMode {
        FullIgnore,
        IgnoreYaw,
        IgnorePitch,
        Down,
        Up,
        Disabled
    }

    public enum Mode {
        Enemy,
        Spin,
        CSGO,
        Custom
    }

    public enum YawMode {
        Normal,
        RelativeOwn,
        RelativeEnemy
    }
}
