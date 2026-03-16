package bodevelopment.client.blackout.mixin.accessors;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface AccessorEntityRenderer<T extends Entity, S extends EntityRenderState> {
    @Invoker("renderNameTag")
    void invokeRenderLabelIfPresent(S state, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light);
}