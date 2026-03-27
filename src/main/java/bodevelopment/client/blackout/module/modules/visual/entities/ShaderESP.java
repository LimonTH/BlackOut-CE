package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.FramebufferMultiBufferSource;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class ShaderESP extends Module {
    private static ShaderESP INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity types will be processed by the shader pipeline.");
    public final Setting<Boolean> texture = this.sgGeneral.booleanSetting("Render Original", true, "Whether to render the original entity texture alongside the shader effect.");
    private final Setting<Integer> bloom = this.sgGeneral.intSetting("Bloom Radius", 3, 1, 10, 1, "The intensity and spread of the glow effect around entities.");
    private final Setting<BlackOutColor> outsideColor = this.sgGeneral.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the outer glowing silhouette.");
    private final Setting<BlackOutColor> insideColor = this.sgGeneral.colorSetting("Interior Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the entity's model body.");

    public static boolean ignore = false;

    public ShaderESP() {
        super("Shader ESP", "Utilizes post-processing framebuffers and GLSL shaders to render glowing silhouettes around entities.", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static ShaderESP getInstance() {
        return INSTANCE;
    }

    public <T extends Entity, S extends EntityRenderState> void onRender(
            EntityRenderer<? super T, S> instance, T entity, S state, PoseStack matrices, MultiBufferSource vertexConsumers, int light
    ) {
        if (this.texture.get()) {
            instance.render(state, matrices, vertexConsumers, light);
        }
        if (this.shouldRenderLabel(entity, state)) {
            instance.renderNameTag(state, state.nameTag, matrices, vertexConsumers, light);
        }

        if (!this.shouldRender(entity)) return;

        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("shaderESP");

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        FramebufferMultiBufferSource fboSource = new FramebufferMultiBufferSource();
        instance.render(state, matrices, fboSource, light);
        fboSource.drawToFramebuffer(buffer);
    }

    private <S extends EntityRenderState> boolean shouldRenderLabel(Entity entity, S state) {
        if (Nametags.shouldCancelLabel(entity)) {
            return false;
        }
        if (state.nameTag == null) {
            return false;
        }
        return entity.shouldShowName() || entity.hasCustomName();
    }

    @Event
    public void onRenderPre(RenderEvent.World.Pre event) {
        Managers.FRAME_BUFFER.getBuffer("shaderESP").clear(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public void onRenderHud() {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("shaderESP");
        FrameBuffer convertBuffer = Managers.FRAME_BUFFER.getBuffer("shaderESP-convert");
        FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer("shaderESP-bloom");

        convertBuffer.clear(0.0F, 0.0F, 0.0F, 0.0F);
        convertBuffer.bind(true);
        RenderUtils.renderBufferWith(buffer, Shaders.convert, new ShaderSetup());
        convertBuffer.unbind();

        RenderUtils.renderBufferWith(convertBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.insideColor.get().getRGB())));

        if (this.bloom.get() > 0) {
            bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
            bloomBuffer.bind(true);
            RenderUtils.renderBufferWith(convertBuffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
            bloomBuffer.unbind();

            RenderUtils.blurBufferBW("shaderESP-bloom", this.bloom.get() + 1);

            bloomBuffer.bind(true);
            Renderer.setTexture(convertBuffer.getTexture(), 1);
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.subtract, new ShaderSetup(setup -> {
                setup.set("uTexture0", 0);
                setup.set("uTexture1", 1);
            }));
            bloomBuffer.unbind();

            RenderUtils.renderBufferWith(bloomBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.outsideColor.get().getRGB())));
        }
    }

    public boolean shouldRender(Entity entity) {
        if (entity == BlackOut.mc.player && !FreeCam.getInstance().enabled) return false;

        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !(entity instanceof AbstractClientPlayer player) || !antiBot.getBots().contains(player)) && this.entities.get().contains(entity.getType());
    }
}
