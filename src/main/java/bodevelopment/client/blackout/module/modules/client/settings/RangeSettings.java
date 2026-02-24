package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RangeSettings extends SettingsModule {
    private static RangeSettings INSTANCE;

    private final SettingGroup sgInteract = this.addGroup("Interact");
    private final SettingGroup sgPlace = this.addGroup("Place");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgMine = this.addGroup("Mine");

    public final Setting<Double> interactRange = this.sgInteract.d("Interact Range", 5.2, 0.0, 6.0, 0.05,
            "Maximum distance for interacting with blocks (e.g., opening chests) with a clear line of sight.");
    public final Setting<Double> interactRangeWalls = this.sgInteract.d("Interact Range Walls", 5.2, 0.0, 6.0, 0.05,
            "Maximum interaction distance when the target block is behind a wall or other obstruction.");
    public final Setting<BlockRangeMode> interactRangeMode = this.sgInteract.e("Interact Range Mode", BlockRangeMode.NCP,
            "The calculation method for interaction distance. NCP uses block centers; Vanilla uses the closest point.");
    public final Setting<Double> interactBlockWidth = this.sgInteract.d("Interact Block Width", 1.0, 0.0, 2.0, 0.05,
            "Custom width for the interaction hit-box calculation.", () -> this.interactRangeMode.get() == BlockRangeMode.CustomBox);
    public final Setting<Double> interactBlockHeight = this.sgInteract.d("Interact Block Height", 1.0, 0.0, 2.0, 0.05,
            "Custom height for the interaction hit-box calculation.", () -> this.interactRangeMode.get() == BlockRangeMode.CustomBox);
    public final Setting<Double> interactHeight = this.sgInteract.d("Interact Height", 0.5, 0.0, 1.0, 0.05,
            "Specific height offset from the block bottom to calculate range from.", () -> this.interactRangeMode.get() == BlockRangeMode.Height);

    public final Setting<Double> placeRange = this.sgPlace.d("Place Range", 5.2, 0.0, 6.0, 0.05,
            "Maximum range for placing blocks when you have a direct line of sight.");
    public final Setting<Double> placeRangeWalls = this.sgPlace.d("Place Range Walls", 5.2, 0.0, 6.0, 0.05,
            "Maximum range for placing blocks through walls or obstructions.");
    public final Setting<BlockRangeMode> placeRangeMode = this.sgPlace.e("Place Range Mode", BlockRangeMode.NCP,
            "Method used to determine the distance between you and the placement position.");
    public final Setting<Double> blockWidth = this.sgPlace.d("Block Width", 1.0, 0.0, 2.0, 0.05,
            "Horizontal size of the target block box for distance checks.", () -> this.placeRangeMode.get() == BlockRangeMode.CustomBox);
    public final Setting<Double> blockHeight = this.sgPlace.d("Block Height", 1.0, 0.0, 2.0, 0.05,
            "Vertical size of the target block box for distance checks.", () -> this.placeRangeMode.get() == BlockRangeMode.CustomBox);
    public final Setting<Double> placeHeight = this.sgPlace.d("Place Height", 0.5, 0.0, 1.0, 0.05,
            "Manual vertical offset for placement range calculations.", () -> this.placeRangeMode.get() == BlockRangeMode.Height);
    // TODO : closestAttackWidth и closestAttackHeight нигде не используются
    public final Setting<Double> attackRange = this.sgAttack.d("Attack Range", 4.8, 0.0, 6.0, 0.05,
            "Standard reach for attacking entities within line of sight.");
    public final Setting<AttackRangeMode> attackRangeMode = this.sgAttack.e("Attack Range Mode", AttackRangeMode.NCP,
            "Logic used to calculate distance to entities. 'UpdatedNCP' provides the most accurate bypass for modern servers.");
    public final Setting<Double> closestAttackWidth = this.sgAttack.d("Closest Attack Width", 1.0, 0.0, 3.0, 0.05,
            "Width multiplier for the entity's hit-box during attack range checks.", () -> this.attackRangeMode.get().equals(AttackRangeMode.CustomBox));
    public final Setting<Double> closestAttackHeight = this.sgAttack.d("Closest Attack Height", 1.0, 0.0, 3.0, 0.05,
            "Height multiplier for the entity's hit-box during attack range checks.", () -> this.attackRangeMode.get().equals(AttackRangeMode.CustomBox));
    public final Setting<Double> attackRangeWalls = this.sgAttack.d("Attack Range Walls", 4.8, 0.0, 6.0, 0.05,
            "Reach for attacking entities when blocks are obstructing your view.");
    public final Setting<AttackRangeMode> wallAttackRangeMode = this.sgAttack.e("Wall Attack Range Mode", AttackRangeMode.NCP,
            "Calculation logic for attack range specifically when hitting through walls.");
    public final Setting<Double> closestWallAttackWidth = this.sgAttack.d("Closest Wall Attack Width", 1.0, 0.0, 3.0, 0.05,
            "Custom width for wall-attack distance calculation.", () -> this.wallAttackRangeMode.get().equals(AttackRangeMode.CustomBox));
    public final Setting<Double> closestWallAttackHeight = this.sgAttack.d("Closest Wall Attack Height", 1.0, 0.0, 3.0, 0.05,
            "Custom height for wall-attack distance calculation.", () -> this.wallAttackRangeMode.get().equals(AttackRangeMode.CustomBox));
    public final Setting<Boolean> reduce = this.sgAttack.b("Reduce", false,
            "Dynamically lowers your reach after each hit. Helps stay under the detection threshold of some anti-cheats.");
    public final Setting<Boolean> wallReduce = this.sgAttack.b("Wall Reduce", false,
            "Applies the range reduction logic specifically to wall interactions.");
    public final Setting<Double> reduceAmount = this.sgAttack.d("Reduce Amount", 0.8, 0.0, 6.0, 0.05,
            "The total amount of distance to be subtracted from your maximum reach during reduction.");
    public final Setting<Double> reduceStep = this.sgAttack.d("Reduce Step", 0.14, 0.0, 1.0, 0.01,
            "How much your reach decreases/increases per hit or tick during the reduction process.");

    public final Setting<Double> mineRange = this.sgMine.d("Mine Range", 5.2, 0.0, 6.0, 0.05,
            "Maximum distance for breaking blocks that are visible.");
    public final Setting<Double> mineRangeWalls = this.sgMine.d("Mine Range Walls", 5.2, 0.0, 6.0, 0.05,
            "Maximum distance for breaking blocks through other obstacles.");
    public final Setting<MineRangeMode> mineRangeMode = this.sgMine.e("Mine Range Mode", MineRangeMode.NCP,
            "The logic for calculating distance to a block you are mining.");
    public final Setting<Double> closestMiningWidth = this.sgMine.d("Closest Mine Width", 1.0, 0.0, 3.0, 0.05,
            "Custom width for mining distance hit-box.", () -> this.mineRangeMode.get() == MineRangeMode.CustomBox);
    public final Setting<Double> closestMiningHeight = this.sgMine.d("Closest Mine Height", 1.0, 0.0, 3.0, 0.05,
            "Custom height for mining distance hit-box.", () -> this.mineRangeMode.get() == MineRangeMode.CustomBox);
    public final Setting<Double> miningHeight = this.sgMine.d("Mine Height", 0.5, 0.0, 1.0, 0.05,
            "Vertical offset used when mining range mode is set to 'Height'.", () -> this.mineRangeMode.get() == MineRangeMode.Height);

    public double reducedAmount = 0.0;

    public RangeSettings() {
        super("Range", false, true);
        INSTANCE = this;
    }

    public static RangeSettings getInstance() {
        return INSTANCE;
    }

    @Event
    public void onAttack(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet && ((AccessorInteractEntityC2SPacket) packet).getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            Entity entity = BlackOut.mc.world.getEntityById(((AccessorInteractEntityC2SPacket) packet).getId());
            if (entity != null) {
                this.registerAttack(entity.getBoundingBox());
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.reducedAmount = MathHelper.clamp(this.reducedAmount, 0.0, this.reduceAmount.get());
    }

    private void registerAttack(Box bb) {
        double distance = this.attackRangeTo(bb, null);
        double range = SettingUtils.attackTrace(bb) ? this.attackRange.get() : this.attackRangeWalls.get();
        if (distance <= range - this.reduceAmount.get()) {
            this.reducedAmount = Math.max(this.reducedAmount - this.reduceStep.get(), 0.0);
        } else {
            this.reducedAmount = Math.min(this.reducedAmount + this.reduceStep.get(), this.reduceAmount.get());
        }
    }

    public boolean inInteractRange(BlockPos pos, Vec3d from) {
        double dist = this.interactRangeTo(pos, from);
        return dist >= 0.0 && dist <= (SettingUtils.interactTrace(pos) ? this.interactRange.get() : this.interactRangeWalls.get());
    }

    public boolean inInteractRangeNoTrace(BlockPos pos, Vec3d from) {
        double dist = this.interactRangeTo(pos, from);
        return dist >= 0.0 && dist <= Math.max(this.interactRange.get(), this.interactRangeWalls.get());
    }

    public double interactRangeTo(BlockPos pos, Vec3d from) {
        if (from == null) {
            from = BlackOut.mc.player.getPos();
        }

        from = from.add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0);
        Vec3d bottom = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        return switch (this.interactRangeMode.get()) {
            case NCP -> from.distanceTo(bottom.add(0.0, 0.5, 0.0));
            case Height -> from.distanceTo(bottom.add(0.0, this.interactHeight.get(), 0.0));
            case Vanilla -> from.distanceTo(OLEPOSSUtils.getClosest(BlackOut.mc.player.getEyePos(), bottom, 1.0, 1.0));
            case CustomBox -> from.distanceTo(
                    OLEPOSSUtils.getClosest(BlackOut.mc.player.getEyePos(), bottom, this.interactBlockWidth.get(), this.interactBlockHeight.get())
            );
        };
    }

    public boolean inPlaceRange(BlockPos pos, Vec3d from) {
        double dist = this.placeRangeTo(pos, from);

        return dist >= 0.0 && dist <= (SettingUtils.placeTrace(pos) ? this.placeRange.get() : this.placeRangeWalls.get());
    }

    public boolean inPlaceRangeNoTrace(BlockPos pos, Vec3d from) {
        double dist = this.placeRangeTo(pos, from);
        return dist >= 0.0 && dist <= Math.max(this.placeRange.get(), this.placeRangeWalls.get());
    }

    public double placeRangeTo(BlockPos pos, Vec3d from) {
        if (from == null) {
            from = BlackOut.mc.player.getPos();
        }

        from = from.add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0);
        Vec3d feet = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        return switch (this.placeRangeMode.get()) {
            case NCP -> from.distanceTo(feet.add(0.0, 0.5, 0.0));
            case Height -> from.distanceTo(feet.add(0.0, this.placeHeight.get(), 0.0));
            case Vanilla -> from.distanceTo(OLEPOSSUtils.getClosest(BlackOut.mc.player.getEyePos(), feet, 1.0, 1.0));
            case CustomBox ->
                    from.distanceTo(OLEPOSSUtils.getClosest(BlackOut.mc.player.getEyePos(), feet, this.blockWidth.get(), this.blockHeight.get()));
        };
    }

    public boolean inAttackRange(Box bb, Vec3d from) {
        boolean visible = SettingUtils.attackTrace(bb);
        return this.innerAttackRangeTo(bb, from, !visible) <= this.reducedRange(visible ? this.attackRange : this.attackRangeWalls, visible);
    }

    private double reducedRange(Setting<Double> distance, boolean walls) {
        return (walls ? this.wallReduce : this.reduce).get() ? distance.get() - this.reducedAmount : distance.get();
    }

    public boolean inAttackRangeNoTrace(Box bb, Vec3d from) {
        return this.attackRangeTo(bb, from) <= this.reducedRange(this.attackRange, false);
    }

    private double wallAttackRangeTo(Box bb, Vec3d from) {
        return this.innerAttackRangeTo(bb, from, true);
    }

    private double attackRangeTo(Box bb, Vec3d from) {
        return this.innerAttackRangeTo(bb, from, false);
    }

    public double innerAttackRangeTo(Box bb, Vec3d from, boolean walls) {
        AttackRangeMode mode = (walls ? this.wallAttackRangeMode : this.attackRangeMode).get();
        if (from == null) {
            from = BlackOut.mc.player.getPos();
        }

        if (mode != AttackRangeMode.Simple) {
            from = from.add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0);
        }
        return switch (mode) {
            case NCP -> {
                Vec3d feet = BoxUtils.feet(bb);
                yield from.distanceTo(new Vec3d(feet.x, Math.min(Math.max(from.getY(), bb.minY), bb.maxY), feet.z));
            }
            case Vanilla -> from.distanceTo(
                    OLEPOSSUtils.getClosest(
                            BlackOut.mc.player.getEyePos(), BoxUtils.feet(bb), Math.abs(bb.minX - bb.maxX), Math.abs(bb.minY - bb.maxY)
                    )
            );
            case Middle -> from.distanceTo(
                    new Vec3d((bb.minX + bb.maxX) / 2.0, (bb.minY + bb.maxY) / 2.0, (bb.minZ + bb.maxZ) / 2.0)
            );
            case CustomBox -> from.distanceTo(
                    OLEPOSSUtils.getClosest(
                            BlackOut.mc.player.getEyePos(),
                            BoxUtils.feet(bb),
                            Math.abs(bb.minX - bb.maxX) * this.closestWallAttackWidth.get(),
                            Math.abs(bb.minY - bb.maxY) * this.closestWallAttackHeight.get()
                    )
            );
            case UpdatedNCP -> {
                Vec3d feet = BoxUtils.feet(bb);
                yield from.distanceTo(new Vec3d(feet.x, Math.min(Math.max(from.getY(), bb.minY), bb.maxY), feet.z))
                        - this.getDistFromCenter(bb, feet, from);
            }
            case Simple -> from.distanceTo(BoxUtils.feet(bb));
        };
    }

    public double getDistFromCenter(Box bb, Vec3d feet, Vec3d from) {
        Vec3d pos = new Vec3d(feet.x, feet.y, feet.z);
        Vec3d vec1 = new Vec3d(from.getX() - pos.getX(), 0.0, from.getZ() - pos.getZ());
        double halfWidth = bb.getLengthX() / 2.0;
        if (vec1.length() < halfWidth * MathHelper.SQUARE_ROOT_OF_TWO) {
            return 0.0;
        } else {
            if (vec1.getZ() > 0.0) {
                ((IVec3d) pos).blackout_Client$setZ(pos.getZ() + halfWidth);
            } else if (vec1.getZ() < 0.0) {
                ((IVec3d) pos).blackout_Client$setZ(pos.getZ() - halfWidth);
            } else if (vec1.getX() > 0.0) {
                ((IVec3d) pos).blackout_Client$setX(pos.getX() + halfWidth);
            } else {
                ((IVec3d) pos).blackout_Client$setX(pos.getX() - halfWidth);
            }

            Vec3d vec2 = new Vec3d(pos.getX() - feet.getX(), 0.0, pos.getZ() - feet.getZ());
            double angle = RotationUtils.radAngle(vec1, vec2);
            if (angle > Math.PI / 4) {
                angle = (Math.PI / 2) - angle;
            }

            return angle >= 0.0 && angle <= Math.PI / 4 ? halfWidth / Math.cos(angle) : 0.0;
        }
    }

    public boolean inMineRange(BlockPos pos) {
        double dist = this.miningRangeTo(pos, null);
        return dist >= 0.0 && dist <= (SettingUtils.mineTrace(pos) ? this.mineRange.get() : this.mineRangeWalls.get());
    }

    public boolean inMineRangeNoTrace(BlockPos pos) {
        double dist = this.miningRangeTo(pos, null);
        return dist >= 0.0 && dist <= Math.max(this.mineRange.get(), this.mineRangeWalls.get());
    }

    public double miningRangeTo(BlockPos pos, Vec3d from) {
        if (from == null) {
            from = BlackOut.mc.player.getPos();
        }

        from = from.add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0);
        Vec3d feet = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        return switch (this.mineRangeMode.get()) {
            case NCP -> from.distanceTo(feet.add(0.0, 0.5, 0.0));
            case Height -> from.distanceTo(feet.add(0.0, this.miningHeight.get(), 0.0));
            case Vanilla -> from.distanceTo(OLEPOSSUtils.getClosest(BlackOut.mc.player.getEyePos(), feet, 1.0, 1.0));
            case CustomBox -> from.distanceTo(
                    OLEPOSSUtils.getClosest(BlackOut.mc.player.getEyePos(), feet, this.closestMiningWidth.get(), this.closestMiningHeight.get())
            );
        };
    }

    public enum AttackRangeMode {
        NCP,
        UpdatedNCP,
        Simple,
        Vanilla,
        Middle,
        CustomBox
    }

    public enum BlockRangeMode {
        NCP,
        Height,
        Vanilla,
        CustomBox
    }

    public enum MineRangeMode {
        NCP,
        Height,
        Vanilla,
        CustomBox
    }
}
