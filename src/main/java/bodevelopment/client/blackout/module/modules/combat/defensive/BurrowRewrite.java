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
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

// TODO: Now not @OnlyDev
public class BurrowRewrite extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRubberband = this.addGroup("Rubberband");

    // TODO: checkCollisions, attack и smartRotate нигде не используются
    private final Setting<BurrowMode> mode = this.sgGeneral.enumSetting("Mode", BurrowMode.Offset,
            "How to handle the rubberband effect. 'Offset' teleports you, while 'Cancel' tries to disrupt server-side positioning.");
    private final Setting<Boolean> checkCollisions = this.sgGeneral.booleanSetting("Check Entities", true,
            "Prevents burrowing if there are entities (like other players) that would block the placement.");
    private final Setting<Boolean> attack = this.sgGeneral.booleanSetting("Attack", true,
            "Automatically clears crystals or other obstacles in your feet before placing the block.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent,
            "Inventory bypass method. 'Silent' is best for servers with strict switch checks.");
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Blocks",
            "Blocks to use for self-fill. Obsidian and Ender Chests are standard choices.", Blocks.OBSIDIAN, Blocks.ENDER_CHEST);
    private final Setting<Boolean> instant = this.sgGeneral.booleanSetting("Instant", true,
            "Performs the entire burrow sequence in a single tick. Desyncs less on most servers.");
    private final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Use Timer", false,
            "Slows down or speeds up game time during the jump to assist with placement timing.", () -> !this.instant.get());
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer", 1.0, 1.0, 5.0, 0.05,
            "The game speed multiplier to use during the burrow jump.", () -> !this.instant.get() && this.useTimer.get());
    private final Setting<Boolean> smartRotate = this.sgGeneral.booleanSetting("Smart Rotate", true,
            "Calculates the most efficient rotation to place the block without triggering anti-cheat flags.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true,
            "Sends rotation packets instantly rather than smoothly interpolating them.");
    private final Setting<Integer> jumpTicks = this.sgGeneral.intSetting("Jump Ticks", 3, 3, 10, 1,
            "How many ticks to wait or simulate air-time before placing the block.");

    private final Setting<Double> offset = this.sgRubberband.doubleSetting("Offset", 1.0, -10.0, 10.0, 0.2,
            "The vertical distance to teleport you after placement. Positive values move you up, negative move you down.",
            () -> this.mode.get() == BurrowMode.Offset);
    private final Setting<Integer> packets = this.sgRubberband.intSetting("Packets", 1, 1, 20, 1,
            "How many teleport packets to send. Higher values can help bypass strict rubberband checks.",
            () -> this.mode.get() == BurrowMode.Offset);
    private final Setting<Boolean> smooth = this.sgRubberband.booleanSetting("Smooth", false,
            "Attempts to smooth out the movement after burrowing to prevent 'lag-back' visual glitches.");
    private final Setting<Boolean> syncPacket = this.sgRubberband.booleanSetting("Sync Packet", false,
            "Sends an extra movement packet to synchronize your position with the server more accurately.", this.smooth::get);

    private final Predicate<ItemStack> predicate = stack -> stack.getItem() instanceof BlockItem blockItem
            && this.blocks.get().contains(blockItem.getBlock());
    private boolean shouldCancel = true;
    private int tick = 0;
    private Vec3d startPos = Vec3d.ZERO;
    private long prevFinish = 0L;
    private boolean modifiedTimer = false;

    public BurrowRewrite() {
        super("Burrow Rewrite", ".", SubCategory.DEFENSIVE, true);
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
        if (!OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().stretch(0.0, this.calcY(), 0.0))
                && System.currentTimeMillis() - this.prevFinish >= 1000L
                && !this.notFound()) {
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
            if (this.modifiedTimer) {
                this.modifiedTimer = false;
                Timer.reset();
            }
        }
    }

    private boolean notFound() {
        if (OLEPOSSUtils.getHand(this.predicate) == null && !this.switchMode.get().find(this.predicate).wasFound()) {
            this.disable("no blocks found");
            return true;
        } else {
            return false;
        }
    }

    private PlaceData preInstant(Vec3d prevPos) {
        PlaceData data = SettingUtils.getPlaceData(BlockPos.ofFloored(prevPos));
        if (!data.valid()) {
            return null;
        } else {
            return SettingUtils.shouldRotate(RotationType.BlockPlace)
                    && !this.rotateBlock(data, RotationType.BlockPlace.withInstant(this.instantRotate.get()), "block")
                    ? null
                    : data;
        }
    }

    private void tickJumping() {
        boolean lastTick = ++this.tick == this.jumpTicks.get();
        if (lastTick) {
            this.tick = -1;
        }

        Vec3d prevPos = BlackOut.mc.player.getPos();
        BlackOut.mc.player.setPosition(this.startPos.add(0.0, this.calcY(), 0.0));
        PlaceData data = this.getPlaceData();
        if (data.valid()) {
            boolean rotated = this.rotateBlock(data, RotationType.BlockPlace.withInstant(this.instantRotate.get()), "placing");
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
        Hand hand = OLEPOSSUtils.getHand(this.predicate);
        if (hand == null) {
            FindResult result = this.switchMode.get().find(this.predicate);
            if (!result.wasFound() || !this.switchMode.get().swap(result.slot())) {
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

    public enum BurrowMode {
        Offset,
        Cancel
    }
}
