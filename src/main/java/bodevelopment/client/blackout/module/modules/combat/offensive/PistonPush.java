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
import net.minecraft.block.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Comparator;

public class PistonPush extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Redstone> redstone = this.sgGeneral.enumSetting("Redstone", Redstone.Block, "What kind of redstone to use", () -> true);
    public final Setting<SwitchMode> pistonSwitch = this.sgGeneral
            .enumSetting("Piston modeMode", SwitchMode.Silent, "Method of switching. Silent is the most reliable.", () -> true);
    public final Setting<SwitchMode> redstoneSwitch = this.sgGeneral
            .enumSetting("Redstone Switch", SwitchMode.Silent, "Method of switching. Silent is the most reliable.", () -> true);
    private final SettingGroup sgDelay = this.addGroup("Delay");
    private final SettingGroup sgSwing = this.addGroup("Swing");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("PauseEat", false, "Do we stop while eating");
    private final Setting<Boolean> onlyHole = this.sgGeneral.booleanSetting("Only Hole", false, "Toggles when enemy gets out of the hole.");
    private final Setting<Boolean> toggleMove = this.sgGeneral.booleanSetting("Toggle Move", false, "Toggles when enemy moves.");
    private final Setting<Double> prDelay = this.sgDelay
            .doubleSetting("Piston > Redstone", 0.0, 0.0, 20.0, 0.1, "How many seconds to wait between placing piston and redstone.");
    private final Setting<Double> rmDelay = this.sgDelay
            .doubleSetting("Redstone > Mine", 0.0, 0.0, 20.0, 0.1, "How many seconds to wait between placing redstone and starting to mine it.");
    private final Setting<Double> mpDelay = this.sgDelay
            .doubleSetting("Mine > Piston", 0.0, 0.0, 20.0, 0.1, "How many seconds to wait after mining the redstone before starting a new cycle.");
    private final Setting<Boolean> pistonSwing = this.sgSwing.booleanSetting("Piston Swing", true, "Renders swing animation when placing a piston.");
    private final Setting<SwingHand> pistonHand = this.sgSwing.enumSetting("Piston Hand", SwingHand.RealHand, "Which hand should be swung.", this.pistonSwing::get);
    private final Setting<Boolean> redstoneSwing = this.sgSwing.booleanSetting("Redstone Swing", true, "Renders swing animation when placing redstone.");
    private final Setting<SwingHand> redstoneHand = this.sgSwing.enumSetting("Redstone Hand", SwingHand.RealHand, "Which hand should be swung.", this.redstoneSwing::get);
    private final Setting<RenderShape> pistonShape = this.sgRender.enumSetting("Piston Render Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> psColor = this.sgRender.colorSetting("Piston Side Color", new BlackOutColor(255, 255, 255, 50), "Color of rendered sides.");
    private final Setting<BlackOutColor> plColor = this.sgRender.colorSetting("Piston Line Color", new BlackOutColor(255, 255, 255, 255), "Color of rendered lines.");
    private final Setting<RenderShape> redstoneShape = this.sgRender.enumSetting("Redstone Render Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> rsColor = this.sgRender.colorSetting("Redstone Side Color", new BlackOutColor(255, 0, 0, 50), "Color of rendered sides.");
    private final Setting<BlackOutColor> rlColor = this.sgRender.colorSetting("Redstone Line Color", new BlackOutColor(255, 0, 0, 255), "Color of rendered lines.");
    private long pistonTime = 0L;
    private long redstoneTime = 0L;
    private long mineTime = 0L;
    private boolean minedThisTick = false;
    private boolean pistonPlaced = false;
    private boolean redstonePlaced = false;
    private boolean mined = false;
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
        super("Piston Push", "Pushes people out of their safe holes.", SubCategory.OFFENSIVE, true);
    }

    @Override
    public void onEnable() {
        this.lastPiston = null;
        this.lastRedstone = null;
        this.lastDirection = null;
        this.startPos = null;
        this.redstonePlaced = false;
        this.pistonPlaced = false;
        this.mined = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.minedThisTick = false;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
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
                    if (System.currentTimeMillis() - this.mineTime > this.mpDelay.get() * 1000.0 && this.redstonePlaced && this.pistonPlaced && this.mined
                            || !this.pistonPos.equals(this.lastPiston)
                            || !this.redstonePos.equals(this.lastRedstone)
                            || !this.pistonDir.equals(this.lastDirection)) {
                        this.redstonePlaced = false;
                        this.pistonPlaced = false;
                        this.mined = false;
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
        if (!this.pistonPlaced) {
            Hand hand = OLEPOSSUtils.getHand(Items.PISTON);
            FindResult result = this.pistonSwitch.get().find(Items.PISTON);

            if (hand != null || result.wasFound()) {
                if (BlackOut.mc.player.isOnGround()) {
                    if (!EntityUtils.intersects(BoxUtils.get(this.pistonPos), entity -> !entity.isSpectator() && !(entity instanceof ItemEntity))) {
                        float yaw = this.pistonDir.asRotation();
                        float pitch = 0.0f;

                        if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(this.pistonData, RotationType.BlockPlace, "piston")) {

                            this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, Managers.PACKET.isOnGround()));
                            boolean switched = false;
                            if (hand == null) {
                                switched = this.pistonSwitch.get().swap(result.slot());
                            }

                            if (hand != null || switched) {
                                hand = (hand == null) ? Hand.MAIN_HAND : hand;

                                this.placeBlock(hand, this.pistonData.pos().toCenterPos(), this.pistonData.dir(), this.pistonData.pos());

                                if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                                    this.end("piston");
                                }

                                this.pistonTime = System.currentTimeMillis();
                                this.pistonPlaced = true;

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
    }

    private void placeRedstone() {
        if (this.pistonPlaced && !this.redstonePlaced) {
            if (!(System.currentTimeMillis() - this.pistonTime < this.prDelay.get() * 1000.0)) {
                Hand hand = OLEPOSSUtils.getHand(this.redstone.get().i);
                FindResult result = this.redstoneSwitch.get().find(this.redstone.get().i);
                boolean available = hand != null;
                if (!available) {
                    available = result.wasFound();
                }

                if (available) {
                    if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(this.redstoneData, RotationType.BlockPlace, "redstone")) {
                        boolean switched = false;
                        if (hand == null) {
                            switched = this.redstoneSwitch.get().swap(result.slot());
                        }

                        if (hand != null || switched) {
                            hand = hand == null ? Hand.MAIN_HAND : hand;
                            this.placeBlock(hand, this.redstoneData.pos().toCenterPos(), this.redstoneData.dir(), this.redstoneData.pos());
                            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                                this.end("redstone");
                            }

                            this.redstonePlaced = true;
                            this.redstoneTime = System.currentTimeMillis();
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
        }
    }

    private Box getBox(BlockPos pos) {
        return new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    private void mineUpdate() {
        if (this.pistonPlaced && this.redstonePlaced) {
            if (!this.minedThisTick) {
                if (!(System.currentTimeMillis() - this.redstoneTime < this.rmDelay.get() * 1000.0)) {
                    if (this.redstonePos != null) {
                        if (this.redstone.get() != Redstone.Torch
                                || BlackOut.mc.world.getBlockState(this.redstonePos).getBlock() instanceof RedstoneTorchBlock) {
                            if (this.redstone.get() != Redstone.Block
                                    || BlackOut.mc.world.getBlockState(this.redstonePos).getBlock() == Blocks.REDSTONE_BLOCK) {
                                AutoMine autoMine = AutoMine.getInstance();
                                if (!autoMine.enabled || !this.redstonePos.equals(autoMine.minePos)) {
                                    if (autoMine.enabled) {
                                        if (this.redstonePos.equals(autoMine.minePos)) {
                                            return;
                                        }

                                        autoMine.onStart(this.redstonePos);
                                    } else {
                                        Direction mineDir = SettingUtils.getPlaceOnDirection(this.redstonePos);
                                        if (mineDir != null) {
                                            this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.redstonePos, mineDir));
                                            this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.redstonePos, mineDir));
                                        }
                                    }

                                    if (!this.mined) {
                                        this.mineTime = System.currentTimeMillis();
                                    }

                                    this.mined = true;
                                    this.minedThisTick = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void update() {
        this.pistonPos = null;

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (!Managers.FRIENDS.isFriend(player)
                    && player != BlackOut.mc.player
                    && !(BlackOut.mc.player.distanceTo(player) > 10.0F)
                    && !(player.getHealth() <= 0.0F)
                    && !player.isSpectator()
                    && (OLEPOSSUtils.solid2(player.getBlockPos()) || !this.onlyHole.get() || HoleUtils.inHole(player.getBlockPos()))) {
                this.updatePos(player);
                if (this.pistonPos != null) {
                    return;
                }
            }
        }
    }

    private void updatePos(PlayerEntity player) {
        BlockPos eyePos = BlockPos.ofFloored(player.getEyePos());
        if (!OLEPOSSUtils.solid2(eyePos.up())) {
            for (Direction dir : Direction.Type.HORIZONTAL
                    .stream()
                    .sorted(Comparator.comparingDouble(d -> eyePos.offset(d).toCenterPos().distanceTo(BlackOut.mc.player.getEyePos())))
                    .toList()) {
                this.resetPos();
                BlockPos pos = eyePos.offset(dir);
                if (this.upCheck(pos)
                        && (
                        OLEPOSSUtils.replaceable(pos)
                                || BlackOut.mc.world.getBlockState(pos).getBlock() instanceof PistonBlock
                                || BlackOut.mc.world.getBlockState(pos).getBlock() == Blocks.MOVING_PISTON
                )
                        && !OLEPOSSUtils.solid2(eyePos.offset(dir.getOpposite()))
                        && !OLEPOSSUtils.solid2(eyePos.offset(dir.getOpposite()).up())
                        && OLEPOSSUtils.solid2(eyePos.offset(dir.getOpposite()).down())) {
                    PlaceData data = SettingUtils.getPlaceData(pos);
                    if (data != null && data.valid()) {
                        this.pistonData = data;
                        this.pistonDir = dir;
                        this.updateRedstone(pos);
                        if (this.redstonePos != null) {
                            if (this.startPos == null) {
                                this.startPos = player.getBlockPos();
                            }

                            this.currentPos = player.getBlockPos();
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
                    .sorted(Comparator.comparingDouble(i -> pos.offset(i).toCenterPos().distanceTo(BlackOut.mc.player.getEyePos())))
                    .toList()) {
                if (direction != this.pistonDir.getOpposite() && direction != Direction.DOWN && direction != Direction.UP) {
                    BlockPos position = pos.offset(direction);
                    if (OLEPOSSUtils.replaceable(position) || BlackOut.mc.world.getBlockState(position).getBlock() instanceof RedstoneTorchBlock) {
                        this.redstoneData = SettingUtils.getPlaceData(
                                position,
                                null,
                                (p, d) -> {
                                    if (d == Direction.UP && !OLEPOSSUtils.solid(position.down())) {
                                        return false;
                                    } else if (direction == d.getOpposite()) {
                                        return false;
                                    } else if (pos.equals(p)) {
                                        return false;
                                    } else {
                                        return !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof TorchBlock) && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonBlock)
                                                && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonHeadBlock);
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
                    .sorted(Comparator.comparingDouble(i -> pos.offset(i).toCenterPos().distanceTo(BlackOut.mc.player.getEyePos())))
                    .toList()) {
                if (directionx != this.pistonDir.getOpposite() && directionx != Direction.DOWN) {
                    BlockPos position = pos.offset(directionx);
                    if ((OLEPOSSUtils.replaceable(position) || BlackOut.mc.world.getBlockState(position).getBlock() == Blocks.REDSTONE_BLOCK)
                            && !EntityUtils.intersects(BoxUtils.get(position), entity -> !entity.isSpectator() && entity instanceof PlayerEntity)) {
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
        double dx = BlackOut.mc.player.getEyePos().x - pos.getX() - 0.5;
        double dz = BlackOut.mc.player.getEyePos().z - pos.getZ() - 0.5;
        return Math.sqrt(dx * dx + dz * dz) > Math.abs(BlackOut.mc.player.getEyePos().y - pos.getY() - 0.5);
    }

    private void resetPos() {
        this.pistonPos = null;
        this.redstonePos = null;
        this.pistonDir = null;
        this.pistonData = null;
        this.redstoneData = null;
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
