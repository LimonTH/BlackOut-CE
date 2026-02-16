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
    public final Setting<SwitchMode> ccSwitchMode = this.sgGeneral
            .e("CC Switch Mode", SwitchMode.Normal, "Which method of switching should be used for cc items.");
    public final Setting<SwitchMode> switchMode = this.sgGeneral.e("Switch Mode", SwitchMode.Normal, "Which method of switching should be used.");
    public final Setting<Integer> pitch = this.sgGeneral.i("Pitch", 85, -90, 90, 1, "How deep down to look.");
    private final Setting<Boolean> ccBypass = this.sgRender.b("CC Bypass", false, "Does funny stuff to bypass cc's anti delay.");
    private final Setting<ObsidianModule.RotationMode> rotationMode = this.sgGeneral.e("Rotation Mode", ObsidianModule.RotationMode.Normal, ".");
    private final Setting<Boolean> swing = this.sgRender.b("Swing", false, "Renders swing animation when placing throwing a peal");
    private final Setting<SwingHand> swingHand = this.sgRender.e("Swing Hand", SwingHand.RealHand, "Which hand should be swung.");
    private boolean placed = false;

    public PearlPhase() {
        super("Pearl Phase", "Throws a pearl", SubCategory.MISC_COMBAT, true);
    }

    @Override
    public void onEnable() {
        this.placed = false;
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            Hand hand = OLEPOSSUtils.getHand(Items.ENDER_PEARL);
            FindResult findResult = this.switchMode.get().find(Items.ENDER_PEARL);
            if (hand != null || findResult.wasFound()) {
                if (!this.ccBypass.get() || this.cc() || this.placed) {
                    switch (this.rotationMode.get()) {
                        case Normal:
                            if (!this.rotate(this.getYaw(), this.pitch.get().intValue(), RotationType.Other, "look")) {
                                return;
                            }
                            break;
                        case Instant:
                            if (!this.rotate(this.getYaw(), this.pitch.get().intValue(), RotationType.InstantOther, "look")) {
                                return;
                            }
                    }

                    boolean switched = false;
                    if (switched || this.switchMode.get().swap(findResult.slot())) {
                        if (this.rotationMode.get() == ObsidianModule.RotationMode.Packet) {
                            this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.getYaw(), this.pitch.get().intValue(), Managers.PACKET.isOnGround()));
                        }

                        this.useItem(hand);
                        if (this.swing.get()) {
                            this.clientSwing(this.swingHand.get(), hand);
                        }

                        this.end("look");
                        this.disable("success");
                        if (hand == null) {
                            this.switchMode.get().swapBack();
                        }
                    }
                }
            }
        }
    }

    private boolean cc() {
        FindResult result = null;
        if (!(result = this.ccSwitchMode.get().find(stack -> stack.getItem() instanceof BlockItem)).wasFound()) {
            this.disable("no CC blocks found");
            return false;
        } else {
            BlockPos pos = BlackOut.mc.player.getBlockPos();
            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                switch (this.rotationMode.get()) {
                    case Normal:
                        if (!this.rotateBlock(pos.down(), Direction.UP, RotationType.BlockPlace, "placing")) {
                            return false;
                        }
                        break;
                    case Instant:
                        if (!this.rotateBlock(pos.down(), Direction.UP, RotationType.InstantBlockPlace, "placing")) {
                            return false;
                        }
                }
            }

            Hand hand = OLEPOSSUtils.getHand(stack -> stack.getItem() instanceof BlockItem);
            if (hand == null && !this.ccSwitchMode.get().swap(result.slot())) {
                return false;
            } else {
                if (SettingUtils.shouldRotate(RotationType.BlockPlace) && this.rotationMode.get() == ObsidianModule.RotationMode.Packet) {
                    this.sendPacket(
                            new PlayerMoveC2SPacket.LookAndOnGround(
                                    (float) RotationUtils.getYaw(pos.toCenterPos()),
                                    (float) RotationUtils.getPitch(BlackOut.mc.player.getEyePos(), pos.toCenterPos()),
                                    Managers.PACKET.isOnGround()
                            )
                    );
                }

                this.placeBlock(hand == null ? Hand.MAIN_HAND : hand, pos.down().toCenterPos(), Direction.UP, pos.down());
                if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                    this.end("placing");
                }

                this.placed = true;
                if (hand == null) {
                    this.ccSwitchMode.get().swapBack();
                }

                return true;
            }
        }
    }

    private int getYaw() {
        return (int) Math.round(
                RotationUtils.getYaw(
                        new Vec3d(Math.floor(BlackOut.mc.player.getX()) + 0.5, 0.0, Math.floor(BlackOut.mc.player.getZ()) + 0.5)
                )
        )
                + 180;
    }
}
