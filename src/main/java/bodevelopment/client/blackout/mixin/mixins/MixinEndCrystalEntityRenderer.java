package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.entities.CrystalChams;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Random;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer {
    @Unique
    private final Random random = new Random();
    @Shadow
    @Final
    private ModelPart bottom;
    @Unique
    private long spawnTime = 0L;
    @Unique
    private EndCrystalEntity entity = null;
    @Unique
    private float tickDelta = 0.0F;
    @Unique
    private int id;
    @Unique
    private long seed = 0L;

    @Inject(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preRender(EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (!Managers.ENTITY.shouldRender(endCrystalEntity.getId())) {
            ci.cancel();
        }

        if (CrystalChams.getInstance().enabled) {
            this.id = 3;
            this.tickDelta = g;
            this.entity = endCrystalEntity;
            this.spawnTime = ((IEndCrystalEntity) endCrystalEntity).blackout_Client$getSpawnTime();
            this.seed = (long) (
                    endCrystalEntity.getPos().x * 1000.0
                            + endCrystalEntity.getPos().y * 1000.0
                            + endCrystalEntity.getPos().z * 1000.0
            );
        }
    }

    @ModifyArgs(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", ordinal = 0)
    )
    private void setSize(Args args) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        if (crystalChams.enabled) {
            float scale;
            if (crystalChams.spawnAnimation.get()) {
                float animTime = crystalChams.animationTime.get().floatValue() * 1000.0F;
                scale = MathHelper.clampedLerp(
                        0.0F, crystalChams.scale.get().floatValue() * 2.0F, Math.min((float) (System.currentTimeMillis() - this.spawnTime), animTime) / animTime
                );
            } else {
                scale = crystalChams.scale.get().floatValue() * 2.0F;
            }

            this.setSeed();
            args.set(0, scale);
            args.set(1, scale);
            args.set(2, scale);
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0)
    )
    private void yOffset(MatrixStack instance, float x, float y, float z) {
        if (!CrystalChams.getInstance().enabled) {
            instance.translate(x, y, z);
        }
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 1),
            index = 1
    )
    private float getBounce(float x) {
        return CrystalChams.getInstance().enabled ? this.getBounce(this.entity, this.tickDelta) : x;
    }

    @Redirect(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"
            )
    )
    private void renderPart(ModelPart instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        if (crystalChams.enabled && instance != this.bottom) {
            crystalChams.renderBox(matrices, --this.id);
        } else {
            instance.render(matrices, vertices, light, overlay);
        }
    }

    @Unique
    private float getBounce(EndCrystalEntity crystal, float tickDelta) {
        this.setSeed();
        CrystalChams crystalChams = CrystalChams.getInstance();
        float r = crystalChams.enabled && crystalChams.bounceSync.get() ? (float) (this.random.nextFloat() * 2.0F * Math.PI) : 0.0F;
        float f = (crystalChams.enabled && crystalChams.bounceSync.get() ? crystalChams.age : crystal.endCrystalAge) + tickDelta;
        float g = MathHelper.sin(f * 0.2F * crystalChams.bounceSpeed.get().floatValue() + r) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F;
        return (float) (crystalChams.y.get() + 0.5 + g * crystalChams.bounce.get()) / 2.0F;
    }

    @Redirect(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/decoration/EndCrystalEntity;endCrystalAge:I", ordinal = 0, opcode = 180)
    )
    private int getAge(EndCrystalEntity instance) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        if (crystalChams.enabled && crystalChams.rotationSync.get()) {
            this.setSeed();
            return crystalChams.age + this.random.nextInt(100);
        } else {
            return instance.endCrystalAge;
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;")
    )
    private Quaternionf rotationSpeed(RotationAxis instance, float deg) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        return instance.rotationDegrees(deg * (crystalChams.enabled ? crystalChams.rotationSpeed.get().floatValue() : 1.0F));
    }

    @Unique
    private void setSeed() {
        this.random.setSeed(this.seed);
    }
}
