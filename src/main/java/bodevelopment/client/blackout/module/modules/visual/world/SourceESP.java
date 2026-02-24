package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SourceESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> water = this.sgGeneral.booleanSetting("Water", true, ".");
    private final Setting<Boolean> lava = this.sgGeneral.booleanSetting("Lava", true, ".");
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Range", 8.0, 0.0, 10.0, 0.1, ".");
    private final Setting<Integer> bloom = this.sgGeneral.intSetting("Bloom", 3, 0, 10, 1, ".");
    private final Setting<BlackOutColor> fillColor = this.sgGeneral.colorSetting("Fill Color", new BlackOutColor(255, 0, 0, 50), "");
    private final Setting<BlackOutColor> bloomColor = this.sgGeneral.colorSetting("Bloom Color", new BlackOutColor(255, 0, 0, 150), "");
    private final BlackOutColor white = new BlackOutColor(255, 255, 255, 255);
    private final List<Pair<BlockPos, Boolean>> sources = new ArrayList<>();
    private long prevCalc = 0L;

    public SourceESP() {
        super("Source ESP", "Highlights water and lava sources.", SubCategory.WORLD, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (System.currentTimeMillis() - this.prevCalc > 100L) {
            this.find(BlackOut.mc.player.getBlockPos(), (int) Math.ceil(this.range.get()), this.range.get() * this.range.get());
            this.prevCalc = System.currentTimeMillis();
        }

        this.render();
    }

    @Event
    public void onRenderHud(RenderEvent.Hud.Pre event) {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("sourceESP");
        FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer("sourceESP-bloom");
        RenderUtils.renderBufferWith(buffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.fillColor.get().getRGB())));
        if (this.bloom.get() > 0) {
            bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
            bloomBuffer.bind(true);
            RenderUtils.renderBufferWith(buffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
            bloomBuffer.unbind();
            RenderUtils.blurBufferBW("sourceESP-bloom", this.bloom.get() + 1);
            bloomBuffer.bind(true);
            Renderer.setTexture(buffer.getTexture(), 1);
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.subtract, new ShaderSetup(setup -> {
                setup.set("uTexture0", 0);
                setup.set("uTexture1", 1);
            }));
            bloomBuffer.unbind();
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.bloomColor.get().getRGB())));
        }
    }

    private void render() {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("sourceESP");
        buffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
        buffer.bind(true);
        this.sources.forEach(this::renderShader);
        buffer.unbind();
    }

    private void renderShader(Pair<BlockPos, Boolean> pair) {
        Render3DUtils.box(BoxUtils.get(pair.getLeft()), this.white, null, RenderShape.Sides);
    }

    private void find(BlockPos center, int r, double rangeSq) {
        this.sources.clear();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (!(x * x + y * y + z * z > rangeSq)) {
                        BlockPos pos = center.add(x, y, z);
                        FluidState fluidState = BlackOut.mc.world.getFluidState(pos);
                        if (!fluidState.isEmpty() && fluidState.isStill()) {
                            if (fluidState.isIn(FluidTags.WATER)) {
                                if (this.water.get()) {
                                    this.sources.add(new Pair<>(pos, true));
                                }
                            } else if (this.lava.get() && fluidState.isIn(FluidTags.LAVA)) {
                                this.sources.add(new Pair<>(pos, false));
                            }
                        }
                    }
                }
            }
        }
    }
}
