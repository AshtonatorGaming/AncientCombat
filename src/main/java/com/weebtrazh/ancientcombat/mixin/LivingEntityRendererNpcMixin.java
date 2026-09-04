package com.weebtrazh.ancientcombat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.weebtrazh.ancientcombat.CompatConfig;
import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNpcMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    protected M model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ancientcombat$afterSetup(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return;
        if (!(entity instanceof NpcBase)) return;
        if (!FACompatHandler.shouldKeepCombatPose(entity)) {
            FACompatHandler.resumeEmfFor(entity);
            return;
        }
        if (!(model instanceof HumanoidModel<?> human)) return;
        FACompatHandler.pauseEmfFor(entity);
        FACompatHandler.restoreHumanoidCombatParts(human);
    }
}