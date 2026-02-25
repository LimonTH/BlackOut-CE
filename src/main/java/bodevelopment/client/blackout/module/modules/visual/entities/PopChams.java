package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PopEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class PopChams extends Module {
    private static PopChams INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> time = this.sgGeneral.doubleSetting("Lifespan", 1.0, 0.0, 5.0, 0.05, "The duration in seconds that the phantom model remains visible.");
    private final Setting<Double> y = this.sgGeneral.doubleSetting("Vertical Drift", 0.0, -5.0, 5.0, 0.1, "Applies a vertical offset or upward travel to the rendered model.");
    private final Setting<Double> scale = this.sgGeneral.doubleSetting("Geometry Scale", 1.0, 0.0, 5.0, 0.1, "The size multiplier for the rendered pop phantom.");
    private final Setting<Boolean> enemy = this.sgGeneral.booleanSetting("Hostile Targets", true, "Generates chams when a non-friendly player consumes a totem.");
    private final Setting<Boolean> friends = this.sgGeneral.booleanSetting("Friendly Targets", true, "Generates chams when a whitelisted friend consumes a totem.");
    private final Setting<Boolean> self = this.sgGeneral.booleanSetting("Self Trigger", false, "Generates chams when you consume a totem.");
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Mesh Mode", RenderShape.Full, "Defines which geometric components (faces, lines, or both) of the player model are rendered.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Wireframe Color", new BlackOutColor(255, 255, 255, 255), "The color of the model's outer edges.");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Face Color", new BlackOutColor(255, 255, 255, 50), "The color applied to the polygon faces of the model.");

    private final TimerList<Pop> pops = new TimerList<>(true);
    public PopChams() {
        super("Pop Chams", "Renders a temporary, translucent phantom of a player's model at the exact position where they 'popped' a Totem of Undying.", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static PopChams getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        if (this.pops.getList().isEmpty()) return;

        this.pops.forEach(timer -> {
            long now = System.currentTimeMillis();
            long total = timer.endTime - timer.startTime;

            float progress;
            if (total <= 0) {
                progress = 1.0f;
            } else {
                progress = (float) (now - timer.startTime) / (float) total;
            }

            progress = MathHelper.clamp(progress, 0.0f, 1.0f);

            if (!Float.isNaN(progress)) {
                this.renderPop(event.stack, timer.value, progress);
            }
        });
    }

    @Event
    public void onPop(PopEvent event) {

        if (this.shouldRender(event.player)) {
            this.pops.add(new Pop(event.player), this.time.get());
        }
    }

    private boolean shouldRender(AbstractClientPlayerEntity player) {
        if (player == BlackOut.mc.player) {
            return this.self.get();
        } else {
            return Managers.FRIENDS.isFriend(player) ? this.friends.get() : this.enemy.get();
        }
    }

    private void renderPop(MatrixStack stack, Pop pop, float progress) {
        Camera camera = BlackOut.mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        stack.push();
        stack.loadIdentity();
        stack.multiply(new Quaternionf(camera.getRotation()).conjugate());

        double x = pop.x - camPos.x;
        double y = pop.y - camPos.y;
        double z = pop.z - camPos.z;

        stack.translate((float) x, (float) y, (float) z);

        WireframeRenderer.renderServerPlayer(
                stack,
                pop.player,
                pop.modelData,
                this.lineColor.get(),
                this.sideColor.get(),
                this.renderShape.get(),
                progress,
                this.y.get(),
                this.scale.get().floatValue()
        );

        stack.pop();
    }

    private static class Pop {
        private final AbstractClientPlayerEntity player;
        private final double x;
        private final double y;
        private final double z;
        private final WireframeRenderer.ModelData modelData;

        public Pop(AbstractClientPlayerEntity player) {
            this.player = player;
            float tickDelta = BlackOut.mc.getRenderTickCounter().getTickDelta(true);

            this.x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
            this.y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
            this.z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());

            this.modelData = new WireframeRenderer.ModelData(player, tickDelta);
        }
    }
}
