package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.NCPRaytracer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

public class RaytraceSettings extends SettingsModule {
    private static RaytraceSettings INSTANCE;

    private final SettingGroup sgInteract = this.addGroup("Interact");
    private final SettingGroup sgPlace = this.addGroup("Place");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgMine = this.addGroup("Mine");

    public final Setting<Boolean> interactTrace = this.sgInteract.booleanSetting("Interact Trace", false,
            "Performs a visibility check before interacting with blocks to ensure they aren't behind obstacles.");
    private final Setting<PlaceTraceMode> interactMode = this.sgInteract.enumSetting("Interact Mode", PlaceTraceMode.SinglePoint,
            "Determines how many points on the block are checked for visibility.");
    private final Setting<Double> interactHeight = this.sgInteract.doubleSetting("Interact Height", 0.5, -2.0, 2.0, 0.05,
            "The vertical offset (from block bottom) for the single-point visibility check.");
    private final Setting<Double> interactHeight1 = this.sgInteract.doubleSetting("Interact Height 1", 0.25, -2.0, 2.0, 0.05,
            "The first vertical offset for the double-point visibility check.");
    private final Setting<Double> interactHeight2 = this.sgInteract.doubleSetting("Interact Height 2", 0.75, -2.0, 2.0, 0.05,
            "The second vertical offset for the double-point visibility check.");
    private final Setting<Double> interactExposure = this.sgInteract.doubleSetting("Interact Exposure", 50.0, 0.0, 100.0, 1.0,
            "The percentage of the block that must be visible to pass the check (checked via 27-point grid).");
    private final Setting<Boolean> interactNCP = this.sgInteract.booleanSetting("Interact NCP", false,
            "Uses specialized NoCheatPlus raytracing logic for interaction checks.");

    public final Setting<Boolean> placeTrace = this.sgPlace.booleanSetting("Place Trace", false,
            "Validates that the placement position is visible before attempting to place a block.");
    private final Setting<PlaceTraceMode> placeMode = this.sgPlace.enumSetting("Place Mode", PlaceTraceMode.SinglePoint,
            "Calculation method for placement visibility, from single points to full block exposure.");
    private final Setting<Double> placeHeight = this.sgPlace.doubleSetting("Place Height", 0.5, -2.0, 2.0, 0.05,
            "Target height for a single-point placement raytrace.");
    private final Setting<Double> placeHeight1 = this.sgPlace.doubleSetting("Place Height 1", 0.25, -2.0, 2.0, 0.05,
            "Primary height for double-point placement check.");
    private final Setting<Double> placeHeight2 = this.sgPlace.doubleSetting("Place Height 2", 0.75, -2.0, 2.0, 0.05,
            "Secondary height for double-point placement check.");
    private final Setting<Double> placeExposure = this.sgPlace.doubleSetting("Place Exposure", 50.0, 0.0, 100.0, 1.0,
            "Required visibility percentage of the target placement area.");
    private final Setting<Boolean> placeNCP = this.sgPlace.booleanSetting("Place NCP", false,
            "Enables NCP-compatible raytrace logic for block placement.");

    public final Setting<Boolean> attackTrace = this.sgAttack.booleanSetting("Attack Trace", false,
            "Prevents attacking entities that are fully occluded by blocks.");
    private final Setting<AttackTraceMode> attackMode = this.sgAttack.enumSetting("Attack Mode", AttackTraceMode.SinglePoint,
            "Visibility logic for entities. 'Exposure' is most accurate but heavier on performance.");
    private final Setting<Double> attackHeight = this.sgAttack.doubleSetting("Attack Height", 0.5, -2.0, 2.0, 0.05,
            "Vertical target point on the entity's hitbox for single-point checks.");
    private final Setting<Double> attackHeight1 = this.sgAttack.doubleSetting("Attack Height 1", 0.25, -2.0, 2.0, 0.05,
            "First vertical check point for entity double-trace.");
    private final Setting<Double> attackHeight2 = this.sgAttack.doubleSetting("Attack Height 2", 0.75, -2.0, 2.0, 0.05,
            "Second vertical check point for entity double-trace.");
    private final Setting<Double> attackExposure = this.sgAttack.doubleSetting("Attack Exposure", 50.0, 0.0, 100.0, 1.0,
            "Required hitbox visibility percentage to allow an attack.");
    private final Setting<Boolean> attackNCP = this.sgAttack.booleanSetting("Attack NCP", false,
            "Applies NCP-compliant raytracing specifically for entity combat.");

