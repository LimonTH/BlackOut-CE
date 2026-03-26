package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.phys.AABB;
import java.util.Arrays;
import java.util.Comparator;

public class PistonPush extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgDelay = this.addGroup("Delay");
    private final SettingGroup sgSwing = this.addGroup("Swing");
    private final SettingGroup sgRender = this.addGroup("Render");

    public final Setting<Redstone> redstone = this.sgGeneral.enumSetting("Redstone Type", Redstone.Block, "The redstone component used to power the piston.");
    public final Setting<SwitchMode> pistonSwitch = this.sgGeneral.enumSetting("Piston Switch", SwitchMode.Silent, "The method used to select pistons in the hotbar.");
    public final Setting<SwitchMode> redstoneSwitch = this.sgGeneral.enumSetting("Redstone Switch", SwitchMode.Silent, "The method used to select redstone sources in the hotbar.");
    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("Pause on Consume", false, "Suspends operations while eating or drinking.");
    private final Setting<Boolean> onlyHole = this.sgGeneral.booleanSetting("Target in Hole Only", false, "Automatically disables the module if the target exits their safety hole.");
    private final Setting<Boolean> toggleMove = this.sgGeneral.booleanSetting("Auto-Disable on Move", false, "Disables the module if the target moves from their current position.");

    private final Setting<Double> prDelay = this.sgDelay.doubleSetting("Piston-to-Redstone Delay", 0.0, 0.0, 20.0, 0.1, "The wait time between placing the piston and the redstone source.");
    private final Setting<Double> rmDelay = this.sgDelay.doubleSetting("Redstone-to-Mine Delay", 0.0, 0.0, 20.0, 0.1, "The wait time between placing redstone and initiating the mining process.");
    private final Setting<Double> mpDelay = this.sgDelay.doubleSetting("Cycle Reset Delay", 0.0, 0.0, 20.0, 0.1, "The wait time after mining redstone before starting the next placement cycle.");

    private final Setting<Boolean> pistonSwing = this.sgSwing.booleanSetting("Piston Swing", true, "Visualizes the arm swing when placing a piston.");
    private final Setting<SwingHand> pistonHand = this.sgSwing.enumSetting("Piston Arm", SwingHand.RealHand, "The arm used for piston placement animations.");
    private final Setting<Boolean> redstoneSwing = this.sgSwing.booleanSetting("Redstone Swing", true, "Visualizes the arm swing when placing redstone.");
    private final Setting<SwingHand> redstoneHand = this.sgSwing.enumSetting("Redstone Arm", SwingHand.RealHand, "The arm used for redstone placement animations.");

    private final Setting<RenderShape> pistonShape = this.sgRender.enumSetting("Piston Highlight Shape", RenderShape.Full, "The geometry used to render the piston placement box.");
    private final Setting<BlackOutColor> psColor = this.sgRender.colorSetting("Piston Fill Color", new BlackOutColor(255, 255, 255, 50), "The color of the piston highlight faces.");
    private final Setting<BlackOutColor> plColor = this.sgRender.colorSetting("Piston Outline Color", new BlackOutColor(255, 255, 255, 255), "The color of the piston highlight edges.");
    private final Setting<RenderShape> redstoneShape = this.sgRender.enumSetting("Redstone Highlight Shape", RenderShape.Full, "The geometry used to render the redstone placement box.");
    private final Setting<BlackOutColor> rsColor = this.sgRender.colorSetting("Redstone Fill Color", new BlackOutColor(255, 0, 0, 50), "The color of the redstone highlight faces.");
    private final Setting<BlackOutColor> rlColor = this.sgRender.colorSetting("Redstone Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the redstone highlight edges.");

    private Phase phase = Phase.PLACE_PISTON;
    private long phaseTime = 0L;
    private boolean minedThisTick = false;
    private BlockPos pistonPos = null;
    private BlockPos redstonePos = null;
    private Direction pistonDir = null;
    private PlaceData pistonData = null;
    private PlaceData redstoneData = null;
    private BlockPos lastPiston = null;
    private BlockPos lastRedstone = null;
    private Direction lastDirection = null;
    private BlockPos startPos = null;
    private BlockPos currentPos = null;

    public PistonPush() {
        super("Piston Push", "Pushes targets out of defensive holes or surrounding cover by strategically placing and powering pistons.", SubCategory.OFFENSIVE, true);
    }

    @Override
    public void onEnable() {
        this.lastPiston = null;
        this.lastRedstone = null;
        this.lastDirection = null;
        this.startPos = null;
        this.phase = Phase.PLACE_PISTON;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.minedThisTick = false;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (PlayerUtils.isInGame()) {
            if (this.startPos != null && this.toggleMove.get() && !this.startPos.equals(this.currentPos)) {
                this.disable("moved");
            } else {
                this.update();
                if (this.pistonPos == null) {
                    this.lastPiston = null;
                    this.lastRedstone = this.redstonePos;
                    this.lastDirection = this.pistonDir;
                } else {
                    Render3DUtils.box(this.getBox(this.pistonPos), this.psColor.get(), this.plColor.get(), this.pistonShape.get());
                    Render3DUtils.box(this.getBox(this.redstonePos), this.rsColor.get(), this.rlColor.get(), this.redstoneShape.get());
                    boolean cycleComplete = this.phase == Phase.CYCLE_COMPLETE
                            && System.currentTimeMillis() - this.phaseTime > this.mpDelay.get() * 1000.0;
                    boolean posChanged = !this.pistonPos.equals(this.lastPiston)
                            || !this.redstonePos.equals(this.lastRedstone)
                            || !this.pistonDir.equals(this.lastDirection);
                    if (cycleComplete || posChanged) {
                        this.phase = Phase.PLACE_PISTON;
                    }

                    this.lastPiston = this.pistonPos;
                    this.lastRedstone = this.redstonePos;
                    this.lastDirection = this.pistonDir;
                    if (!this.pauseEat.get() || !BlackOut.mc.player.isUsingItem()) {
                        this.placePiston();
                        this.placeRedstone();
                        this.mineUpdate();
                    }
                }
            }
        }
    }

    private void placePiston() {
        if (this.phase != Phase.PLACE_PISTON) return;

        InteractionHand hand = InvUtils.getHand(Items.PISTON);
        FindResult result = this.pistonSwitch.get().find(Items.PISTON);

        if (hand != null || result.wasFound()) {
            if (BlackOut.mc.player.onGround()) {
                if (!EntityUtils.intersects(BoxUtils.get(this.pistonPos), entity -> !entity.isSpectator() && !(entity instanceof ItemEntity))) {
                    float yaw = this.pistonDir.toYRot();
                    float pitch = 0.0f;

                    if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotation.rotateBlock(this.pistonData, RotationType.BlockPlace, "piston")) {

                        this.sendPacket(new ServerboundMovePlayerPacket.Rot(yaw, pitch, Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision));
                        boolean switched = false;
                        if (hand == null) {
                            switched = this.pistonSwitch.get().swap(result.slot());
                        }

                        if (hand != null || switched) {
                            hand = (hand == null) ? InteractionHand.MAIN_HAND : hand;

                            this.placeBlock(hand, this.pistonData.pos().getCenter(), this.pistonData.dir(), this.pistonData.pos());

                            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                                this.rotation.end("piston");
                            }

                            this.phase = Phase.PLACE_REDSTONE;
                            this.phaseTime = System.currentTimeMillis();

                            if (this.pistonSwing.get()) {
                                this.clientSwing(this.pistonHand.get(), hand);
                            }

                            if (switched) {
                                this.pistonSwitch.get().swapBack();
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeRedstone() {
        if (this.phase != Phase.PLACE_REDSTONE) return;
        if (System.currentTimeMillis() - this.phaseTime < this.prDelay.get() * 1000.0) return;

        InteractionHand hand = InvUtils.getHand(this.redstone.get().i);
        FindResult result = this.redstoneSwitch.get().find(this.redstone.get().i);
        boolean available = hand != null;
        if (!available) {
            available = result.wasFound();
        }

        if (available) {
            if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotation.rotateBlock(this.redstoneData, RotationType.BlockPlace, "redstone")) {
                boolean switched = false;
                if (hand == null) {
                    switched = this.redstoneSwitch.get().swap(result.slot());
                }

                if (hand != null || switched) {
                    hand = hand == null ? InteractionHand.MAIN_HAND : hand;
                    this.placeBlock(hand, this.redstoneData.pos().getCenter(), this.redstoneData.dir(), this.redstoneData.pos());
                    if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                        this.rotation.end("redstone");
                    }

                    this.phase = Phase.MINE;
                    this.phaseTime = System.currentTimeMillis();

                    if (this.redstoneSwing.get()) {
                        this.clientSwing(this.redstoneHand.get(), hand);
                    }

                    if (switched) {
                        this.redstoneSwitch.get().swapBack();
                    }
                }
            }
        }
    }

    private AABB getBox(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    private void mineUpdate() {
        if (this.phase != Phase.MINE && this.phase != Phase.CYCLE_COMPLETE) return;
        if (this.minedThisTick) return;
        if (System.currentTimeMillis() - this.phaseTime < this.rmDelay.get() * 1000.0) return;

        if (this.redstonePos != null) {
            if (this.redstone.get() != Redstone.Torch
                    || BlackOut.mc.level.getBlockState(this.redstonePos).getBlock() instanceof RedstoneTorchBlock) {
                if (this.redstone.get() != Redstone.Block
                        || BlackOut.mc.level.getBlockState(this.redstonePos).getBlock() == Blocks.REDSTONE_BLOCK) {
                    AutoMine autoMine = AutoMine.getInstance();
                    if (!autoMine.enabled || !this.redstonePos.equals(autoMine.minePos)) {
                        if (autoMine.enabled) {
                            autoMine.onStart(this.redstonePos);
                        } else {
                            Direction mineDir = SettingUtils.getPlaceOnDirection(this.redstonePos);
                            if (mineDir != null) {
                                this.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.redstonePos, mineDir));
                                this.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.redstonePos, mineDir));
                            }
                        }

                        if (this.phase == Phase.MINE) {
                            this.phase = Phase.CYCLE_COMPLETE;
                            this.phaseTime = System.currentTimeMillis();
                        }

                        this.minedThisTick = true;
                    }
                }
            }
        }
    }

    private void update() {
        this.pistonPos = null;

        for (AbstractClientPlayer player : BlackOut.mc.level.players()) {
            if (!Managers.FRIENDS.isFriend(player)
                    && player != BlackOut.mc.player
                    && !(BlackOut.mc.player.distanceTo(player) > 10.0F)
                    && !(player.getHealth() <= 0.0F)
                    && !player.isSpectator()
                    && (BlockUtils.hasCollision(player.blockPosition()) || !this.onlyHole.get() || HoleUtils.inHole(player.blockPosition()))) {
                this.updatePos(player);
                if (this.pistonPos != null) {
                    return;
                }
            }
        }
    }

    private void updatePos(Player player) {
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        if (!BlockUtils.hasCollision(eyePos.above())) {
            for (Direction dir : Direction.Plane.HORIZONTAL
                    .stream()
                    .sorted(Comparator.comparingDouble(d -> eyePos.relative(d).getCenter().distanceTo(BlackOut.mc.player.getEyePosition())))
                    .toList()) {
                this.resetPos();
                BlockPos pos = eyePos.relative(dir);
                if (this.upCheck(pos)
                        && (
                        BlockUtils.replaceable(pos)
                                || BlackOut.mc.level.getBlockState(pos).getBlock() instanceof PistonBaseBlock
                                || BlackOut.mc.level.getBlockState(pos).getBlock() == Blocks.MOVING_PISTON
                )
                        && !BlockUtils.hasCollision(eyePos.relative(dir.getOpposite()))
                        && !BlockUtils.hasCollision(eyePos.relative(dir.getOpposite()).above())
                        && BlockUtils.hasCollision(eyePos.relative(dir.getOpposite()).below())) {
                    PlaceData data = SettingUtils.getPlaceData(pos);
                    if (data != null && data.valid()) {
                        this.pistonData = data;
                        this.pistonDir = dir;
                        this.updateRedstone(pos);
                        if (this.redstonePos != null) {
                            if (this.startPos == null) {
                                this.startPos = player.blockPosition();
                            }

                            this.currentPos = player.blockPosition();
                            this.pistonPos = pos;
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updateRedstone(BlockPos pos) {
        if (this.redstone.get() == Redstone.Torch) {
            for (Direction direction : Arrays.stream(Direction.values())
                    .sorted(Comparator.comparingDouble(i -> pos.relative(i).getCenter().distanceTo(BlackOut.mc.player.getEyePosition())))
                    .toList()) {
                if (direction != this.pistonDir.getOpposite() && direction != Direction.DOWN && direction != Direction.UP) {
                    BlockPos position = pos.relative(direction);
                    if (BlockUtils.replaceable(position) || BlackOut.mc.level.getBlockState(position).getBlock() instanceof RedstoneTorchBlock) {
                        this.redstoneData = SettingUtils.getPlaceData(
                                position,
                                null,
                                (p, d) -> {
                                    if (d == Direction.UP && !BlockUtils.isFaceSturdy(position.below())) {
                                        return false;
                                    } else if (direction == d.getOpposite()) {
                                        return false;
                                    } else if (pos.equals(p)) {
                                        return false;
                                    } else {
                                        return !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof TorchBlock) && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof PistonBaseBlock)
                                                && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof PistonHeadBlock);
                                    }
                                }
                        );
                        if (this.redstoneData.valid() && SettingUtils.inPlaceRange(this.redstoneData.pos()) && SettingUtils.inMineRange(position)) {
                            this.redstonePos = position;
                            return;
                        }
                    }
                }
            }

            this.redstonePos = null;
        } else {
            for (Direction directionx : Arrays.stream(Direction.values())
                    .sorted(Comparator.comparingDouble(i -> pos.relative(i).getCenter().distanceTo(BlackOut.mc.player.getEyePosition())))
                    .toList()) {
                if (directionx != this.pistonDir.getOpposite() && directionx != Direction.DOWN) {
                    BlockPos position = pos.relative(directionx);
                    if ((BlockUtils.replaceable(position) || BlackOut.mc.level.getBlockState(position).getBlock() == Blocks.REDSTONE_BLOCK)
                            && !EntityUtils.intersects(BoxUtils.get(position), entity -> !entity.isSpectator() && entity instanceof Player)) {
                        this.redstoneData = SettingUtils.getPlaceData(position, (p, d) -> pos.equals(p), null);
                        if (this.redstoneData.valid()) {
                            this.redstonePos = position;
                            return;
                        }
                    }
                }
            }

            this.redstonePos = null;
        }
    }

    private boolean upCheck(BlockPos pos) {
        double dx = BlackOut.mc.player.getEyePosition().x - pos.getX() - 0.5;
        double dz = BlackOut.mc.player.getEyePosition().z - pos.getZ() - 0.5;
        return Math.sqrt(dx * dx + dz * dz) > Math.abs(BlackOut.mc.player.getEyePosition().y - pos.getY() - 0.5);
    }

    private void resetPos() {
        this.pistonPos = null;
        this.redstonePos = null;
        this.pistonDir = null;
        this.pistonData = null;
        this.redstoneData = null;
    }

    enum Phase {
        PLACE_PISTON,
        PLACE_REDSTONE,
        MINE,
        CYCLE_COMPLETE
    }

    public enum Redstone {
        Torch(Items.REDSTONE_TORCH, Blocks.REDSTONE_TORCH),
        Block(Items.REDSTONE_BLOCK, Blocks.REDSTONE_BLOCK);

        public final Item i;
        public final Block b;

        Redstone(Item i, Block b) {
            this.i = i;
            this.b = b;
        }
    }
}
