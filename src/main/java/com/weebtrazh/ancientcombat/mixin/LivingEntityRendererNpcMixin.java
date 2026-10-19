package com.weebtrazh.ancientcombat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNpcMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "render", at = @At("HEAD"))
    private void ancientcombat$npcRenderStart(T entity, float yaw, float pt, PoseStack pose,
                                              MultiBufferSource buf, int light, CallbackInfo ci) {
        if (entity instanceof NpcBase) {
            FACompatHandler.setEmfNpc(entity);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ancientcombat$npcRenderEnd(T entity, float yaw, float pt, PoseStack pose,
                                            MultiBufferSource buf, int light, CallbackInfo ci) {
        if (entity instanceof NpcBase) {
            FACompatHandler.setEmfNpc(null);
        }
    }
}