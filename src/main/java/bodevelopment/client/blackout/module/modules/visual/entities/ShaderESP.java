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
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.joml.Matrix4f;

import java.util.List;

public class ShaderESP extends Module {
    private static ShaderESP INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity types will be processed by the shader pipeline.");
    public final Setting<Boolean> texture = this.sgGeneral.booleanSetting("Render Original", true, "Whether to render the original entity texture alongside the shader effect.");
    private final Setting<Integer> bloom = this.sgGeneral.intSetting("Bloom Radius", 3, 1, 10, 1, "The intensity and spread of the glow effect around entities.");
    private final Setting<BlackOutColor> outsideColor = this.sgGeneral.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the outer glowing silhouette.");
    private final Setting<BlackOutColor> insideColor = this.sgGeneral.colorSetting("Interior Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the entity's model body.");

    public static boolean ignore = false;
    private BufferBuilderStorage storage;
    private VertexConsumerProvider.Immediate vertexConsumerProvider;

    private VertexConsumerProvider.Immediate getVCP() {
        if (storage == null) {
            storage = new BufferBuilderStorage(69);
            vertexConsumerProvider = storage.getEntityVertexConsumers();
        }
        return vertexConsumerProvider;
    }

    public ShaderESP() {
        super("Shader ESP", "Utilizes post-processing framebuffers and GLSL shaders to render glowing silhouettes around entities.", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static ShaderESP getInstance() {
        return INSTANCE;
    }

    public <T extends Entity, S extends EntityRenderState> void onRender(
            EntityRenderer<T, S> instance, T entity, S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light
    ) {
        if (this.texture.get()) {
            instance.render(state, matrices, vertexConsumers, light);
        }
        if (!this.shouldRender(entity)) return;
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("shaderESP");
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
        buffer.bind(true);
        VertexConsumerProvider.Immediate vcp = getVCP();
        instance.render(state, matrices, vcp, light);
        vcp.draw();
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
