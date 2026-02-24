package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.movement.PacketFly;
import bodevelopment.client.blackout.module.modules.movement.Scaffold;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Predicate;

public class Burrow extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgLagBack = this.addGroup("Lag Back");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "Method of switching.");
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Blocks", "Blocks to use.", Blocks.OBSIDIAN, Blocks.ENDER_CHEST);
    private final Predicate<ItemStack> predicate = itemStack -> itemStack.getItem() instanceof BlockItem block && this.blocks.get().contains(block.getBlock());
    private final Setting<Boolean> instaRot = this.sgGeneral.booleanSetting("Insta Rotation", false, "Instantly rotates.");
    private final Setting<Boolean> pFly = this.sgGeneral.booleanSetting("Packet Fly", false, "Enabled packetfly after burrowing.");
    private final Setting<Boolean> enableScaffold = this.sgGeneral.booleanSetting("Scaffold", false, "Enabled scaffold after burrowing.", this.pFly::get);
    private final Setting<Double> lagBackOffset = this.sgLagBack.doubleSetting("Offset", -0.1, -10.0, 10.0, 0.2, "Y offset for rubberband packet.");
    private final Setting<Integer> lagBackPackets = this.sgLagBack.intSetting("Packets", 1, 1, 20, 1, "How many offset packets to send.");
    private final Setting<Boolean> smooth = this.sgLagBack.booleanSetting("Smooth", false, "Enabled scaffold after burrowing.");
    private final Setting<Boolean> syncPacket = this.sgLagBack.booleanSetting("Sync Packet", false, ".", this.smooth::get);
    private final Setting<Boolean> renderSwing = this.sgRender.booleanSetting("Render Swing", true, "Renders swing animation when placing a block.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand, "Which hand should be swung.", this.renderSwing::get);
    private boolean success = false;
    private boolean enabledPFly = false;
    private boolean enabledScaffold = false;

    public Burrow() {
        super("Burrow", "Places a block inside your feet.", SubCategory.DEFENSIVE, true);
    }

    @Override
    public void onEnable() {
        this.success = false;
        this.enabledPFly = false;
        this.enabledScaffold = false;
    }

    @Override
    public void onDisable() {
        PacketFly pFly = PacketFly.getInstance();
        if (this.enabledPFly) {
            pFly.disable(pFly.getDisplayName() + " disabled by Burrow");
        }

        if (this.enabledScaffold) {
            pFly.disable(pFly.getDisplayName() + " disabled by Burrow");
        }

        this.enabledPFly = false;
        this.enabledScaffold = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && !this.success) {
            Hand hand = OLEPOSSUtils.getHand(this.predicate);
            boolean blocksPresent = hand != null;
            FindResult result = this.switchMode.get().find(this.predicate);
            if (!blocksPresent) {
                blocksPresent = result.wasFound();
            }

            if (blocksPresent) {
                boolean rotated = this.instaRot.get()
                        || !SettingUtils.shouldRotate(RotationType.BlockPlace)
                        || this.rotatePitch(90.0F, RotationType.BlockPlace, "placing");
                if (rotated) {
                    boolean switched = hand != null;
                    if (!switched) {
                        switched = this.switchMode.get().swap(result.slot());
                    }

                    if (!switched) {
                        this.disable(this.getDisplayName() + " correct blocks not found", 2, Notifications.Type.Alert);
                    } else {
                        if (this.instaRot.get() && SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                            this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(Managers.ROTATION.nextYaw, 90.0F, Managers.PACKET.isOnGround()));
                        }

                        double y = 0.0;
                        double velocity = 0.42;

                        while (y < 1.1) {
                            y += velocity;
                            velocity = (velocity - 0.08) * 0.98;
                            this.sendPacket(
                                    new PlayerMoveC2SPacket.PositionAndOnGround(
                                            BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + y, BlackOut.mc.player.getZ(), false
                                    )
                            );
                        }

                        this.placeBlock(
                                hand,
                                BlackOut.mc.player.getBlockPos().down().toCenterPos(),
                                Direction.UP,
                                BlackOut.mc.player.getBlockPos().down()
                        );
                        if (this.renderSwing.get()) {
                            this.clientSwing(this.swingHand.get(), hand);
                        }

                        if (!this.instaRot.get() && SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                            this.end("placing");
                        }

                        this.lagBack(y);
                        this.success = true;
                        if (hand == null) {
                            this.switchMode.get().swapBack();
                        }

                        if (!this.pFly.get()) {
                            this.disable(this.getDisplayName() + " was successful");
                        }
                    }
                }
            }
        }
    }

    @Event
    public void onPacket(PacketEvent.Receive.Pre event) {
        if (this.pFly.get() && this.success && event.packet instanceof PlayerPositionLookS2CPacket) {
            PacketFly packetFly = PacketFly.getInstance();
            if (!packetFly.enabled) {
                this.enabledPFly = true;
                packetFly.enable("enabled by Burrow");
                Scaffold scaffold = Scaffold.getInstance();
                if (this.enableScaffold.get() && !scaffold.enabled) {
                    scaffold.enable("enabled by burrow");
                    this.enabledScaffold = true;
                }
            }
        }
    }

    private void lagBack(double y) {
        for (int i = 0; i < this.lagBackPackets.get(); i++) {
            this.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(
                            BlackOut.mc.player.getX(),
                            BlackOut.mc.player.getY() + y + this.lagBackOffset.get(),
                            BlackOut.mc.player.getZ(),
                            false
                    )
            );
        }

        if (this.smooth.get()) {
            this.sendPacket(Managers.PACKET.incrementedPacket(BlackOut.mc.player.getPos()));
            if (this.syncPacket.get()) {
                float yaw = MathHelper.wrapDegrees(Managers.ROTATION.prevYaw);
                if (yaw < 0.0F) {
                    yaw += 360.0F;
                }

                Managers.PACKET
                        .sendInstantly(
                                new PlayerMoveC2SPacket.Full(
                                        BlackOut.mc.player.getX(),
                                        BlackOut.mc.player.getY(),
                                        BlackOut.mc.player.getZ(),
                                        yaw,
                                        Managers.ROTATION.prevPitch,
                                        false
                                )
                        );
            }
        }
    }
}
