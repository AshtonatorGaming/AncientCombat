package com.weebtrazh.ancientcombat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.weebtrazh.ancientcombat.CompatConfig;
import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.render.RenderNpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderNpcBase.class)
public abstract class RenderNpcBaseMixin {

    @Inject(
            method = "render(Lnet/shadowmage/ancientwarfare/npc/entity/NpcBase;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            remap = false
    )
    private void ancientcombat$beforeNpcRender(NpcBase npc, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return;
        if (!FACompatHandler.shouldKeepCombatPose(npc)) return;
        EntityModel<?> model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
        if (model instanceof PlayerModel<?> player) {
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.jacket.copyFrom(player.body);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        } else if (model instanceof HumanoidModel<?> human) {
            FACompatHandler.restoreHumanoidCombatParts(human);
        }
    }
}