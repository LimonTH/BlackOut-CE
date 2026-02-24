package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Search extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Blocks", "");
    private final Setting<Boolean> dynamicBox = this.sgGeneral.booleanSetting("Dynamic Box", true, ".");
    private final Setting<Boolean> instantScan = this.sgGeneral.booleanSetting("Instant Scan", false, ".");
    private final Setting<Integer> scanSpeed = this.sgGeneral.intSetting("Scan Speed", 1, 1, 10, 1, "Chunks per frame.", () -> !this.instantScan.get());
    private final Setting<BlackOutColor> fillColor = this.sgGeneral.colorSetting("Fill Color", new BlackOutColor(255, 0, 0, 50), "");
    private final Setting<Integer> bloom = this.sgGeneral.intSetting("Bloom", 5, 0, 10, 1, ".");
    private final Setting<BlackOutColor> bloomColor = this.sgGeneral.colorSetting("Bloom Color", new BlackOutColor(255, 0, 0, 100), "");
    private final Setting<Boolean> onlyExposed = this.sgGeneral.booleanSetting("Only Exposed", false, ".");
    private final List<BlockPos> positions = Collections.synchronizedList(new ArrayList<>());
    private final List<ChunkPos> toScan = new ArrayList<>();
    private final List<ChunkPos> prevChunks = new ArrayList<>();

    public Search() {
        super("Search", "Highlights blocks.", SubCategory.WORLD, true);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Event
    public void onJoin(GameJoinEvent event) {
        this.reset();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.world != null) {
            this.checkChunks();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.find();
        this.render();
    }

    @Event
    public void onRenderHud(RenderEvent.Hud.Pre event) {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("search");
        FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer("search-bloom");
        RenderUtils.renderBufferWith(buffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.fillColor.get().getRGB())));
        if (this.bloom.get() > 0) {
            bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
            bloomBuffer.bind(true);
            RenderUtils.renderBufferWith(buffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
            bloomBuffer.unbind();
            RenderUtils.blurBufferBW("search-bloom", this.bloom.get() + 1);
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

    @Event
    public void onState(BlockStateEvent event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.onBlock(event.state.getBlock(), event.pos, true);
        }
    }

    private void reset() {
        this.prevChunks.clear();
        this.positions.clear();
    }

    private void checkChunks() {
        List<ChunkPos> current = new ArrayList<>();
        ClientChunkManager.ClientChunkMap map = BlackOut.mc.world.getChunkManager().chunks;

        for (int i = 0; i < map.chunks.length(); i++) {
            WorldChunk chunk = map.chunks.get(i);
            if (chunk != null) {
                ChunkPos pos = chunk.getPos();
                if (!this.prevChunks.contains(pos)) {
                    this.addScan(pos);
                }

                this.prevChunks.remove(pos);
                current.add(pos);
            }
        }

        this.prevChunks.forEach(this::unScan);
        this.prevChunks.clear();
        this.prevChunks.addAll(current);
    }

    private void unScan(ChunkPos pos) {
        this.toScan.remove(pos);
        this.positions
                .removeIf(
                        block -> block.getX() >= pos.getStartX()
                                && block.getX() <= pos.getEndX()
                                && block.getZ() >= pos.getStartZ()
                                && block.getZ() <= pos.getEndZ()
                );
    }

    private void render() {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("search");
        buffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
        buffer.bind(true);
        Render3DUtils.matrices.push();
        Render3DUtils.setRotation(Render3DUtils.matrices);
        Render3DUtils.start();
        Matrix4f matrix4f = Render3DUtils.matrices.peek().getPositionMatrix();
        Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        this.positions.forEach(pos -> this.renderBox(bufferBuilder, matrix4f, pos, camPos));
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        Render3DUtils.end();
        Render3DUtils.matrices.pop();
        buffer.unbind();
    }

    private void renderBox(BufferBuilder bufferBuilder, Matrix4f matrix4f, BlockPos pos, Vec3d camPos) {
        Box box = this.getBox(pos);
        float minX = (float) (box.minX - camPos.x);
        float maxX = (float) (box.maxX - camPos.x);
        float minY = (float) (box.minY - camPos.y);
        float maxY = (float) (box.maxY - camPos.y);
        float minZ = (float) (box.minZ - camPos.z);
        float maxZ = (float) (box.maxZ - camPos.z);
        this.drawQuads(bufferBuilder, matrix4f, minX, maxX, minY, maxY, minZ, maxZ);
    }

    private void drawQuads(BufferBuilder bufferBuilder, Matrix4f matrix4f, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        if (minY > 0.0F) {
            this.vertex(bufferBuilder, matrix4f, minX, minY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, minY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, minY, maxZ);
            this.vertex(bufferBuilder, matrix4f, minX, minY, maxZ);
        } else if (maxY < 0.0F) {
            this.vertex(bufferBuilder, matrix4f, minX, maxY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, maxY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, maxY, maxZ);
            this.vertex(bufferBuilder, matrix4f, minX, maxY, maxZ);
        }

        if (minX > 0.0F) {
            this.vertex(bufferBuilder, matrix4f, minX, minY, minZ);
            this.vertex(bufferBuilder, matrix4f, minX, maxY, minZ);
            this.vertex(bufferBuilder, matrix4f, minX, maxY, maxZ);
            this.vertex(bufferBuilder, matrix4f, minX, minY, maxZ);
        } else if (maxX < 0.0F) {
            this.vertex(bufferBuilder, matrix4f, maxX, minY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, maxY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, maxY, maxZ);
            this.vertex(bufferBuilder, matrix4f, maxX, minY, maxZ);
        }

        if (minZ > 0.0F) {
            this.vertex(bufferBuilder, matrix4f, minX, minY, minZ);
            this.vertex(bufferBuilder, matrix4f, minX, maxY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, maxY, minZ);
            this.vertex(bufferBuilder, matrix4f, maxX, minY, minZ);
        } else if (maxZ < 0.0F) {
            this.vertex(bufferBuilder, matrix4f, minX, minY, maxZ);
            this.vertex(bufferBuilder, matrix4f, minX, maxY, maxZ);
            this.vertex(bufferBuilder, matrix4f, maxX, maxY, maxZ);
            this.vertex(bufferBuilder, matrix4f, maxX, minY, maxZ);
        }
    }

    private void vertex(BufferBuilder bufferBuilder, Matrix4f matrix4f, float x, float y, float z) {
        bufferBuilder.vertex(matrix4f, x, y, z).color(1.0F, 1.0F, 1.0F, 1.0F).normal(0.0F, 0.0F, 0.0F);
    }

    private Box getBox(BlockPos pos) {
        if (this.dynamicBox.get()) {
            VoxelShape shape = BlackOut.mc.world.getBlockState(pos).getOutlineShape(BlackOut.mc.world, pos);
            if (!shape.isEmpty()) {
                return shape.getBoundingBox().offset(pos);
            }
        }

        return BoxUtils.get(pos);
    }

    private void find() {
        if (this.instantScan.get()) {
            this.toScan.forEach(this::scan);
            this.toScan.clear();
        } else {
            for (int i = 0; i < this.scanSpeed.get(); i++) {
                if (this.toScan.isEmpty()) {
                    return;
                }

                this.scan(this.toScan.getFirst());
                this.toScan.removeFirst();
            }
        }
    }

    private void scan(ChunkPos pos) {
        for (int x = pos.getStartX(); x <= pos.getEndX(); x++) {
            for (int y = -64; y <= 319; y++) {
                for (int z = pos.getStartZ(); z <= pos.getEndZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (this.blocks.get().contains(BlackOut.mc.world.getBlockState(p).getBlock())) {
                        this.positions.add(p);
                    }
                }
            }
        }
    }

    private void addScan(ChunkPos pos) {
        if (!this.toScan.contains(pos)) {
            this.toScan.add(pos);
        }
    }

    private void onBlock(Block block, BlockPos pos, boolean first) {
        boolean valid = this.blocks.get().contains(block);
        if (valid && this.onlyExposed.get()) {
            valid = false;

            for (Direction dir : Direction.values()) {
                BlockPos offsetPos = pos.offset(dir);
                if (!BlackOut.mc.world.getBlockState(offsetPos).getBlock().settings.opaque) {
                    valid = true;
                    break;
                }
            }
        }

        if (valid) {
            if (!this.positions.contains(pos)) {
                this.positions.add(pos);
            }
        } else {
            this.positions.remove(pos);
        }

        if (first) {
            for (Direction dirx : Direction.values()) {
                BlockPos offsetPos = pos.offset(dirx);
                this.onBlock(BlackOut.mc.world.getBlockState(offsetPos).getBlock(), offsetPos, false);
            }
        }
    }
}
