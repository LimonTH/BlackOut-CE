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
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import bodevelopment.client.blackout.util.render.misc.FramebufferMultiBufferSource;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderESP extends Module {
    private static ShaderESP INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity types will be processed by the shader pipeline.");
    public final Setting<Boolean> texture = this.sgGeneral.booleanSetting("Render Original", true, "Whether to render the original entity texture alongside the shader effect.");
    private final Setting<Integer> bloom = this.sgGeneral.intSetting("Bloom Radius", 3, 1, 10, 1, "The intensity and spread of the glow effect around entities.");
    private final Setting<BlackOutColor> outsideColor = this.sgGeneral.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the outer glowing silhouette.");
    private final Setting<BlackOutColor> insideColor = this.sgGeneral.colorSetting("Interior Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the entity's model body.");

    private static final String MAIN_FBO = "shaderESP";
    private static final String CONVERT_FBO = "shaderESP-convert";
    private static final String BLOOM_FBO = "shaderESP-bloom";
    public static boolean ignore = false;
    private final FramebufferMultiBufferSource fboSource = new FramebufferMultiBufferSource();
    /**
     * Per-entity-type custom color overrides discovered during the current frame.
     * Keyed by {@link ResourceLocation#toString()} of the entity type.
     */
    private final Map<String, CustomColorData> customOverrides = new HashMap<>();

    @SuppressWarnings("unchecked")
    public ShaderESP() {
        super("Shader ESP", "Utilizes post-processing framebuffers and GLSL shaders to render glowing silhouettes around entities.", SubCategory.ENTITIES, true);
        INSTANCE = this;
        ((ListSetting<EntityType<?>>) this.entities).withItemColors(
                () -> this.outsideColor.get().getColor(),
                () -> this.insideColor.get().getColor()
        ).snapshotDefaults();
    }

    public static ShaderESP getInstance() {
        return INSTANCE;
    }

    private static String typeKey(EntityType<?> type) {
        return EntityType.getKey(type).toString(); // e.g. "minecraft:zombie"
    }

    private static String customFboName(String typeKey) {
        return "shaderESP-" + typeKey.replace(':', '_');
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

        ListSetting<EntityType<?>> list = (ListSetting<EntityType<?>>) this.entities;
        Color customLine = list.getItemData(entity.getType(), "lineColor");
        Color customSide = list.getItemData(entity.getType(), "sideColor");

        if (customLine != null || customSide != null) {
            String key = typeKey(entity.getType());
            String fboName = customFboName(key);
            FrameBuffer typeFbo = Managers.FRAME_BUFFER.getBuffer(fboName);

            customOverrides.put(key, new CustomColorData(customLine, customSide));

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            instance.render(state, matrices, this.fboSource, light);
            this.fboSource.drawToFramebuffer(typeFbo);
        } else {
            FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer(MAIN_FBO);

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            instance.render(state, matrices, this.fboSource, light);
            this.fboSource.drawToFramebuffer(buffer);
        }
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
        Managers.FRAME_BUFFER.getBuffer(MAIN_FBO).clear(0.0F, 0.0F, 0.0F, 0.0F);

        for (String key : customOverrides.keySet()) {
            String fboName = customFboName(key);
            Managers.FRAME_BUFFER.getBuffer(fboName).clear(0.0F, 0.0F, 0.0F, 0.0F);
            Managers.FRAME_BUFFER.getBuffer(fboName + "-bloom").clear(0.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public void onRenderHud() {
        FrameBuffer convertBuffer = Managers.FRAME_BUFFER.getBuffer(CONVERT_FBO);
        FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer(BLOOM_FBO);

        processFbo(MAIN_FBO, convertBuffer, bloomBuffer,
                this.insideColor.get().getRGB(),
                this.outsideColor.get().getRGB());

        for (Map.Entry<String, CustomColorData> entry : customOverrides.entrySet()) {
            String fboName = customFboName(entry.getKey());
            CustomColorData data = entry.getValue();

            int insideRgb = data.sideColor != null
                    ? data.sideColor.getRGB()
                    : this.insideColor.get().getRGB();
            int outsideRgb = data.lineColor != null
                    ? data.lineColor.getRGB()
                    : this.outsideColor.get().getRGB();

            processFbo(fboName, convertBuffer, bloomBuffer, insideRgb, outsideRgb);
        }
    }

    private void processFbo(String fboName, FrameBuffer convertBuffer, FrameBuffer bloomBuffer, int insideRgb, int outsideRgb) {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer(fboName);

        convertBuffer.clear(0.0F, 0.0F, 0.0F, 0.0F);
        convertBuffer.bind(true);
        Render2DUtils.renderBufferWith(buffer, Shaders.convert, new ShaderSetup());
        convertBuffer.unbind();

        Render2DUtils.renderBufferWith(convertBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", insideRgb)));

        if (this.bloom.get() > 0) {
            String bloomBufferName = fboName + "-bloom";
            FrameBuffer customBloom = Managers.FRAME_BUFFER.getBuffer(bloomBufferName);

            customBloom.clear(0.0F, 0.0F, 0.0F, 1.0F);
            customBloom.bind(true);
            Render2DUtils.renderBufferWith(convertBuffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
            customBloom.unbind();

            Render2DUtils.blurBufferBW(bloomBufferName, this.bloom.get() + 1);

            customBloom.bind(true);
            Renderer.setTexture(convertBuffer.getTexture(), 1);
            Render2DUtils.renderBufferWith(customBloom, Shaders.subtract, new ShaderSetup(setup -> {
                setup.set("uTexture0", 0);
                setup.set("uTexture1", 1);
            }));
            customBloom.unbind();

            Render2DUtils.renderBufferWith(customBloom, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", outsideRgb)));
        }
    }

    public boolean shouldRender(Entity entity) {
        if (entity == BlackOut.mc.player && !FreeCam.getInstance().enabled) return false;

        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !(entity instanceof AbstractClientPlayer player) || !antiBot.getBots().contains(player)) && this.entities.get().contains(entity.getType());
    }

    private record CustomColorData(Color lineColor, Color sideColor) {
    }
}
