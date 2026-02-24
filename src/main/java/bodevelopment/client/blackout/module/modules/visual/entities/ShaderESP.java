package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.mixin.accessors.AccessorEntityRenderer;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.List;

public class ShaderESP extends Module {
    public static boolean ignore = false;
    private static ShaderESP INSTANCE;
    private final BufferBuilderStorage storage = new BufferBuilderStorage(69);
    private final VertexConsumerProvider.Immediate vertexConsumerProvider = this.storage.getEntityVertexConsumers();
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Entities", ".");
    public final Setting<Boolean> texture = this.sgGeneral.booleanSetting("Texture", true, ".");
    private final Setting<Integer> bloom = this.sgGeneral.intSetting("Bloom", 3, 1, 10, 1, ".");
    private final Setting<BlackOutColor> outsideColor = this.sgGeneral.colorSetting("Outside Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> insideColor = this.sgGeneral.colorSetting("Inside Color", new BlackOutColor(255, 0, 0, 50), ".");

    public ShaderESP() {
        super("Shader ESP", ".", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static ShaderESP getInstance() {
        return INSTANCE;
    }

    public <T extends Entity> void onRender(
            EntityRenderer<T> instance, T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light
    ) {
        if (this.texture.get()) {
            instance.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }

        if (this.shouldRenderLabel(entity)) {
            ignore = true;

            ((AccessorEntityRenderer<?>) instance).invokeRenderLabelIfPresent(
                    entity,
                    entity.getDisplayName(),
                    matrices,
                    vertexConsumers,
                    light,
                    BlackOut.mc.getRenderTickCounter().getTickDelta(false)
            );

            ignore = false;
        }

        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("shaderESP");
        buffer.bind(true);
        instance.render(entity, yaw, tickDelta, matrices, this.vertexConsumerProvider, light);
        this.vertexConsumerProvider.draw();
        buffer.unbind();
    }

    private boolean shouldRenderLabel(Entity entity) {
        if (Nametags.shouldCancelLabel(entity)) {
            return false;
        } else {
            return !this.shouldRender(entity) && entity.shouldRenderName() && entity.hasCustomName();
        }
    }

    public void onRenderHud() {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("shaderESP");
        FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer("shaderESP-bloom");
        buffer.bind(true);
        RenderUtils.renderBufferWith(buffer, Shaders.convert, new ShaderSetup());
        buffer.unbind();
        RenderUtils.renderBufferWith(buffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.insideColor.get().getRGB())));
        if (this.bloom.get() > 0) {
            bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
            bloomBuffer.bind(true);
            RenderUtils.renderBufferWith(buffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
            bloomBuffer.unbind();
            RenderUtils.blurBufferBW("shaderESP-bloom", this.bloom.get() + 1);
            bloomBuffer.bind(true);
            Renderer.setTexture(buffer.getTexture(), 1);
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.subtract, new ShaderSetup());
            bloomBuffer.unbind();
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.outsideColor.get().getRGB())));
            buffer.clear(1.0F, 1.0F, 1.0F, 0.0F);
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !(entity instanceof AbstractClientPlayerEntity player) || !antiBot.getBots().contains(player)) && this.entities.get().contains(entity.getType());
    }
}
