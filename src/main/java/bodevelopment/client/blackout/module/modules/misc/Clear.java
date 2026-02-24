package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Clear extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Integer> minX = this.sgGeneral.intSetting("Minimum X", -75, -100, 100, 1, "The lower bound of the X-axis for the clearing area.");
    private final Setting<Integer> maxX = this.sgGeneral.intSetting("Maximum X", 75, -100, 100, 1, "The upper bound of the X-axis for the clearing area.");
    private final Setting<Integer> minY = this.sgGeneral.intSetting("Minimum Y", 0, -65, 350, 1, "The lower bound of the Y-axis for the clearing area.");
    private final Setting<Integer> maxY = this.sgGeneral.intSetting("Maximum Y", 100, -100, 100, 1, "The upper bound of the Y-axis for the clearing area.");
    private final Setting<Integer> minZ = this.sgGeneral.intSetting("Minimum Z", -75, -100, 100, 1, "The lower bound of the Z-axis for the clearing area.");
    private final Setting<Integer> maxZ = this.sgGeneral.intSetting("Maximum Z", 75, -100, 100, 1, "The upper bound of the Z-axis for the clearing area.");
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Game Speed", 5.0, 1.0, 10.0, 0.1, "The multiplier applied to the game timer to accelerate the clearing process.");
    private final Setting<Integer> movement = this.sgGeneral.intSetting("Step Distance", 1, 1, 10, 1, "The distance the player moves per iteration while scanning.");
    private final Setting<Integer> maxMovements = this.sgGeneral.intSetting("Iterations Per Tick", 3, 1, 10, 1, "The maximum number of position updates processed within a single game tick.");
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Interaction Range", 6.0, 1.0, 10.0, 0.1, "The maximum distance at which blocks will be targeted for removal.");

    private boolean setTimer = true;
    private int x = 0;
    private int y = 0;
    private int z = 0;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private boolean directionX = false;
    private boolean directionZ = false;

    public Clear() {
        super("Clear", "Automates block removal across a specified 3D volume, typically used for clearing large areas on creative servers.", SubCategory.MISC, true);
    }

    @Override
    public void onEnable() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.directionX = false;
        this.directionZ = false;
    }

    @Override
    public void onDisable() {
        if (this.setTimer) {
            this.setTimer = false;
            Timer.reset();
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.tick();
        event.set(this, 0.0, 0.0, 0.0);
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        this.setTimer = true;
        Timer.set(this.timer.get().floatValue());
    }

    private void tick() {
        this.updateScale();

        for (int i = 0; i < this.maxMovements.get(); i++) {
            if (this.move()) {
                this.disable("done");
                return;
            }

            List<BlockPos> list = new ArrayList<>();
            this.find(list);
            if (!list.isEmpty()) {
                this.updatePos();
                this.mine(list);
                return;
            }
        }

        this.updatePos();
    }

    private void mine(List<BlockPos> list) {
        list.forEach(this::clickBlock);
    }

    private boolean move() {
        if (this.tickX()) {
            this.directionX = !this.directionX;
            this.x = this.directionX ? this.sizeX : 0;
            if (this.tickZ()) {
                this.directionZ = !this.directionZ;
                this.z = this.directionZ ? this.sizeZ : 0;
                return this.y++ >= this.sizeY;
            }
        }

        return false;
    }

    private boolean tickX() {
        return this.directionX ? --this.x < 0 : ++this.x > this.sizeX;
    }

    private boolean tickZ() {
        return this.directionZ ? --this.z < 0 : ++this.z > this.sizeZ;
    }

    private void updatePos() {
        Vec3d pos = this.getPos();
        BlackOut.mc.player.setPosition(pos);
        this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, BlackOut.mc.player.isOnGround()));
    }

    private Vec3d getPos() {
        return new Vec3d(
                MathHelper.lerp((float) this.x / this.sizeX, this.minX.get(), this.maxX.get()),
                MathHelper.lerp((float) this.y / this.sizeY, this.minY.get(), this.maxY.get()),
                MathHelper.lerp((float) this.z / this.sizeZ, this.minZ.get(), this.maxZ.get())
        );
    }

    private void updateScale() {
        this.sizeX = (int) Math.ceil((this.maxX.get() - this.minX.get()) / this.movement.get().floatValue());
        this.sizeY = (int) Math.ceil((this.maxY.get() - this.minY.get()) / this.movement.get().floatValue());
        this.sizeZ = (int) Math.ceil((this.maxZ.get() - this.minZ.get()) / this.movement.get().floatValue());
    }

    private void find(List<BlockPos> list) {
        Vec3d eyePos = this.getPos().add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0);
        BlockPos center = BlockPos.ofFloored(eyePos);
        int r = (int) Math.ceil(this.range.get());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (!(eyePos.squaredDistanceTo(pos.toCenterPos()) > this.range.get() * this.range.get())
                            && !(BlackOut.mc.world.getBlockState(pos).getBlock() instanceof AirBlock)) {
                        list.add(pos);
                    }
                }
            }
        }
    }

    private void clickBlock(BlockPos pos) {
        this.sendSequenced(sequence -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN, sequence));
        BlackOut.mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }
}
