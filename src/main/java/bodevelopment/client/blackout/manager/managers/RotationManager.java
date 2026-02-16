package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.settings.RotationSettings;
import bodevelopment.client.blackout.module.modules.movement.ElytraFly;
import bodevelopment.client.blackout.module.modules.movement.PacketFly;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.SharedFeatures;
import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RotationManager extends Manager {
    public final List<FloatFloatPair> rotationHistory = Collections.synchronizedList(new ArrayList<>());
    public final List<FloatFloatPair> tickRotationHistory = Collections.synchronizedList(new ArrayList<>());
    public long prevRotation = 0L;
    public float nextYaw = 0.0F;
    public float nextPitch = 0.0F;
    public long timeYaw = 0L;
    public long timePitch = 0L;
    public RotatePhase rotatingYaw = RotatePhase.Inactive;
    public RotatePhase rotatingPitch = RotatePhase.Inactive;
    public float prevYaw = 0.0F;
    public float prevPitch = 0.0F;
    public float renderYaw = 0.0F;
    public float renderPitch = 0.0F;
    public float prevRenderYaw = 0.0F;
    public float prevRenderPitch = 0.0F;
    public double priorityYaw = 0.0;
    public double priorityPitch = 0.0;
    public String keyYaw = "Luposulu best";
    public String keyPitch = "fr ^";
    public float moveLookYaw = 0.0F;
    public float moveYaw = 0.0F;
    public float moveOffset = 0.0F;
    public boolean move = false;
    public double packetsLeft = 0.0;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onTick(TickEvent.Post event) {
        this.updateRender();
        this.packetsLeft = Math.min(this.packetsLeft, 1.0);
        this.packetsLeft = this.packetsLeft + RotationSettings.getInstance().packetRotations.get();
        synchronized (this.tickRotationHistory) {
            this.tickRotationHistory.addFirst(new FloatFloatImmutablePair(this.prevYaw, this.prevPitch));
            OLEPOSSUtils.limitList(this.tickRotationHistory, 20);
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (this.rotatingYaw == RotatePhase.Rotating && SettingUtils.shouldVanillaRotate()) {
            BlackOut.mc.player.setYaw(MathHelper.lerp(event.tickDelta, this.prevRenderYaw, this.renderYaw));
        }

        if (this.rotatingPitch == RotatePhase.Rotating && SettingUtils.shouldVanillaRotate()) {
            BlackOut.mc.player.setPitch(MathHelper.lerp(event.tickDelta, this.prevRenderPitch, this.renderPitch));
        }
    }

    @Event
    public void onRotate(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesLook()) {
            this.setPrev(packet.getYaw(0.0F), packet.getPitch(0.0F));
            this.prevRotation = System.currentTimeMillis();
        }
    }

    @Event
    public void onSetback(PacketEvent.Received event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            this.setPrev(packet.getYaw(), packet.getPitch());
            this.updateRender();
        }
    }

    private void setPrev(float yaw, float pitch) {
        synchronized (this.rotationHistory) {
            this.rotationHistory.addFirst(new FloatFloatImmutablePair(Math.abs(yaw - this.prevYaw), Math.abs(pitch - this.prevPitch)));
            OLEPOSSUtils.limitList(this.rotationHistory, 20);
        }

        this.prevYaw = yaw;
        this.prevPitch = pitch;
    }

    private void updateRender() {
        if (RotationSettings.getInstance().renderSmoothness.get() <= 0) {
            this.renderYaw = this.prevYaw;
            this.renderPitch = this.prevPitch;
            this.prevRenderYaw = this.renderYaw;
            this.prevRenderPitch = this.renderPitch;
        } else {
            this.prevRenderYaw = this.renderYaw;
            this.prevRenderPitch = this.renderPitch;
            this.renderYaw = this.getRenderRotation(this.prevYaw, this.prevRenderYaw);
            this.renderPitch = this.getRenderRotation(this.prevPitch, this.prevRenderPitch);
        }
    }

    public float getRenderRotation(float current, float prev) {
        return prev + (current - prev) / RotationSettings.getInstance().renderSmoothness.get();
    }

    public int updateMove(float yaw, boolean move) {
        this.moveYaw = yaw;
        this.move = move;
        int offset = Math.round(MathHelper.wrapDegrees(yaw - this.moveLookYaw) / 45.0F);
        offset += this.moveOffset < -45.0F ? 1 : (this.moveOffset > 45.0F ? -1 : 0);
        this.moveOffset = this.moveOffset + (float) RotationUtils.yawAngle(yaw, this.moveLookYaw + offset * 45);
        int i = offset % 8;
        return i < 0 ? 8 + i : i;
    }

    public float getNextYaw() {
        return this.rotated() ? this.nextYaw : this.prevYaw;
    }

    public float getNextPitch() {
        return this.rotated() ? this.nextPitch : this.prevPitch;
    }

    public void updateNext() {
        this.updateNextYaw();
        this.updateNextPitch();
        this.nextYaw = this.prevYaw + (float) RotationUtils.yawAngle(this.prevYaw, this.nextYaw);
    }

    private void updateNextYaw() {
        float yaw = BlackOut.mc.player.getYaw();
        if (System.currentTimeMillis() > this.timeYaw) {
            if (this.rotatingYaw == RotatePhase.Rotating) {
                this.rotatingYaw = RotatePhase.Returning;
            }
        } else {
            this.rotatingYaw = RotatePhase.Rotating;
        }

        if (this.rotatingYaw == RotatePhase.Returning) {
            if (Math.abs(RotationUtils.yawAngle(this.nextYaw, yaw)) < SettingUtils.returnSpeed()) {
                this.rotatingYaw = RotatePhase.Inactive;
            } else {
                this.nextYaw = RotationUtils.nextYaw(this.prevYaw, yaw, SettingUtils.returnSpeed());
            }
        }

        if (this.rotatingYaw == RotatePhase.Inactive) {
            this.nextYaw = yaw;
        }
    }

    private void updateNextPitch() {
        ElytraFly elytraFly = ElytraFly.getInstance();
        if (elytraFly.enabled && elytraFly.isBouncing()) {
            this.rotatingPitch = RotatePhase.Rotating;
            this.timePitch = System.currentTimeMillis() + 500L;
            this.priorityPitch = 69420.0;
            this.nextPitch = elytraFly.getPitch();
        } else {
            float pitch = BlackOut.mc.player.getPitch();
            if (System.currentTimeMillis() > this.timePitch) {
                if (this.rotatingPitch == RotatePhase.Rotating) {
                    this.rotatingPitch = RotatePhase.Returning;
                }
            } else {
                this.rotatingPitch = RotatePhase.Rotating;
            }

            if (this.rotatingPitch == RotatePhase.Returning) {
                if (Math.abs(this.nextPitch - pitch) < SettingUtils.returnSpeed()) {
                    this.rotatingPitch = RotatePhase.Inactive;
                } else {
                    this.nextPitch = RotationUtils.nextPitch(this.prevPitch, pitch, SettingUtils.returnSpeed());
                }
            }

            if (this.rotatingPitch == RotatePhase.Inactive) {
                this.nextPitch = pitch;
            }
        }
    }

    public boolean rotated() {
        return !SharedFeatures.shouldPauseRotations() && (this.nextYaw != this.prevYaw || this.nextPitch != this.prevPitch);
    }

    public boolean yawActive() {
        return Managers.ROTATION.rotatingYaw != RotatePhase.Inactive || SharedFeatures.shouldPauseRotations() || PacketFly.getInstance().enabled;
    }

    public boolean pitchActive() {
        return Managers.ROTATION.rotatingPitch != RotatePhase.Inactive
                || SharedFeatures.shouldPauseRotations()
                || PacketFly.getInstance().enabled;
    }

    public enum RotatePhase {
        Rotating,
        Returning,
        Inactive
    }
}