    public final Setting<Boolean> mineTrace = this.sgMine.booleanSetting("Mine Trace", false,
            "Verifies block visibility before starting the mining process.");
    private final Setting<PlaceTraceMode> mineMode = this.sgMine.enumSetting("Mine Mode", PlaceTraceMode.SinglePoint,
            "Trace complexity for mining, ranging from center-point to multi-side checks.");
    private final Setting<Double> mineHeight = this.sgMine.doubleSetting("Mine Height", 0.5, -2.0, 2.0, 0.05,
            "Height above block bottom to check for mining visibility.");
    private final Setting<Double> mineHeight1 = this.sgMine.doubleSetting("Mine Height 1", 0.25, -2.0, 2.0, 0.05,
            "First height point for double-point mining trace.");
    private final Setting<Double> mineHeight2 = this.sgMine.doubleSetting("Mine Height 2", 0.75, -2.0, 2.0, 0.05,
            "Second height point for double-point mining trace.");
    private final Setting<Double> mineExposure = this.sgMine.doubleSetting("Mine Exposure", 50.0, 0.0, 100.0, 1.0,
            "Required block surface exposure for mining to commence.");
    private final Setting<Boolean> mineNCP = this.sgMine.booleanSetting("Mine NCP", false,
            "Utilizes NCP-style raytracing for mining visibility checks.");

    private RaycastContext raycastContext;
    private BlockHitResult result;
    private int hit = 0;

    public RaytraceSettings() {
        super("Raytrace", false, true);
        INSTANCE = this;
    }

    public static RaytraceSettings getInstance() {
        return INSTANCE;
    }

    public boolean interactTrace(BlockPos pos) {
        return !this.interactTrace.get() || this.blockTrace(
                pos, this.interactNCP.get(), this.interactMode, this.interactExposure, this.interactHeight, this.interactHeight1, this.interactHeight2
        );
    }

    public boolean placeTrace(BlockPos pos) {
        return !this.placeTrace.get() || this.blockTrace(pos, this.placeNCP.get(), this.placeMode, this.placeExposure, this.placeHeight, this.placeHeight1, this.placeHeight2);
    }

