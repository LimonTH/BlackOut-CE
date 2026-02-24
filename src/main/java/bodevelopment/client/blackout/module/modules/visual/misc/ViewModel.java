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
    public final Setting<Double> fov = this.sgGeneral.doubleSetting("Hand FOV", 70.0, 10.0, 170.0, 5.0, ".");
    private final SettingGroup sgMain = this.addGroup("Main");
    private final SettingGroup sgOff = this.addGroup("Off");
    private final Setting<Boolean> renderMain = this.sgMain.booleanSetting("Render Main", true, "");
    private final Setting<Double> mainX = this.sgMain.doubleSetting("Main X", 0.0, -1.0, 1.0, 0.02, "");
    private final Setting<Double> mainY = this.sgMain.doubleSetting("Main Y", 0.0, -1.0, 1.0, 0.02, "");
    private final Setting<Double> mainZ = this.sgMain.doubleSetting("Main Z", 0.0, -1.0, 1.0, 0.02, "");
    private final Setting<Double> mainScaleX = this.sgMain.doubleSetting("Main Scale X", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> mainScaleY = this.sgMain.doubleSetting("Main Scale Y", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> mainScaleZ = this.sgMain.doubleSetting("Main Scale Z", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> mainRotX = this.sgMain.doubleSetting("Main Rotation X", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> mainRotY = this.sgMain.doubleSetting("Main Rotation Y", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> mainRotZ = this.sgMain.doubleSetting("Main Rotation Z", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> mainRotSpeedX = this.sgMain.doubleSetting("Main Rotation Speed X", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> mainRotSpeedY = this.sgMain.doubleSetting("Main Rotation Speed Y", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> mainRotSpeedZ = this.sgMain.doubleSetting("Main Rotation Speed Z", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Boolean> renderOff = this.sgOff.booleanSetting("Render Off", true, "");
    private final Setting<Double> offX = this.sgOff.doubleSetting("Off X", 0.0, -1.0, 1.0, 0.02, "");
    private final Setting<Double> offY = this.sgOff.doubleSetting("Off Y", 0.0, -1.0, 1.0, 0.02, "");
    private final Setting<Double> offZ = this.sgOff.doubleSetting("Off Z", 0.0, -1.0, 1.0, 0.02, "");
    private final Setting<Double> offScaleX = this.sgOff.doubleSetting("Off Scale X", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> offScaleY = this.sgOff.doubleSetting("Off Scale Y", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> offScaleZ = this.sgOff.doubleSetting("Off Scale Z", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> offRotX = this.sgOff.doubleSetting("Off Rotation X", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> offRotY = this.sgOff.doubleSetting("Off Rotation Y", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> offRotZ = this.sgOff.doubleSetting("Off Rotation Z", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> offRotSpeedX = this.sgOff.doubleSetting("Off Rotation Speed X", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> offRotSpeedY = this.sgOff.doubleSetting("Off Rotation Speed Y", 0.0, -180.0, 180.0, 5.0, "");
    private final Setting<Double> offRotSpeedZ = this.sgOff.doubleSetting("Off Rotation Speed Z", 0.0, -180.0, 180.0, 5.0, "");
    private float mainRotationX = 0.0F;
    private float mainRotationY = 0.0F;
    private float mainRotationZ = 0.0F;
    private long mainTime = 0L;
    private float offRotationX = 0.0F;
    private float offRotationY = 0.0F;
    private float offRotationZ = 0.0F;
    private long offTime = 0L;

    public ViewModel() {
        super("View Model", "Modifies where hands and held items are rendered.", SubCategory.MISC_VISUAL, false);
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
