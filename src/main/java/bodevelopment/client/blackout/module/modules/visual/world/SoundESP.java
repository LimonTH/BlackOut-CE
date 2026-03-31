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
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SoundESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<FilterMode> filterMode = this.sgGeneral.enumSetting("Filter Logic", FilterMode.Whitelist, "Determines whether the sounds list acts as an inclusion or exclusion filter.");
    private final Setting<List<SoundEvent>> sounds = this.sgGeneral.registrySetting(
            "Tracked Sounds",
            "The specific sound events to be visualized in the world.",
            BuiltInRegistries.SOUND_EVENT,
            sound -> {
                String key = "subtitles." + sound.location().toLanguageKey();
                String translated = Component.translatable(key).getString();
                return translated.equals(key) ? sound.location().getPath() : translated;
            },
            SoundEvents.GENERIC_EXPLODE.value()
    );
    private final Setting<BlackOutColor> color = this.sgGeneral.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), "The color and transparency of the rendered sound labels.");
    private final Setting<Double> fadeIn = this.sgGeneral.doubleSetting("Fade-In Duration", 0.1, 0.0, 10.0, 0.1, "The time in seconds for the label to reach full opacity.");
    private final Setting<Double> renderTime = this.sgGeneral.doubleSetting("Dwell Duration", 0.2, 0.0, 10.0, 0.1, "The amount of time the label remains at full opacity before fading.");
    private final Setting<Double> fadeOut = this.sgGeneral.doubleSetting("Fade-Out Duration", 0.5, 0.0, 10.0, 0.1, "The time in seconds for the label to disappear completely.");
    private final Setting<Double> scale = this.sgGeneral.doubleSetting("Base Scale", 1.0, 0.0, 10.0, 0.1, "The initial size multiplier for the text labels.");
    private final Setting<Double> scaleInc = this.sgGeneral.doubleSetting("Distance Scaling", 1.0, 0.0, 5.0, 0.05, "Adjusts label size based on distance to ensure visibility from afar.");

    private final RenderList<SoundRender> renderList = RenderList.getList(false);
    private final PoseStack stack = new PoseStack();

    public SoundESP() {
        super("Sound ESP", "Captures and displays localized sound events as spatial text labels, providing visual situational awareness for audio cues.", SubCategory.WORLD, true);
    }

    @Event
    public void onSound(PlaySoundEvent event) {
        SoundInstance instance = event.sound;

        boolean contains = this.contains(instance);
        if ((this.filterMode.get() == FilterMode.Blacklist && !contains) ||
                (this.filterMode.get() == FilterMode.Whitelist && contains)) {

            String translationKey = "subtitles." + instance.getLocation().toLanguageKey();

            String localizedName = Component.translatable(translationKey).getString();

            if (localizedName.equals(translationKey)) {
                localizedName = instance.getLocation().getPath();
            }

            this.renderList.add(
                    new SoundRender(instance.getX(), instance.getY(), instance.getZ(), localizedName),
                    this.fadeIn.get() + this.renderTime.get() + this.fadeOut.get()
            );
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        this.stack.pushPose();
        Render2DUtils.unGuiScale(this.stack);
        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();
        this.renderList.update((render, time, delta) -> this.draw(render.x(), render.y(), render.z(), render.text(), time, camPos));
        this.stack.popPose();
    }

    private void draw(double x, double y, double z, String string, double time, Vec3 camPos) {
        Vec2 f = Render2DUtils.getCoords(x, y, z, true);
        if (f != null) {
            double alpha = Mth.clamp(this.getAlpha(time), 0.0, 1.0);
            float scale = this.getScale(x, y, z, camPos);
            BlackOut.FONT.text(this.stack, string, scale, f.x, f.y, this.color.get().alphaMultiRGB(alpha), true, true);
        }
    }

    private float getScale(double x, double y, double z, Vec3 camPos) {
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
            if (instance.getLocation().equals(event.location())) {
                return true;
            }
        }

        return false;
    }

    private record SoundRender(double x, double y, double z, String text) {
    }
}
