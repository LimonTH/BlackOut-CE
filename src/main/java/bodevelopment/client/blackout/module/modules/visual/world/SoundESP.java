package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.FilterMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PlaySoundEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class SoundESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<FilterMode> filterMode = this.sgGeneral.enumSetting("Filter Mode", FilterMode.Whitelist, ".");
    private final Setting<List<SoundEvent>> sounds = this.sgGeneral
            .registrySetting("Sounds", ".", Registries.SOUND_EVENT, sound -> sound.getId().getPath(), SoundEvents.ENTITY_GENERIC_EXPLODE.value());
    private final Setting<BlackOutColor> color = this.sgGeneral.colorSetting("Color", new BlackOutColor(255, 255, 255, 255), ".");
    private final Setting<Double> fadeIn = this.sgGeneral.doubleSetting("Fade In", 0.1, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> renderTime = this.sgGeneral.doubleSetting("Render Time", 0.2, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> fadeOut = this.sgGeneral.doubleSetting("Fade Out", 0.5, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> scale = this.sgGeneral.doubleSetting("Scale", 1.0, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> scaleInc = this.sgGeneral
            .doubleSetting("Scale Increase", 1.0, 0.0, 5.0, 0.05, "How much should the scale increase when enemy is further away.");
    private final RenderList<SoundRender> renderList = RenderList.getList(false);
    private final MatrixStack stack = new MatrixStack();

    public SoundESP() {
        super("Sound ESP", ".", SubCategory.WORLD, true);
    }

    @Event
    public void onSound(PlaySoundEvent event) {
        SoundInstance instance = event.sound;
        if (this.filterMode.get() != FilterMode.Blacklist || !this.contains(instance)) {
            if (this.filterMode.get() != FilterMode.Whitelist || this.contains(instance)) {
                this.renderList
                        .add(
                                new SoundRender(instance.getX(), instance.getY(), instance.getZ(), instance.getId().getPath()),
                                this.fadeIn.get() + this.renderTime.get() + this.fadeOut.get()
                        );
            }
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        this.stack.push();
        RenderUtils.unGuiScale(this.stack);
        Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        this.renderList.update((render, time, delta) -> this.draw(render.x(), render.y(), render.z(), render.text(), time, camPos));
        this.stack.pop();
    }

    private void draw(double x, double y, double z, String string, double time, Vec3d camPos) {
        Vec2f f = RenderUtils.getCoords(x, y, z, true);
        if (f != null) {
            double alpha = MathHelper.clamp(this.getAlpha(time), 0.0, 1.0);
            float scale = this.getScale(x, y, z, camPos);
            BlackOut.FONT.text(this.stack, string, scale, f.x, f.y, this.color.get().alphaMultiRGB(alpha), true, true);
        }
    }

    private float getScale(double x, double y, double z, Vec3d camPos) {
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;
        float dist = (float) Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz));
        return this.scale.get().floatValue() * 8.0F / dist + this.scaleInc.get().floatValue() / 20.0F * dist;
    }

    private double getAlpha(double time) {
        if (time <= this.fadeIn.get()) {
            return time / this.fadeIn.get();
        } else {
            return time >= this.fadeIn.get() && time <= this.fadeIn.get() + this.renderTime.get()
                    ? 1.0
                    : 1.0 - (time - this.fadeIn.get() - this.renderTime.get()) / this.fadeOut.get();
        }
    }

    private boolean contains(SoundInstance instance) {
        for (SoundEvent event : this.sounds.get()) {
            if (instance.getId().equals(event.getId())) {
                return true;
            }
        }

        return false;
    }

    private record SoundRender(double x, double y, double z, String text) {
    }
}
