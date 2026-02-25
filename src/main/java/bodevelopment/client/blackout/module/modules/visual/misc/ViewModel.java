package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class ViewModel extends Module {
    private static ViewModel INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgMain = this.addGroup("Main Hand");
    private final SettingGroup sgOff = this.addGroup("Off Hand");

    public final Setting<Double> fov = this.sgGeneral.doubleSetting("Focal Length", 70.0, 10.0, 170.0, 5.0, "The field of view specifically applied to the first-person hand and item rendering.");

    private final Setting<Boolean> renderMain = this.sgMain.booleanSetting("Show Main", true, "Whether to render the main-hand model.");
    private final Setting<Double> mainX = this.sgMain.doubleSetting("Offset X", 0.0, -1.0, 1.0, 0.02, "Lateral translation of the main hand.");
    private final Setting<Double> mainY = this.sgMain.doubleSetting("Offset Y", 0.0, -1.0, 1.0, 0.02, "Vertical translation of the main hand.");
    private final Setting<Double> mainZ = this.sgMain.doubleSetting("Offset Z", 0.0, -1.0, 1.0, 0.02, "Depth translation of the main hand.");
    private final Setting<Double> mainScaleX = this.sgMain.doubleSetting("Scale X", 1.0, 0.0, 2.0, 0.02, "Horizontal scale multiplier.");
    private final Setting<Double> mainScaleY = this.sgMain.doubleSetting("Scale Y", 1.0, 0.0, 2.0, 0.02, "Vertical scale multiplier.");
    private final Setting<Double> mainScaleZ = this.sgMain.doubleSetting("Scale Z", 1.0, 0.0, 2.0, 0.02, "Depth scale multiplier.");
    private final Setting<Double> mainRotX = this.sgMain.doubleSetting("Pitch Rotation", 0.0, -180.0, 180.0, 5.0, "Static X-axis rotation.");
    private final Setting<Double> mainRotY = this.sgMain.doubleSetting("Yaw Rotation", 0.0, -180.0, 180.0, 5.0, "Static Y-axis rotation.");
    private final Setting<Double> mainRotZ = this.sgMain.doubleSetting("Roll Rotation", 0.0, -180.0, 180.0, 5.0, "Static Z-axis rotation.");
    private final Setting<Double> mainRotSpeedX = this.sgMain.doubleSetting("Pitch Velocity", 0.0, -180.0, 180.0, 5.0, "Continuous rotation speed around the X-axis.");
    private final Setting<Double> mainRotSpeedY = this.sgMain.doubleSetting("Yaw Velocity", 0.0, -180.0, 180.0, 5.0, "Continuous rotation speed around the Y-axis.");
    private final Setting<Double> mainRotSpeedZ = this.sgMain.doubleSetting("Roll Velocity", 0.0, -180.0, 180.0, 5.0, "Continuous rotation speed around the Z-axis.");

    private final Setting<Boolean> renderOff = this.sgOff.booleanSetting("Show Off", true, "Whether to render the off-hand model.");
    private final Setting<Double> offX = this.sgOff.doubleSetting("Offset X", 0.0, -1.0, 1.0, 0.02, "Lateral translation of the off hand.");
    private final Setting<Double> offY = this.sgOff.doubleSetting("Offset Y", 0.0, -1.0, 1.0, 0.02, "Vertical translation of the off hand.");
    private final Setting<Double> offZ = this.sgOff.doubleSetting("Offset Z", 0.0, -1.0, 1.0, 0.02, "Depth translation of the off hand.");
    private final Setting<Double> offScaleX = this.sgOff.doubleSetting("Scale X", 1.0, 0.0, 2.0, 0.02, "Horizontal scale multiplier.");
    private final Setting<Double> offScaleY = this.sgOff.doubleSetting("Scale Y", 1.0, 0.0, 2.0, 0.02, "Vertical scale multiplier.");
    private final Setting<Double> offScaleZ = this.sgOff.doubleSetting("Scale Z", 1.0, 0.0, 2.0, 0.02, "Depth scale multiplier.");
    private final Setting<Double> offRotX = this.sgOff.doubleSetting("Pitch Rotation", 0.0, -180.0, 180.0, 5.0, "Static X-axis rotation.");
    private final Setting<Double> offRotY = this.sgOff.doubleSetting("Yaw Rotation", 0.0, -180.0, 180.0, 5.0, "Static Y-axis rotation.");
    private final Setting<Double> offRotZ = this.sgOff.doubleSetting("Roll Rotation", 0.0, -180.0, 180.0, 5.0, "Static Z-axis rotation.");
    private final Setting<Double> offRotSpeedX = this.sgOff.doubleSetting("Pitch Velocity", 0.0, -180.0, 180.0, 5.0, "Continuous rotation speed around the X-axis.");
    private final Setting<Double> offRotSpeedY = this.sgOff.doubleSetting("Yaw Velocity", 0.0, -180.0, 180.0, 5.0, "Continuous rotation speed around the Y-axis.");
    private final Setting<Double> offRotSpeedZ = this.sgOff.doubleSetting("Roll Velocity", 0.0, -180.0, 180.0, 5.0, "Continuous rotation speed around the Z-axis.");

    private float mainRotationX = 0.0F;
    private float mainRotationY = 0.0F;
    private float mainRotationZ = 0.0F;
    private long mainTime = 0L;
    private float offRotationX = 0.0F;
    private float offRotationY = 0.0F;
    private float offRotationZ = 0.0F;
    private long offTime = 0L;

    public ViewModel() {
        super("View Model", "Grants precise control over the first-person hand and item camera transformations, including translation, scaling, and rotation.", SubCategory.MISC_VISUAL, false);
        INSTANCE = this;
    }

    public static ViewModel getInstance() {
        return INSTANCE;
    }

    public void transform(MatrixStack stack, Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            this.transform(stack, this.mainX, this.mainY, this.mainZ);
        } else {
            this.transform(stack, this.offX, this.offY, this.offZ);
        }
    }

    public boolean shouldCancel(Hand hand) {
        return hand == Hand.MAIN_HAND ? !this.renderMain.get() : !this.renderOff.get();
    }

    public void scaleAndRotate(MatrixStack stack, Hand hand) {
        stack.push();
        Setting<Double> scaleX;
        Setting<Double> scaleY;
        Setting<Double> scaleZ;
        Setting<Double> rotX;
        Setting<Double> rotY;
        Setting<Double> rotZ;
        float rotationX;
        float rotationY;
        float rotationZ;
        if (hand == Hand.MAIN_HAND) {
            scaleX = this.mainScaleX;
            scaleY = this.mainScaleY;
            scaleZ = this.mainScaleZ;
            rotX = this.mainRotX;
            rotY = this.mainRotY;
            rotZ = this.mainRotZ;
            double delta = (System.currentTimeMillis() - this.mainTime) / 250.0;
            this.mainTime = System.currentTimeMillis();
            if (this.mainRotSpeedX.get() == 0.0) {
                this.mainRotationX = 0.0F;
            } else {
                this.mainRotationX = (float) (this.mainRotationX + delta * this.mainRotSpeedX.get());
            }

            if (this.mainRotSpeedY.get() == 0.0) {
                this.mainRotationY = 0.0F;
            } else {
                this.mainRotationY = (float) (this.mainRotationY + delta * this.mainRotSpeedY.get());
            }

            if (this.mainRotSpeedZ.get() == 0.0) {
                this.mainRotationZ = 0.0F;
            } else {
                this.mainRotationZ = (float) (this.mainRotationZ + delta * this.mainRotSpeedZ.get());
            }

            rotationX = this.mainRotationX;
            rotationY = this.mainRotationY;
            rotationZ = this.mainRotationZ;
        } else {
            scaleX = this.offScaleX;
            scaleY = this.offScaleY;
            scaleZ = this.offScaleZ;
            rotX = this.offRotX;
            rotY = this.offRotY;
            rotZ = this.offRotZ;
            double deltax = (System.currentTimeMillis() - this.offTime) / 250.0;
            this.offTime = System.currentTimeMillis();
            if (this.offRotSpeedX.get() == 0.0) {
                this.offRotationX = 0.0F;
            } else {
                this.offRotationX = (float) (this.offRotationX + deltax * this.offRotSpeedX.get());
            }

            if (this.offRotSpeedY.get() == 0.0) {
                this.offRotationY = 0.0F;
            } else {
                this.offRotationY = (float) (this.offRotationY + deltax * this.offRotSpeedY.get());
            }

            if (this.offRotSpeedZ.get() == 0.0) {
                this.offRotationZ = 0.0F;
            } else {
                this.offRotationZ = (float) (this.offRotationZ + deltax * this.offRotSpeedZ.get());
            }

            rotationX = this.offRotationX;
            rotationY = this.offRotationY;
            rotationZ = this.offRotationZ;
        }

        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX.get().floatValue() + rotationX));
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY.get().floatValue() + rotationY));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ.get().floatValue() + rotationZ));
        stack.translate(-0.5, -0.5, -0.5);
        stack.push();
        stack.translate(0.5, 0.5, 0.5);
        stack.scale(scaleX.get().floatValue(), scaleY.get().floatValue(), scaleZ.get().floatValue());
    }

    public void post(MatrixStack stack) {
        stack.pop();
    }

    public void postRender(MatrixStack stack) {
        stack.pop();
        stack.pop();
    }

    private void transform(MatrixStack stack, Setting<Double> x, Setting<Double> y, Setting<Double> z) {
        stack.push();
        stack.translate(x.get(), y.get(), -z.get());
    }
}
