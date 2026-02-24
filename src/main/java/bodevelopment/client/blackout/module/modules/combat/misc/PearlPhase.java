package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PearlPhase extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgRender = this.addGroup("Render");

    public final Setting<SwitchMode> ccSwitchMode = this.sgGeneral.enumSetting("CC Switch Mode", SwitchMode.Normal, "Switch method for CC blocks.");
    public final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Normal, "Switch method for pearl.");
    public final Setting<Integer> pitch = this.sgGeneral.intSetting("Pitch", 85, -90, 90, 1, "How deep down to look.");
    private final Setting<Boolean> ccBypass = this.sgGeneral.booleanSetting("CC Bypass", false, "Bypass CC anti-delay by placing a block first.");
    private final Setting<ObsidianModule.RotationMode> rotationMode = this.sgGeneral.enumSetting("Rotation Mode", ObsidianModule.RotationMode.Normal, "Rotation method.");
    private final Setting<Boolean> swing = this.sgRender.booleanSetting("Swing", false, "Swing animation.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand, "Hand to swing.");

    private boolean placed = false;

    public PearlPhase() {
        super("Pearl Phase", "Throws a pearl to phase through blocks", SubCategory.MISC_COMBAT, true);
    }

    @Override
    public void onEnable() {
        this.placed = false;
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        Hand hand = OLEPOSSUtils.getHand(Items.ENDER_PEARL);
        FindResult pearlResult = this.switchMode.get().find(Items.ENDER_PEARL);

        if (hand == null && !pearlResult.wasFound()) return;

        if (this.ccBypass.get() && !this.placed) {
            if (!this.cc()) return;
        }

        if (!this.handleRotations(this.getYaw(), this.pitch.get(), "look")) return;

        boolean isInvSwitch = hand == null;
        if (!isInvSwitch || this.switchMode.get().swap(pearlResult.slot())) {
            if (this.rotationMode.get() == ObsidianModule.RotationMode.Packet) {
                this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.getYaw(), this.pitch.get(), Managers.PACKET.isOnGround()));
            }
            this.useItem(hand == null ? Hand.MAIN_HAND : hand);
            if (this.swing.get()) {
                this.clientSwing(this.swingHand.get(), hand == null ? Hand.MAIN_HAND : hand);
            }
            this.end("look");
            if (isInvSwitch) this.switchMode.get().swapBack();
            this.disable("success");
        }
    }

    private boolean cc() {
        FindResult blockResult = this.ccSwitchMode.get().find(stack -> stack.getItem() instanceof BlockItem);

        if (!blockResult.wasFound()) {
            this.disable("no CC blocks found");
            return false;
        }

        BlockPos pos = BlackOut.mc.player.getBlockPos().down();

        if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
            if (!this.handleRotations(
                    (float) RotationUtils.getYaw(pos.toCenterPos()),
                    (float) RotationUtils.getPitch(BlackOut.mc.player.getEyePos(), pos.toCenterPos()),
                    "placing")) return false;
        }

        Hand blockHand = OLEPOSSUtils.getHand(stack -> stack.getItem() instanceof BlockItem);
        boolean isInvSwitch = blockHand == null;

        if (!isInvSwitch || this.ccSwitchMode.get().swap(blockResult.slot())) {
            this.placeBlock(blockHand == null ? Hand.MAIN_HAND : blockHand, pos.toCenterPos(), Direction.UP, pos);

            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) this.end("placing");

            if (isInvSwitch) this.ccSwitchMode.get().swapBack();
            this.placed = true;
            return true;
        }

        return false;
    }

    private boolean handleRotations(float yaw, float pitch, String name) {
        RotationType type = name.equals("placing") ? RotationType.BlockPlace : RotationType.Other;
        RotationType instantType = name.equals("placing") ? RotationType.InstantBlockPlace : RotationType.InstantOther;

        return switch (this.rotationMode.get()) {
            case Normal -> this.rotate(yaw, pitch, type, name);
            case Instant -> this.rotate(yaw, pitch, instantType, name);
            case Packet -> true;
        };
    }

    private int getYaw() {
        return (int) Math.round(RotationUtils.getYaw(new Vec3d(
                Math.floor(BlackOut.mc.player.getX()) + 0.5,
                0.0,
                Math.floor(BlackOut.mc.player.getZ()) + 0.5))) + 180;
    }
}