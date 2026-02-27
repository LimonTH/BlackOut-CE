package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.functional.DoubleConsumer;
import bodevelopment.client.blackout.interfaces.functional.DoubleFunction;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.BowSpam;
import bodevelopment.client.blackout.module.modules.visual.entities.Trails;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Trajectories extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Integer> maxTicks = this.sgGeneral.intSetting("Simulation Depth", 500, 0, 500, 5, "The maximum number of physics steps to calculate for the projected path.");
    private final Setting<Boolean> playerVelocity = this.sgGeneral.booleanSetting("Inertia Compensation", true, "Includes the player's current movement velocity in the initial projectile calculation.");

    public final Setting<Trails.ColorMode> colorMode = this.sgColor.enumSetting("Color Logic", Trails.ColorMode.Custom, "The algorithmic style used to calculate the trajectory line colors.");
    private final Setting<Double> saturation = this.sgColor.doubleSetting("Rainbow Saturation", 0.8, 0.0, 1.0, 0.1, "The color richness of the rainbow cycle.", () -> this.colorMode.get() == Trails.ColorMode.Rainbow);
    private final Setting<BlackOutColor> clr = this.sgColor.colorSetting("Primary Color", new BlackOutColor(255, 255, 255, 255), "The main color of the trajectory path.", () -> this.colorMode.get() != Trails.ColorMode.Rainbow);
    private final Setting<BlackOutColor> clr1 = this.sgColor.colorSetting("Wave Secondary", new BlackOutColor(175, 175, 175, 255), "The secondary color used for wave interpolation.", () -> this.colorMode.get() != Trails.ColorMode.Rainbow);
    private final Setting<Double> fadeLength = this.sgColor.doubleSetting("Start Fade Distance", 1.0, 0.0, 10.0, 0.1, "The distance from the player where the trajectory line begins to fade into full opacity.");

    private final Map<Item, SimulationData> dataMap = new HashMap<>();

    public Trajectories() {
        super("Trajectories", "Predicts and renders the flight path of projectiles, accounting for gravity, drag, and initial velocity.", SubCategory.MISC_VISUAL, true);
        this.initMap();
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            ItemStack itemStack = BlackOut.mc.player.getMainHandStack();
            Item item = itemStack.getItem();
            if (this.dataMap.containsKey(item)) {
                SimulationData data = this.dataMap.get(item);
                MatrixStack stack = Render3DUtils.matrices;
                stack.push();
                Render3DUtils.setRotation(stack);
                Render3DUtils.start();
                float yaw = Managers.ROTATION.getNextYaw();
                this.draw(data, this.getVelocity(data.speed.apply(itemStack), yaw, 0.0), itemStack, event.tickDelta, stack);
                if (this.hasMulti(itemStack)) {
                    this.draw(data, this.getVelocity(data.speed.apply(itemStack), yaw, -10.0), itemStack, event.tickDelta, stack);
                    this.draw(data, this.getVelocity(data.speed.apply(itemStack), yaw, 10.0), itemStack, event.tickDelta, stack);
                }

                Render3DUtils.end();
                stack.pop();
            }
        }
    }

    private void rotateVelocity(double[] velocity, Vec3d opposite, double yaw) {
        Quaternionf quaternionf = new Quaternionf().setAngleAxis(yaw * (float) (Math.PI / 180.0), opposite.x, opposite.y, opposite.z);
        Vec3d velocityVec = new Vec3d(velocity[0], velocity[1], velocity[2]);
        Vector3f vector3f = velocityVec.toVector3f().rotate(quaternionf);
        velocity[0] = vector3f.x;
        velocity[1] = vector3f.y;
        velocity[2] = vector3f.z;
    }

    private boolean hasMulti(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof CrossbowItem)) {
            return false;
        }

        var enchantmentRegistry = BlackOut.mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var multishotEntry = enchantmentRegistry.getEntry(Enchantments.MULTISHOT);

        return multishotEntry.map(entry -> EnchantmentHelper.getLevel(entry, itemStack) > 0).orElse(false);
    }

    private void draw(SimulationData data, double[] velocity, ItemStack itemStack, float tickDelta, MatrixStack stack) {
        HitResult hitResult = this.drawLine(data, velocity, itemStack, tickDelta, stack);

        if (hitResult != null) {
            Color color = this.getColor();
            int rgb = color.getRGB();
            double radius = 0.25;

            if (hitResult instanceof BlockHitResult blockHitResult) {
                Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
                Vec3d pos = blockHitResult.getPos().subtract(camPos);

                Render3DUtils.Orientation orientation = switch (blockHitResult.getSide()) {
                    case DOWN, UP -> Render3DUtils.Orientation.XZ;
                    case NORTH, SOUTH -> Render3DUtils.Orientation.XY;
                    case WEST, EAST -> Render3DUtils.Orientation.YZ;
                };

                Render3DUtils.circle(stack, pos, radius, rgb, 360, orientation);

                int fillCol = (color.getAlpha() / 4 << 24) | (rgb & 0x00FFFFFF);
                Render3DUtils.fillCircle(stack, pos, radius, fillCol, 360, orientation);

            } else if (hitResult instanceof EntityHitResult entityHitResult) {
                Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
                Box box = OLEPOSSUtils.getLerpedBox(entityHitResult.getEntity(), tickDelta)
                        .offset(-camPos.x, -camPos.y, -camPos.z);

                Render3DUtils.renderOutlines(stack, box, rgb);
            }
        }
    }

    private HitResult drawLine(SimulationData data, double[] velocity, ItemStack itemStack, float tickDelta, MatrixStack stack) {
        Vec3d pos = data.startPos.apply(itemStack, tickDelta);
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        MutableDouble dist = new MutableDouble(0.0);
        this.vertex(bufferBuilder, matrix4f, pos, pos, dist);
        Box box = this.getBox(pos, data);

        for (int i = 0; i < this.maxTicks.get(); i++) {
            Vec3d prevPos = pos;
            pos = pos.add(velocity[0], velocity[1], velocity[2]);
            ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player);
            HitResult blockHitResult = DamageUtils.raycast(DamageUtils.raycastContext, false);
            EntityHitResult entityHitResult = ProjectileUtil.getEntityCollision(
                    BlackOut.mc.world,
                    BlackOut.mc.player,
                    prevPos,
                    pos,
                    box.stretch(velocity[0], velocity[1], velocity[2]).expand(1.0),
                    entity -> entity != BlackOut.mc.player && this.canHit(entity),
                    0.3F
            );
            boolean blockValid = blockHitResult.getType() != HitResult.Type.MISS;
            boolean entityValid = entityHitResult != null && entityHitResult.getType() == HitResult.Type.ENTITY;
            HitResult hitResult;
            if (blockValid && entityValid) {
                if (prevPos.distanceTo(entityHitResult.getPos()) < prevPos.distanceTo(blockHitResult.getPos())) {
                    hitResult = entityHitResult;
                } else {
                    hitResult = blockHitResult;
                }
            } else if (blockValid) {
                hitResult = blockHitResult;
            } else if (entityValid) {
                hitResult = entityHitResult;
            } else {
                hitResult = null;
            }

            if (hitResult != null) {
                this.vertex(bufferBuilder, matrix4f, hitResult.getPos(), prevPos, dist);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                return hitResult;
            }

            data.physics.accept(box, velocity);
            box = this.getBox(pos, data);
            this.vertex(bufferBuilder, matrix4f, pos, prevPos, dist);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        return null;
    }

    private void vertex(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3d pos, Vec3d prevPos, MutableDouble dist) {
        DoubleConsumer<Vec3d, Double> consumer = (vec, d) -> {
            Color color = this.withAlpha(this.getColor(), this.getAlpha(d));
            Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
            bufferBuilder.vertex(
                            matrix4f, (float) (vec.x - camPos.x), (float) (vec.y - camPos.y), (float) (vec.z - camPos.z)
                    )
                    .color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F)
                    ;
        };
        double totalDist = prevPos.distanceTo(pos);
        if (dist.getValue() <= this.fadeLength.get()) {
            for (double i = 1.0; i < 30.0; i++) {
                double delta = i / 30.0;
                consumer.accept(prevPos.lerp(pos, delta), dist.getValue() + i / 30.0 * totalDist);
            }
        } else {
            consumer.accept(pos, dist.getValue());
        }

        dist.add(totalDist);
    }

    private Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F * alpha);
    }

    private float getAlpha(double dist) {
        return (float) Math.min(dist / this.fadeLength.get(), 1.0);
    }

    private Box getBox(Vec3d pos, SimulationData data) {
        return new Box(
                pos.x - data.width / 2.0,
                pos.y,
                pos.z - data.width / 2.0,
                pos.x + data.width / 2.0,
                pos.y + data.height,
                pos.z + data.width / 2.0
        );
    }

    private boolean canHit(Entity entity) {
        return entity.canBeHitByProjectile() && !BlackOut.mc.player.isConnectedThroughVehicle(entity);
    }

    private Color getColor() {
        return switch (this.colorMode.get()) {
            case Custom -> this.clr.get().getColor();
            case Rainbow -> {
                int rainbowColor = ColorUtils.getRainbow(4.0F, this.saturation.get().floatValue(), 1.0F, 150L);
                yield new Color(rainbowColor >> 16 & 0xFF, rainbowColor >> 8 & 0xFF, rainbowColor & 0xFF, this.clr.get().alpha);
            }
            case Wave -> ColorUtils.getWave(this.clr.get().getColor(), this.clr1.get().getColor(), 1.0, 1.0, 1);
        };
    }

    private double[] getVelocity(double[] d, float yaw, double simulation) {
        double[] velocity = new double[]{
                -MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(Managers.ROTATION.getNextPitch() * (float) (Math.PI / 180.0)),
                -MathHelper.sin((Managers.ROTATION.getNextPitch() + (float) d[1]) * (float) (Math.PI / 180.0)),
                MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(Managers.ROTATION.getNextPitch() * (float) (Math.PI / 180.0))
        };
        if (simulation != 0.0) {
            this.rotateVelocity(velocity, RotationUtils.rotationVec(yaw, Managers.ROTATION.getNextPitch() - 90.0F, 1.0), simulation);
        }

        velocity[0] *= d[0];
        velocity[1] *= d[0];
        velocity[2] *= d[0];
        if (this.playerVelocity.get()) {
            velocity[0] += BlackOut.mc.player.getVelocity().x;
            if (!BlackOut.mc.player.isOnGround()) {
                velocity[1] += BlackOut.mc.player.getVelocity().y;
            }

            velocity[2] += BlackOut.mc.player.getVelocity().z;
        }

        return velocity;
    }

    private void initMap() {
        double[] snowball = new double[]{1.5, 0.0};
        double[] exp = new double[]{0.7, -20.0};
        this.put(
                0.25,
                0.25,
                (stack, tickDelta) -> OLEPOSSUtils.getLerpedPos(BlackOut.mc.player, tickDelta)
                        .add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()) - 0.1, 0.0),
                stack -> snowball,
                (box, vel) -> {
                    double f = OLEPOSSUtils.inWater(box) ? 0.8 : 0.99;
                    vel[0] *= f;
                    vel[1] *= f;
                    vel[2] *= f;
                    vel[1] -= 0.03;
                },
                Items.SNOWBALL,
                Items.EGG
        );
        this.put(
                0.5,
                0.5,
                (stack, tickDelta) -> OLEPOSSUtils.getLerpedPos(BlackOut.mc.player, tickDelta)
                        .add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()) - 0.1, 0.0),
                stack -> {
                    BowSpam bowSpam = BowSpam.getInstance();
                    int i;
                    if (bowSpam.enabled && BlackOut.mc.options.useKey.isPressed()) {
                        i = bowSpam.charge.get();
                    } else {
                        i = stack.getMaxUseTime(BlackOut.mc.player) - BlackOut.mc.player.getItemUseTimeLeft();
                    }

                    float f = Math.max(BowItem.getPullProgress(i), 0.1F);
                    return new double[]{f * 3.0, 0.0};
                },
                (box, vel) -> {
                    double f = OLEPOSSUtils.inWater(box) ? 0.6 : 0.99;
                    vel[0] *= f;
                    vel[1] *= f;
                    vel[2] *= f;
                    vel[1] -= 0.05;
                },
                Items.BOW
        );
        this.put(
                0.5,
                0.5,
                (stack, tickDelta) -> OLEPOSSUtils.getLerpedPos(BlackOut.mc.player, tickDelta)
                        .add(
                                0.0,
                                BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()) - (isChargedWith(stack, Items.FIREWORK_ROCKET) ? 0.15 : 0.1),
                                0.0
                        ),
                stack -> new double[]{isChargedWith(stack, Items.FIREWORK_ROCKET) ? 1.6 : 3.15, 0.0},
                (box, vel) -> {
                    if (!isChargedWith(BlackOut.mc.player.getMainHandStack(), Items.FIREWORK_ROCKET)) {
                        double f = OLEPOSSUtils.inWater(box) ? 0.6 : 0.99;
                        vel[0] *= f;
                        vel[1] *= f;
                        vel[2] *= f;
                        vel[1] -= 0.05;
                    }
                },
                Items.CROSSBOW
        );
        this.put(
                0.25,
                0.25,
                (stack, tickDelta) -> OLEPOSSUtils.getLerpedPos(BlackOut.mc.player, tickDelta)
                        .add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()) - 0.1, 0.0),
                stack -> exp,
                (box, vel) -> {
                    double f = OLEPOSSUtils.inWater(box) ? 0.8 : 0.99;
                    vel[0] *= f;
                    vel[1] *= f;
                    vel[2] *= f;
                    vel[1] -= 0.07;
                },
                Items.EXPERIENCE_BOTTLE
        );
        this.put(
                0.25,
                0.25,
                (stack, tickDelta) -> OLEPOSSUtils.getLerpedPos(BlackOut.mc.player, tickDelta)
                        .add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()) - 0.1, 0.0),
                stack -> snowball,
                (box, vel) -> {
                    double f = OLEPOSSUtils.inWater(box) ? 0.8 : 0.99;
                    vel[0] *= f;
                    vel[1] *= f;
                    vel[2] *= f;
                    vel[1] -= 0.03;
                },
                Items.ENDER_PEARL
        );
    }

    private void put(
            double width,
            double height,
            DoubleFunction<ItemStack, Float, Vec3d> startPos,
            Function<ItemStack, double[]> speed,
            DoubleConsumer<Box, double[]> physics,
            Item... items
    ) {
        for (Item item : items) {
            this.dataMap.put(item, new SimulationData(width, height, startPos, speed, physics));
        }
    }

    private record SimulationData(
            double width,
            double height,
            DoubleFunction<ItemStack, Float, Vec3d> startPos,
            Function<ItemStack, double[]> speed,
            DoubleConsumer<Box, double[]> physics
    ) {}

    private boolean isChargedWith(ItemStack crossbowStack, Item projectileItem) {
        if (!(crossbowStack.getItem() instanceof CrossbowItem)) return false;
        ChargedProjectilesComponent charged = crossbowStack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (charged == null || charged.isEmpty()) return false;

        for (ItemStack projectile : charged.getProjectiles()) {
            if (projectile.isOf(projectileItem)) {
                return true;
            }
        }

        return false;
    }
}