    private boolean blockTrace(
            BlockPos pos,
            boolean ncp,
            Setting<PlaceTraceMode> mode,
            Setting<Double> exposure,
            Setting<Double> ph,
            Setting<Double> ph1,
            Setting<Double> ph2
    ) {
        this.updateContext();
        Vec3d vec;
        switch (mode.get()) {
            case SinglePoint:
                Vec3d to = new Vec3d(pos.getX() + 0.5, pos.getY() + ph.get(), pos.getZ() + 0.5);
                if (ncp) {
                    return this.ncpRaytrace(to, BoxUtils.get(pos));
                }

                ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(to);
                this.result = DamageUtils.raycast(this.raycastContext, false);
                return this.result.getType() == HitResult.Type.MISS || this.result.getBlockPos().equals(pos);
            case DoublePoint:
                Vec3d to1 = new Vec3d(pos.getX() + 0.5, pos.getY() + ph1.get(), pos.getZ() + 0.5);
                Vec3d to2 = new Vec3d(pos.getX() + 0.5, pos.getY() + ph2.get(), pos.getZ() + 0.5);
                if (ncp) {
                    return this.ncpRaytrace(to1, BoxUtils.get(pos)) || this.ncpRaytrace(to2, BoxUtils.get(pos));
                }

                ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(to1);
                this.result = DamageUtils.raycast(this.raycastContext, false);
                if (this.result.getBlockPos().equals(pos)) {
                    return true;
                }

                ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(to2);
                this.result = DamageUtils.raycast(this.raycastContext, false);
                return this.result.getType() == HitResult.Type.MISS || this.result.getBlockPos().equals(pos);
            case Sides:
                vec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                for (Direction dir : Direction.values()) {
                    Vec3d vec2 = vec.add(dir.getOffsetX() / 2.0F, dir.getOffsetY() / 2.0F, dir.getOffsetZ() / 2.0F);
                    if (ncp) {
                        return this.ncpRaytrace(vec2, BoxUtils.get(pos));
                    }

                    ((IRaycastContext) this.raycastContext)
                            .blackout_Client$setEnd(vec.add(dir.getOffsetX() / 2.0F, dir.getOffsetY() / 2.0F, dir.getOffsetZ() / 2.0F));
                    this.result = DamageUtils.raycast(this.raycastContext, false);
                    if (this.result.getType() == HitResult.Type.MISS || this.result.getBlockPos().equals(pos)) {
                        return true;
                    }
                }
                break;
            case Exposure:
                vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                this.hit = 0;

                for (int x = 0; x <= 2; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int zx = 0; zx <= 2; zx++) {
                            Vec3d vec2 = vec.add(0.1 + x * 0.4, 0.1 + y * 0.4, 0.1 + zx * 0.4);
                            if (ncp) {
                                if (this.ncpRaytrace(vec2, BoxUtils.get(pos)) && ++this.hit >= exposure.get() / 100.0 * 27.0) {
                                    return true;
                                }
                            } else {
                                ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(vec2);
                                this.result = DamageUtils.raycast(this.raycastContext, false);
                                if (this.result.getType() == HitResult.Type.MISS || this.result.getBlockPos().equals(pos)) {
                                    this.hit++;
                                    if (this.hit >= exposure.get() / 100.0 * 27.0) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case Any:
                vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                this.hit = 0;

                for (int x = 0; x <= 2; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int z = 0; z <= 2; z++) {
                            Vec3d vec2 = vec.add(0.1 + x * 0.4, 0.1 + y * 0.4, 0.1 + z * 0.4);
                            if (ncp) {
                                if (this.ncpRaytrace(vec2, BoxUtils.get(pos))) {
                                    return true;
                                }
                            } else {
                                ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(vec2);
                                this.result = DamageUtils.raycast(this.raycastContext, false);
                                if (this.result.getType() == HitResult.Type.MISS || this.result.getBlockPos().equals(pos)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
        }

        return false;
    }

    public boolean attackTrace(Box box) {
        if (!this.attackTrace.get()) {
            return true;
        } else {
            this.updateContext();
            Vec3d vec;
            double xl;
            double yl;
            double zl;
            switch (this.attackMode.get()) {
                case SinglePoint:
                    Vec3d to = new Vec3d(
                            (box.minX + box.maxX) / 2.0, box.minY + this.attackHeight.get(), (box.minZ + box.maxZ) / 2.0
                    );
                    if (this.attackNCP.get()) {
                        return this.ncpRaytrace(to, box);
                    }

                    ((IRaycastContext) DamageUtils.raycastContext)
                            .blackout_Client$set(BlackOut.mc.player.getEyePos(), to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player);
                    return DamageUtils.raycast(DamageUtils.raycastContext, false).getType() != HitResult.Type.BLOCK;
                case DoublePoint:
                    Vec3d to1 = new Vec3d(
                            (box.minX + box.maxX) / 2.0, box.minY + this.attackHeight1.get(), (box.minZ + box.maxZ) / 2.0
                    );
                    Vec3d to2 = new Vec3d(
                            (box.minX + box.maxX) / 2.0, box.minY + this.attackHeight2.get(), (box.minZ + box.maxZ) / 2.0
                    );
                    if (!this.attackNCP.get()) {
                        ((IRaycastContext) DamageUtils.raycastContext)
                                .blackout_Client$set(BlackOut.mc.player.getEyePos(), to1, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player);
                        if (DamageUtils.raycast(DamageUtils.raycastContext, false).getType() != HitResult.Type.BLOCK) {
                            return true;
                        }

                        ((IRaycastContext) DamageUtils.raycastContext)
                                .blackout_Client$set(BlackOut.mc.player.getEyePos(), to2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player);
                        return DamageUtils.raycast(DamageUtils.raycastContext, false).getType() != HitResult.Type.BLOCK;
                    }

                    return this.ncpRaytrace(to1, box) || this.ncpRaytrace(to2, box);
                case Exposure:
                    vec = new Vec3d(box.minX, box.minY, box.minZ);
                    xl = box.getLengthX();
                    yl = box.getLengthY();
                    zl = box.getLengthZ();
                    this.hit = 0;

                    for (int x = 0; x <= 2; x++) {
                        for (int y = 0; y <= 2; y++) {
                            for (int zx = 0; zx <= 2; zx++) {
                                Vec3d vec2 = vec.add(
                                        MathHelper.lerp(x / 2.0F, xl * 0.1, xl * 0.9),
                                        MathHelper.lerp(y / 2.0F, yl * 0.1, yl * 0.9),
                                        MathHelper.lerp(zx / 2.0F, zl * 0.1, zl * 0.9)
                                );
                                if (this.attackNCP.get()) {
                                    if (this.ncpRaytrace(vec2, box) && ++this.hit >= this.attackExposure.get() / 100.0 * 27.0) {
                                        return true;
                                    }
                                } else {
                                    ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(vec2);
                                    this.result = DamageUtils.raycast(this.raycastContext, false);
                                    if (this.result.getType() != HitResult.Type.BLOCK) {
                                        this.hit++;
                                        if (this.hit >= this.attackExposure.get() / 100.0 * 27.0) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case Any:
                    vec = new Vec3d(box.minX, box.minY, box.minZ);
                    xl = box.getLengthX();
                    yl = box.getLengthY();
                    zl = box.getLengthZ();

                    for (int x = 0; x <= 2; x++) {
                        for (int y = 0; y <= 2; y++) {
                            for (int z = 0; z <= 2; z++) {
                                Vec3d vec2 = vec.add(
                                        MathHelper.lerp(x / 2.0F, xl * 0.1, xl * 0.9),
                                        MathHelper.lerp(y / 2.0F, yl * 0.1, yl * 0.9),
                                        MathHelper.lerp(z / 2.0F, zl * 0.1, zl * 0.9)
                                );
                                if (this.attackNCP.get()) {
                                    if (this.ncpRaytrace(vec2, box)) {
                                        return true;
                                    }
                                } else {
                                    ((IRaycastContext) this.raycastContext).blackout_Client$setEnd(vec2);
                                    this.result = DamageUtils.raycast(this.raycastContext, false);
                                    if (this.result.getType() != HitResult.Type.BLOCK) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
            }

            return false;
        }
    }

    public boolean mineTrace(BlockPos pos) {
        return !this.mineTrace.get() || this.blockTrace(pos, this.mineNCP.get(), this.mineMode, this.mineExposure, this.mineHeight, this.mineHeight1, this.mineHeight2);
    }

    private boolean ncpRaytrace(Vec3d to, Box box) {
        return NCPRaytracer.raytrace(BlackOut.mc.player.getEyePos(), to, box);
    }

    private void updateContext() {
        if (this.raycastContext == null) {
            this.raycastContext = new RaycastContext(BlackOut.mc.player.getEyePos(), null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, BlackOut.mc.player);
        } else {
            ((IRaycastContext) this.raycastContext).blackout_Client$setStart(BlackOut.mc.player.getEyePos());
        }
    }

    public enum AttackTraceMode {
        SinglePoint,
        DoublePoint,
        Exposure,
        Any
    }

    public enum PlaceTraceMode {
        SinglePoint,
        DoublePoint,
        Sides,
        Exposure,
        Any
    }
}
