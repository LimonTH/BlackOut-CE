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
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

public class BurrowRewrite extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRubberband = this.addGroup("Rubberband");

    private final Setting<BurrowMode> mode = this.sgGeneral.enumSetting("Mode", BurrowMode.Offset, ".");
    private final Setting<Boolean> checkCollisions = this.sgGeneral.booleanSetting("Check Entities", true, ".");
    private final Setting<Boolean> attack = this.sgGeneral.booleanSetting("Attack", true, ".");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "Method of switching.");
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Blocks", "Blocks to use.", Blocks.OBSIDIAN, Blocks.ENDER_CHEST);
    private final Predicate<ItemStack> predicate = stack -> stack.getItem() instanceof BlockItem blockItem
            && this.blocks.get().contains(blockItem.getBlock());
    private final Setting<Boolean> instant = this.sgGeneral.booleanSetting("Instant", true, ".");
    private final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Use Timer", false, ".", () -> !this.instant.get());
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer", 1.0, 1.0, 5.0, 0.05, ".", () -> !this.instant.get() && this.useTimer.get());
    private final Setting<Boolean> smartRotate = this.sgGeneral.booleanSetting("Smart Rotate", true, ".");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true, ".");
    private final Setting<Integer> jumpTicks = this.sgGeneral.intSetting("Jump Ticks", 3, 3, 10, 1, ".");
    private final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Cooldown", 1.0, 0.0, 5.0, 0.05, ".");

    private final Setting<Double> offset = this.sgRubberband.doubleSetting("Offset", 1.0, -10.0, 10.0, 0.2, ".", () -> this.mode.get() == BurrowMode.Offset);
    private final Setting<Integer> packets = this.sgRubberband.intSetting("Packets", 1, 1, 20, 1, ".", () -> this.mode.get() == BurrowMode.Offset);
    private final Setting<Boolean> smooth = this.sgRubberband.booleanSetting("Smooth", false, "Enabled scaffold after burrowing.");
    private final Setting<Boolean> syncPacket = this.sgRubberband.booleanSetting("Sync Packet", false, ".", this.smooth::get);

    private boolean shouldCancel = true;
    private int tick = 0;
    private Vec3d startPos = Vec3d.ZERO;
    private long prevFinish = 0L;
    private long lastNotify = 0L;
    private long lastAttack = 0L;
    private boolean modifiedTimer = false;

    public BurrowRewrite() {
        super("Burrow Rewrite", ".", SubCategory.DEFENSIVE, true);
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
                    this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(BlackOut.mc.player.getX(), 1337.0, BlackOut.mc.player.getZ(), false));
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
                                        BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + y, BlackOut.mc.player.getZ(), false
                                )
                        );
                    }

                    this.place(data);
                } else {
                    this.tick = 0;
                    this.startPos = BlackOut.mc.player.getPos();
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
        BlockPos pos = BlockPos.ofFloored(prevPos);
        if (!this.canAttempt(pos)) {
            return null;
        }

        PlaceData data = SettingUtils.getPlaceData(pos);
        if (!data.valid()) {
            this.notifyFailure("invalid position");
            return null;
        }

        return this.rotateBlockIfNeeded(data, "block") ? data : null;
    }

    private void tickJumping() {
        boolean lastTick = ++this.tick == this.jumpTicks.get();
        if (lastTick) {
            this.tick = -1;
        }

        Vec3d prevPos = BlackOut.mc.player.getPos();
        BlackOut.mc.player.setPosition(this.startPos.add(0.0, this.calcY(), 0.0));
        BlockPos pos = BlockPos.ofFloored(this.startPos);
        if (!this.canAttempt(pos)) {
            BlackOut.mc.player.setPosition(prevPos);
            this.tick = -1;
            this.resetTimer();
            return;
        }

        PlaceData data = this.getPlaceData();
        if (data.valid()) {
            boolean rotated = this.rotateBlockIfNeeded(data, "placing");
            if (lastTick) {
                if (rotated) {
                    this.place(data);
                }

                this.tick = -1;
            }

            BlackOut.mc.player.setPosition(prevPos);
        }
    }

    private void place(PlaceData data) {
        if (!this.canAttempt(data.pos())) {
            return;
        }

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
    }

    private void rubberband() {
        double x = BlackOut.mc.player.getX();
        double y = BlackOut.mc.player.getY() + this.offset.get();
        double z = BlackOut.mc.player.getZ();

        for (int i = 0; i < this.packets.get(); i++) {
            this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
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
                                        false
                                )
                        );
            }
        }
    }

    private PlaceData getPlaceData() {
        return SettingUtils.getPlaceData(BlockPos.ofFloored(this.startPos));
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
        if (!shouldRotate) {
            return true;
        }

        if (!this.rotateBlock(data, type, label)) {
            this.notifyFailure("rotation failed");
            return false;
        }

        return true;
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
        this.sendMessage("Burrow: " + reason);
    }

    public enum BurrowMode {
        Offset,
        Cancel
    }
}
