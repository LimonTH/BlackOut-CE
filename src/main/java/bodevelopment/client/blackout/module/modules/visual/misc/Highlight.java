package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.HashMap;
import java.util.Map;

public class Highlight extends Module {
    private static Highlight INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<RenderMode> mode = this.sgGeneral.enumSetting("Animation Mode", RenderMode.Fade, "The visual style used to transition between highlighted blocks.");
    private final Setting<Double> moveSpeed = this.sgGeneral.doubleSetting("Translation Speed", 1.0, 1.0, 10.0, 0.1, "The speed at which the selection box slides toward the new target.", () -> this.mode.get() == RenderMode.Move);
    private final Setting<RenderShape> shape = this.sgGeneral.enumSetting("Mesh Mode", RenderShape.Sides, "Determines which geometric components of the highlighted box are rendered.");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Face Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the polygonal faces of the selection box.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Edge Color", new BlackOutColor(255, 0, 0, 255), "The color applied to the wireframe edges of the selection box.");
    private final Setting<Double> fadeIn = this.sgGeneral.doubleSetting("Opacity Attack", 2.0, 0.0, 20.0, 0.2, "How quickly the highlight reaches maximum visibility.");
    private final Setting<Double> fadeOut = this.sgGeneral.doubleSetting("Opacity Decay", 1.0, 0.0, 20.0, 0.2, "How quickly the highlight vanishes after moving your crosshair.");

    private final Map<BlockPos, MutableDouble> alphas = new HashMap<>();
    private Vec3d middle = null;
    private Vec3d targetPos = null;
    private double lx = 0.0;
    private double ly = 0.0;
    private double lz = 0.0;
    private double tlx = 0.0;
    private double tly = 0.0;
    private double tlz = 0.0;
    private double alpha = 0.0;

    public Highlight() {
        super("Highlight", "Draws a customizable geometric box around the block currently targeted by your crosshair.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static Highlight getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            BlockPos pos = this.getCurrentPos();
            switch (this.mode.get()) {
                case Move:
                    this.alphas.clear();
                    this.moveRender(this.getBox(pos), event.frameTime);
                    break;
                case Fade:
                    this.fadeRender(pos, event.frameTime);
            }
        }
    }

    private BlockPos getCurrentPos() {
        return BlackOut.mc.crosshairTarget instanceof BlockHitResult blockHitResult ? blockHitResult.getBlockPos() : null;
    }

    private void moveRender(Box box, double frameTime) {
        if (box == null) {
            this.alpha = Math.max(this.alpha - frameTime * this.fadeOut.get() * 5.0, 0.0);
        } else {
            this.alpha = Math.min(this.alpha + frameTime * this.fadeIn.get() * 5.0, 1.0);
        }

        if (box != null) {
            this.targetPos = BoxUtils.middle(box);
            this.tlx = box.getLengthX();
            this.tly = box.getLengthY();
            this.tlz = box.getLengthZ();
        }

        this.animLengths(frameTime);
        this.movePos(frameTime);
        if (this.middle != null) {
            Box box1 = new Box(
                    this.middle.getX() - this.lx / 2.0,
                    this.middle.getY() - this.ly / 2.0,
                    this.middle.getZ() - this.lz / 2.0,
                    this.middle.getX() + this.lx / 2.0,
                    this.middle.getY() + this.ly / 2.0,
                    this.middle.getZ() + this.lz / 2.0
            );
            Render3DUtils.box(box1, this.sideColor.get().alphaMulti(this.alpha), this.lineColor.get().alphaMulti(this.alpha), this.shape.get());
        }
    }

    private void animLengths(double frameTime) {
        frameTime = Math.min(frameTime, 1.0);
        double dx = this.tlx - this.lx;
        double dy = this.tly - this.ly;
        double dz = this.tlz - this.lz;
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);
        double adz = Math.abs(dz);
        this.lx += dx * frameTime * 10.0 * (adx * adx + 1.0);
        this.ly += dy * frameTime * 10.0 * (ady * ady + 1.0);
        this.lz += dz * frameTime * 10.0 * (adz * adz + 1.0);
    }

    private void movePos(double frameTime) {
        if (this.targetPos != null) {
            if (this.alpha <= 0.0) {
                this.middle = null;
            } else {
                if (this.middle == null) {
                    this.middle = this.targetPos;
                }

                Vec3d diff = this.targetPos.subtract(this.middle);
                double length = diff.length();
                if (Double.isNaN(length)) {
                    this.middle = this.targetPos;
                } else if (!(length <= 0.0)) {
                    double delta = this.moveSpeed.get() / length * frameTime;
                    if (delta >= 1.0) {
                        this.middle = this.targetPos;
                    } else {
                        this.middle = this.middle.add(diff.multiply(delta));
                    }
                }
            }
        }
    }

    private void fadeRender(BlockPos pos, double frameTime) {
        if (pos != null && !this.alphas.containsKey(pos)) {
            this.alphas.put(pos, new MutableDouble(0.0));
        }

        this.alphas.entrySet().removeIf(entry -> {
            BlockPos p = entry.getKey();
            MutableDouble a = entry.getValue();
            if (p.equals(pos)) {
                a.setValue(Math.min(a.getValue() + frameTime * this.fadeIn.get() * 5.0, 1.0));
            } else {
                double reduced = a.getValue() - frameTime * this.fadeOut.get() * 5.0;
                if (reduced <= 0.0) {
                    return true;
                }

                a.setValue(reduced);
            }

            Box box = this.getBox(p);
            if (box == null) {
                return false;
            } else {
                Render3DUtils.box(box, this.sideColor.get().alphaMulti(a.getValue()), this.lineColor.get().alphaMulti(a.getValue()), this.shape.get());
                return false;
            }
        });
    }

    private Box getBox(BlockPos pos) {
        if (pos == null) {
            return null;
        } else {
            BlockState state = BlackOut.mc.world.getBlockState(pos);
            VoxelShape shape = state.getOutlineShape(BlackOut.mc.world, pos);
            return shape.isEmpty() ? null : shape.getBoundingBox().offset(pos);
        }
    }

    public enum RenderMode {
        Move,
        Fade
    }
}
