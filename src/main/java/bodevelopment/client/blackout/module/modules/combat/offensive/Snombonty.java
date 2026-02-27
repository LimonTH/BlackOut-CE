package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.MoveUpdateModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ExtrapolationMap;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.Rotation;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.ProjectileUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class Snombonty extends MoveUpdateModule {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Boolean> playerVelocity = this.sgGeneral.booleanSetting("Inertia Compensation", true, "Incorporates your current movement velocity into the projectile trajectory math.");
    private final Setting<Boolean> onlyPlayers = this.sgGeneral.booleanSetting("Players Only", true, "Restricts automated targeting to player entities.");
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Maximum Range", 50.0, 0.0, 100.0, 1.0, "The maximum distance allowed to acquire a target.");
    private final Setting<Double> throwSpeed = this.sgGeneral.doubleSetting("Fire Rate", 20.0, 0.0, 20.0, 0.1, "The number of projectiles to launch per second.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Normal, "The method used to swap to snowballs or eggs in the hotbar.");
    private final Setting<Boolean> extrapolation = this.sgGeneral.booleanSetting("Target Prediction", true, "Calculates the target's future position based on their current velocity.");
    private final Setting<Double> extrapolationStrength = this.sgGeneral.doubleSetting("Prediction Intensity", 1.0, 0.0, 1.0, 0.01, "The weight applied to the target's movement prediction.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Aim", true, "Bypasses rotation speed limits to snap immediately to targets.");

    private final Setting<Boolean> renderSwing = this.sgRender.booleanSetting("Swing Animation", false, "Visualizes the arm swing for each projectile thrown.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Arm", SwingHand.RealHand, "The arm used for the throw animation.");
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Target Highlight Shape", RenderShape.Full, "The geometry used to highlight the current target.");
    private final Setting<BlackOutColor> lineColor = this.sgRender.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the target highlight edges.");
    private final Setting<BlackOutColor> sideColor = this.sgRender.colorSetting("Fill Color", new BlackOutColor(255, 0, 0, 50), "The color of the target highlight faces.");
    private final Setting<Boolean> renderSpread = this.sgRender.booleanSetting("Visual Spread Circle", true, "Displays a circle representing the potential projectile spread on the target.");
    private final Setting<BlackOutColor> spreadColor = this.sgRender.colorSetting("Spread Circle Color", new BlackOutColor(255, 255, 255, 255), "The color of the visual spread circle.", this.renderSpread::get);

    private final MatrixStack stack = new MatrixStack();
    private final ExtrapolationMap extMap = new ExtrapolationMap();
    private final Predicate<ItemStack> predicate = stack -> stack.isOf(Items.SNOWBALL) || stack.isOf(Items.EGG);
    private final Consumer<double[]> snowballVelocity = vel -> {
        vel[0] *= 0.99;
        vel[1] *= 0.99;
        vel[2] *= 0.99;
        vel[1] -= 0.03;
    };
    private Entity target = null;
    private Box targetBox = null;
    private Box prevBox = null;
    private double yaw = 0.0;
    private double pitch = 0.0;
    private double throwsLeft = 0.0;
    private int balls = 0;
    private FindResult result = null;
    private boolean switched = false;

    public Snombonty() {
        super("Snombonty", "Rapidly spams snowballs or eggs at entities using advanced trajectory prediction.", SubCategory.OFFENSIVE);
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.throwsLeft = 0;
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.extMap.clear();
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.target != null) {
            Render3DUtils.box(this.lerpBox(event.tickDelta, this.prevBox, this.targetBox), this.sideColor.get(), this.lineColor.get(), this.renderShape.get());
            if (this.renderSpread.get()) {
                this.renderSpread(event.tickDelta);
            }
        }
    }

    private void renderSpread(float tickDelta) {
        Vec3d cameraPos = BlackOut.mc.gameRenderer.getCamera().getPos();

        double x = MathHelper.lerp(tickDelta, (this.prevBox.minX + this.prevBox.maxX) / 2.0, (this.targetBox.minX + this.targetBox.maxX) / 2.0) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, this.prevBox.minY, this.targetBox.minY) - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, (this.prevBox.minZ + this.prevBox.maxZ) / 2.0, (this.targetBox.minZ + this.targetBox.maxZ) / 2.0) - cameraPos.z;

        this.stack.push();
        Render3DUtils.setRotation(this.stack);

        BlackOutColor color = this.spreadColor.get();

        Vec3d targetPos = new Vec3d(x, y, z);
        float radius = (float) (new Vec3d(x, y, z).length() * 0.0174);

        Render3DUtils.circle(this.stack, targetPos, radius, color.getRGB(), 360, Render3DUtils.Orientation.XZ);
        this.stack.pop();
    }

    private Box lerpBox(float tickDelta, Box prev, Box current) {
        return new Box(
                MathHelper.lerp(tickDelta, prev.minX, current.minX),
                MathHelper.lerp(tickDelta, prev.minY, current.minY),
                MathHelper.lerp(tickDelta, prev.minZ, current.minZ),
                MathHelper.lerp(tickDelta, prev.maxX, current.maxX),
                MathHelper.lerp(tickDelta, prev.maxY, current.maxY),
                MathHelper.lerp(tickDelta, prev.maxZ, current.maxZ)
        );
    }

    @Override
    protected void update(boolean allowAction, boolean fakePos) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.extrapolation.get()) {
                this.extMap
                        .update(
                                player -> (int) Math.floor(BlackOut.mc.player.getPos().distanceTo(player.getPos()) / 5.0 * this.extrapolationStrength.get())
                        );
            } else {
                this.extMap.clear();
            }

            this.findTarget();
            if (this.target != null) {
                this.prevBox = this.targetBox;
                this.targetBox = this.target instanceof AbstractClientPlayerEntity pl && this.extMap.contains(pl) ? this.extMap.get(pl) : this.target.getBoundingBox();
                if (this.prevBox == null) {
                    this.prevBox = this.targetBox;
                }

                if (BoxUtils.middle(this.prevBox).distanceTo(BoxUtils.middle(this.targetBox)) > 5.0) {
                    this.prevBox = this.targetBox;
                }

                this.update(allowAction);
            }
        }
    }

    private void update(boolean allowAction) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) {
            this.target = null;
            this.targetBox = null;
            return;
        }
        this.throwsLeft = this.throwsLeft + this.throwSpeed.get() / 20.0;
        this.result = this.switchMode.get().find(this.predicate);
        this.throwUpdate(allowAction);
        this.throwsLeft = Math.min(this.throwsLeft, 1.0);
    }

    private void throwUpdate(boolean allowAction) {
        if (!BlackOut.mc.player.isAlive()) return;

        Hand hand = OLEPOSSUtils.getHand(this.predicate);
        if (hand != null || (this.result != null && this.result.wasFound())) {
            if (this.rotate((float) this.yaw, (float) this.pitch, 0.0, 10.0, RotationType.Other.withInstant(this.instantRotate.get()), "throwing")) {
                if (allowAction) {

                    if (hand != null) {
                        this.balls = this.getBalls(hand);
                    } else if (this.result.wasFound()) {
                        this.balls = Math.min((int) Math.floor(this.throwsLeft), this.result.amount());
                    }

                    int maxIterations = 64;
                    while (this.balls > 0 && maxIterations > 0) {
                        this.throwSnowBall(hand);
                        this.balls--;
                        this.throwsLeft--;
                        maxIterations--;
                    }

                    if (this.switched) {
                        this.switchMode.get().swapBack();
                        this.switched = false;
                    }
                }
            }
        }
    }

    private void throwSnowBall(Hand hand) {
        if (hand != null || (this.switched = this.switchMode.get().swap(this.result.slot()))) {
            this.useItem(hand);
            if (this.renderSwing.get()) {
                this.clientSwing(this.swingHand.get(), hand);
            }
        }
    }

    private int getBalls(Hand hand) {
        return Math.min(
                (int) Math.floor(this.throwsLeft),
                hand == Hand.MAIN_HAND
                        ? Managers.PACKET.getStack().getCount()
                        : (hand == Hand.OFF_HAND ? BlackOut.mc.player.getOffHandStack().getCount() : 0)
        );
    }

    private void findTarget() {
        this.target = null;
        double dist = 10000.0;
        double maxRange = this.range.get();

        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity == BlackOut.mc.player || !(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            if (this.onlyPlayers.get() && !(entity instanceof PlayerEntity)) continue;

            double d = BlackOut.mc.player.getPos().distanceTo(entity.getPos());
            if (maxRange > 0.0 && d > maxRange) continue;

            if (d < dist) {
                Box box = entity instanceof AbstractClientPlayerEntity pl && this.extMap.contains(pl)
                        ? this.extMap.get(pl)
                        : entity.getBoundingBox();

                Rotation rotation = ProjectileUtils.calcShootingRotation(
                        BlackOut.mc.player.getEyePos(),
                        BoxUtils.middle(box),
                        1.5,
                        this.playerVelocity.get(),
                        this.snowballVelocity
                );

                if (rotation.pitch() != 0.0F && rotation.pitch() >= -85.0F) {
                    this.yaw = rotation.yaw();
                    this.pitch = rotation.pitch();
                    this.target = entity;
                    dist = d;
                }
            }
        }
    }
}
