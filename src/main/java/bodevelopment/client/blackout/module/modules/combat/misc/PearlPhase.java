package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.util.PlayerUtils;
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
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class PearlPhase extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgRender = this.addGroup("Render");

    public final Setting<SwitchMode> ccSwitchMode = this.sgGeneral.enumSetting("CC Switch Mode", SwitchMode.Normal, "Method used to switch to bypass blocks.");
    public final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Normal, "Method used to switch to ender pearls.");
    public final Setting<Integer> pitch = this.sgGeneral.intSetting("Pitch", 85, -90, 90, 1, "The downward angle for the throw. 85 is recommended for most clips.");
    private final Setting<Boolean> ccBypass = this.sgGeneral.booleanSetting("CC Bypass", false, "Attempts to bypass pearl delay by placing a block beneath you first.");
    private final Setting<ObsidianModule.RotationMode> rotationMode = this.sgGeneral.enumSetting("Rotation Mode", ObsidianModule.RotationMode.Normal, "How rotations should be handled during the phase.");
    private final Setting<Boolean> swing = this.sgRender.booleanSetting("Swing", false, "Enables client-side swing animation upon throwing.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand, "Determines which hand performs the swing animation.", this.swing::get);

    private boolean placed = false;

    public PearlPhase() {
        super("Pearl Phase", "Automates precision pearl throws to clip through block collisions and phase into holes.", SubCategory.MISC_COMBAT, true);
    }

    @Override
    public void onEnable() {
        this.placed = false;
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (!PlayerUtils.isInGame()) return;

        InteractionHand hand = InvUtils.getHand(Items.ENDER_PEARL);
        FindResult pearlResult = this.switchMode.get().find(Items.ENDER_PEARL);

        if (hand == null && !pearlResult.wasFound()) return;

        if (this.ccBypass.get() && !this.placed) {
            if (!this.cc()) return;
        }

        if (!this.handleRotations(this.getYaw(), this.pitch.get(), "look")) return;

        boolean isInvSwitch = hand == null;
        if (!isInvSwitch || this.switchMode.get().swap(pearlResult.slot())) {
            if (this.rotationMode.get() == ObsidianModule.RotationMode.Packet) {
                this.sendPacket(new ServerboundMovePlayerPacket.Rot(this.getYaw(), this.pitch.get(), Managers.PACKET.isOnGround(), BlackOut.mc.player.isShiftKeyDown()));
            }
            this.useItem(hand == null ? InteractionHand.MAIN_HAND : hand);
            if (this.swing.get()) {
                this.clientSwing(this.swingHand.get(), hand == null ? InteractionHand.MAIN_HAND : hand);
            }
            this.rotation.end("look");
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

        BlockPos pos = BlackOut.mc.player.blockPosition().below();

        if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
            if (!this.handleRotations(
                    (float) RotationUtils.getYaw(pos.getCenter()),
                    (float) RotationUtils.getPitch(BlackOut.mc.player.getEyePosition(), pos.getCenter()),
                    "placing")) return false;
        }

        InteractionHand blockHand = InvUtils.getHand(stack -> stack.getItem() instanceof BlockItem);
        boolean isInvSwitch = blockHand == null;

        if (!isInvSwitch || this.ccSwitchMode.get().swap(blockResult.slot())) {
            this.placeBlock(blockHand == null ? InteractionHand.MAIN_HAND : blockHand, pos.getCenter(), Direction.UP, pos);

            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) this.rotation.end("placing");

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
            case Normal -> this.rotation.rotate(yaw, pitch, type, name);
            case Instant -> this.rotation.rotate(yaw, pitch, instantType, name);
            case Packet -> true;
        };
    }

    private int getYaw() {
        return (int) Math.round(RotationUtils.getYaw(new Vec3(
                Math.floor(BlackOut.mc.player.getX()) + 0.5,
                0.0,
                Math.floor(BlackOut.mc.player.getZ()) + 0.5))) + 180;
    }
}