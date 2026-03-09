package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.EntityUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

@OnlyDev
public class BurrowRewrite extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRubberband = this.addGroup("Rubberband");

    private final Setting<BurrowMode> mode = this.sgGeneral.enumSetting("Execution Mode", BurrowMode.Offset, "The method used to force the server to desync the player's position after block placement.");
    private final Setting<Boolean> checkCollisions = this.sgGeneral.booleanSetting("Collision Check", true, "Prevents activation if entities are obstructing the target coordinates.");
    private final Setting<Boolean> attack = this.sgGeneral.booleanSetting("Auto Attack", true, "Attempts to clear obstructing entities like End Crystals before initiating the burrow.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Swap Logic", SwitchMode.Silent, "The inventory management method used to select the required block.");
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Block Registry", "A prioritized list of blocks to use for the burrowing process.", Blocks.OBSIDIAN, Blocks.ENDER_CHEST);
    private final Setting<Boolean> instant = this.sgGeneral.booleanSetting("Zero-Tick", true, "Executes the entire jump, placement, and offset sequence within a single game tick.");
    private final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Timer Modulation", false, "Modifies the game clock speed to accelerate the jump sequence.", () -> !this.instant.get());
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Speed", 1.0, 1.0, 5.0, 0.05, "The multiplier applied to the game timer during execution.", () -> !this.instant.get() && this.useTimer.get());
    private final Setting<Boolean> smartRotate = this.sgGeneral.booleanSetting("Smart Orientation", true, "Dynamically calculates rotations to ensure block placement validity.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotation", true, "Forces the rotation to complete immediately without interpolation.");
    private final Setting<Integer> jumpTicks = this.sgGeneral.intSetting("Airborne Duration", 4, 3, 10, 1, "The number of ticks simulated or spent in the air to reach sufficient height.");
    private final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Activation Cooldown", 1.0, 0.0, 5.0, 0.05, "Minimum delay between consecutive burrow attempts.");
    private final Setting<Boolean> autoDisable = this.sgGeneral.booleanSetting("Auto Disable", true, "Automatically disables the module after successful burrow placement.");

    private final Setting<Double> offset = this.sgRubberband.doubleSetting("Teleport Offset", 1.0, -10.0, 10.0, 0.2, "The vertical distance used to trigger a server-side rubberband effect.", () -> this.mode.get() == BurrowMode.Offset);
    private final Setting<Integer> packets = this.sgRubberband.intSetting("Packet Burst", 1, 1, 20, 1, "The number of redundant position packets sent to ensure desynchronization.", () -> this.mode.get() == BurrowMode.Offset);
    private final Setting<Boolean> smooth = this.sgRubberband.booleanSetting("Kinetic Smoothing", false, "Maintains movement momentum post-burrow to avoid immediate velocity resets.");
    private final Setting<Boolean> syncPacket = this.sgRubberband.booleanSetting("State Synchronization", false, "Sends an additional full movement packet to align client and server states.", this.smooth::get);

    private final Direction[] burrowDirections = new Direction[]{
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private final Predicate<ItemStack> predicate = stack -> stack.getItem() instanceof BlockItem blockItem
            && this.blocks.get().contains(blockItem.getBlock());

    private boolean shouldCancel = true;
    private int tick = 0;
    private Vec3d startPos = Vec3d.ZERO;
    private double maxHeight = 0.0;
    private long prevFinish = 0L;
    private long lastNotify = 0L;
    private long lastAttack = 0L;
    private boolean modifiedTimer = false;

    public BurrowRewrite() {
        super("Burrow Rewrite", "A modernized defensive module that forces the player inside a block to prevent knockback and projectile damage.", SubCategory.DEFENSIVE, true);
    }

    @Override
    public void onDisable() {
        this.resetTimer();
        this.shouldCancel = false;
        this.tick = -1;
    }

    @Event
    public void onReceive(PacketEvent.Receive.Post event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && this.shouldCancel) {
            this.shouldCancel = false;
            event.setCancelled(true);
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean ready = now - this.prevFinish >= (long) (this.cooldown.get() * 1000.0);
        boolean outside = !OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().stretch(0.0, this.calcY(), 0.0));
        if (outside && ready && !this.notFound()) {
            BlockPos pos = BlockPos.ofFloored(BlackOut.mc.player.getPos());
            if (!this.canAttempt(pos)) {
                this.resetTimer();
                return;
            }

            if (!this.instant.get() && this.useTimer.get()) {
                this.modifiedTimer = true;
                Timer.set(this.timer.get().floatValue());
            }

            if (BlackOut.mc.player.isOnGround()) {
                if (this.mode.get() == BurrowMode.Cancel) {
                    this.shouldCancel = true;
                    this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(BlackOut.mc.player.getX(), 1337.0, BlackOut.mc.player.getZ(), false, BlackOut.mc.player.horizontalCollision));
                }

                if (this.instant.get()) {
                    Vec3d prevPos = BlackOut.mc.player.getPos();
                    BlackOut.mc.player.setPosition(BlackOut.mc.player.getPos().add(0.0, this.calcY(), 0.0));
                    PlaceData data = this.preInstant(prevPos);
                    BlackOut.mc.player.setPosition(prevPos);
                    if (data == null) {
                        this.resetTimer();
                        return;
                    }

                    double y = 0.0;
                    float yVel = 0.42F;

                    for (int i = 0; i < this.jumpTicks.get(); i++) {
                        y += yVel;
                        yVel = (yVel - 0.08F) * 0.98F;
                        this.sendPacket(
                                new PlayerMoveC2SPacket.PositionAndOnGround(
                                        BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + y, BlackOut.mc.player.getZ(), false, BlackOut.mc.player.horizontalCollision));
                    }

                    this.place(data);
                } else {
                    this.tick = 0;
                    this.startPos = BlackOut.mc.player.getPos();
                    this.maxHeight = this.startPos.getY();
                    event.setY(this, 0.42F);
                    BlackOut.mc.player.jump();
                }
            }

            if (this.tick >= 0 && !this.instant.get()) {
                event.setXZ(this, 0.0, 0.0);
                this.tickJumping();
            }
        } else {
            this.resetTimer();
        }
    }

    private boolean notFound() {
        if (OLEPOSSUtils.getHand(this.predicate) == null && !this.switchMode.get().find(this.predicate).wasFound()) {
            this.resetTimer();
            this.disable("no blocks found");
            return true;
        }

        return false;
    }

    private PlaceData preInstant(Vec3d prevPos) {
        BlockPos bestPos = this.findBestBurrowPos(prevPos);
        if (bestPos == null) {
            this.notifyFailure("no valid burrow position");
            return null;
        }

        if (!this.canAttempt(bestPos)) {
            return null;
        }

        PlaceData data = SettingUtils.getPlaceData(bestPos);
        if (!data.valid()) {
            this.notifyFailure("invalid position");
            return null;
        }

        return this.rotateBlockIfNeeded(data, "block") ? data : null;
    }

    private void tickJumping() {
        this.tick++;

        double currentY = BlackOut.mc.player.getY();
        double velocityY = BlackOut.mc.player.getVelocity().y;

        if (currentY > this.maxHeight) {
            this.maxHeight = currentY;
        }

        boolean startedFalling = currentY < this.maxHeight - 0.05;
        boolean closeToGround = currentY <= this.startPos.getY() + 0.2;
        if (startedFalling && closeToGround) {
            PlaceData data = this.getPlaceData();
            if (data.valid()) {
                boolean rotated = this.rotateBlockIfNeeded(data, "placing");
                if (rotated) {
                    this.place(data);
                    this.tick = -1;
                    return;
                }
            } else {
                PlaceData altData = this.getPlaceDataForCurrentPos();
                if (altData.valid()) {
                    boolean rotated = this.rotateBlockIfNeeded(altData, "placing");
                    if (rotated) {
                        this.place(altData);
                        this.tick = -1;
                        return;
                    }
                }
            }
            this.tick = -1;
        }

        if (this.tick > this.jumpTicks.get() * 2) {
            this.tick = -1;
        }
    }

    private void place(PlaceData data) {
        Hand hand = OLEPOSSUtils.getHand(this.predicate);
        if (hand == null) {
            FindResult result = this.switchMode.get().find(this.predicate);
            if (!result.wasFound() || !this.switchMode.get().swap(result.slot())) {
                this.notifyFailure("no blocks found");
                return;
            }
        }

        this.prevFinish = System.currentTimeMillis();
        this.placeBlock(hand, data);

        if (this.mode.get() == BurrowMode.Offset) {
            this.rubberband();
        }

        if (hand == null) {
            this.switchMode.get().swapBack();
        }

        if (this.autoDisable.get()) {
            this.silentDisable();
        }
    }

    private void rubberband() {
        double x = BlackOut.mc.player.getX();
        double y = BlackOut.mc.player.getY() + this.offset.get();
        double z = BlackOut.mc.player.getZ();

        for (int i = 0; i < this.packets.get(); i++) {
            this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, BlackOut.mc.player.horizontalCollision));
        }

        if (this.smooth.get()) {
            this.sendPacket(Managers.PACKET.incrementedPacket(BlackOut.mc.player.getPos()));
            if (this.syncPacket.get()) {
                Managers.PACKET
                        .sendInstantly(
                                new PlayerMoveC2SPacket.Full(
                                        this.startPos.getX(),
                                        this.startPos.getY(),
                                        this.startPos.getZ(),
                                        Managers.ROTATION.prevYaw,
                                        Managers.ROTATION.prevPitch,
                                        false,
                                        BlackOut.mc.player.horizontalCollision
                                )
                        );
            }
        }
    }

    private PlaceData getPlaceData() {
        return SettingUtils.getPlaceData(BlockPos.ofFloored(this.startPos));
    }

    private PlaceData getPlaceDataForCurrentPos() {
        return SettingUtils.getPlaceData(BlockPos.ofFloored(BlackOut.mc.player.getPos()));
    }

    private BlockPos findBestBurrowPos(Vec3d playerPos) {
        BlockPos basePos = BlockPos.ofFloored(playerPos);

        if (OLEPOSSUtils.replaceable(basePos) && this.canAttempt(basePos)) {
            return basePos;
        }

        for (Direction dir : this.burrowDirections) {
            if (dir == Direction.DOWN) continue;

            BlockPos sidePos = basePos.offset(dir);
            if (OLEPOSSUtils.replaceable(sidePos) && this.canAttempt(sidePos)) {
                Box playerBox = BlackOut.mc.player.getBoundingBox();
                Box blockBox = BoxUtils.get(sidePos);
                if (playerBox.intersects(blockBox)) {
                    return sidePos;
                }
            }
        }

        BlockPos upPos = basePos.up();
        if (OLEPOSSUtils.replaceable(upPos) && this.canAttempt(upPos)) {
            return upPos;
        }

        return null;
    }

    private float calcY() {
        float velocity = 0.42F;
        float y = 0.0F;

        for (int i = 0; i < this.jumpTicks.get(); i++) {
            y += velocity;
            velocity = (velocity - 0.08F) * 0.98F;
        }

        return y;
    }

    private boolean rotateBlockIfNeeded(PlaceData data, String label) {
        RotationType type = RotationType.BlockPlace.withInstant(this.instantRotate.get());
        boolean shouldRotate = this.smartRotate.get() || SettingUtils.shouldRotate(RotationType.BlockPlace);

        if (!shouldRotate) return true;

        return this.rotateBlock(data, type, label);
    }

    private boolean canAttempt(BlockPos pos) {
        if (!OLEPOSSUtils.replaceable(pos)) {
            this.notifyFailure("position blocked");
            return false;
        }

        if (this.checkCollisions.get()) {
            if (this.attack.get()) {
                this.attackBlockingEntities(pos);
            }

            if (this.hasBlockingEntities(pos)) {
                this.notifyFailure("entity collision");
                return false;
            }
        }

        return true;
    }

    private boolean hasBlockingEntities(BlockPos pos) {
        Box box = BoxUtils.get(pos);
        return EntityUtils.intersects(box, entity ->
                !entity.isSpectator()
                        && entity != BlackOut.mc.player
                        && !(entity instanceof ItemEntity)
                        && !(this.attack.get() && entity instanceof EndCrystalEntity));
    }

    private void attackBlockingEntities(BlockPos pos) {
        if (System.currentTimeMillis() - this.lastAttack < 100L) {
            return;
        }

        Box crystalBox = OLEPOSSUtils.getCrystalBox(pos);
        List<Entity> crystals = EntityUtils.getEntities(crystalBox, entity -> entity instanceof EndCrystalEntity);
        if (crystals.isEmpty()) {
            return;
        }

        for (Entity entity : crystals) {
            this.attackEntity(entity);
        }

        this.lastAttack = System.currentTimeMillis();
    }

    private void resetTimer() {
        if (this.modifiedTimer) {
            this.modifiedTimer = false;
            Timer.reset();
        }
    }

    private void notifyFailure(String reason) {
        long now = System.currentTimeMillis();
        if (now - this.lastNotify < 750L) {
            return;
        }

        this.lastNotify = now;
        this.sendNotification("Burrow: " + reason, reason, "Burrow Alert", Notifications.Type.Alert, 2.0);
    }



    public enum BurrowMode {
        Offset,
        Cancel
    }
}