package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Priority 2502 so the RETURN inject runs after Player Animator (2000) and after
 * EMF Compat's player capture (2500). The arm pose on the model at RETURN is the
 * Better Combat swing or hold. NPCs are not {@code AbstractClientPlayer}, so the
 * addon never snapshots them.
 */
@Mixin(value = PlayerModel.class, priority = 2502)
public abstract class PlayerModelNpcEmfCompatMixin {

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void ancientcombat$markNpc(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                       float ageInTicks, float netHeadYaw, float headPitch,
                                       CallbackInfo ci) {
        if (entity instanceof NpcBase) {
            FACompatHandler.setEmfNpc(entity);
        } else {
            FACompatHandler.setEmfNpc(null);
        }
    }

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ancientcombat$captureNpcPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                              float ageInTicks, float netHeadYaw, float headPitch,
                                              CallbackInfo ci) {
        if (entity instanceof NpcBase npc) {
            FACompatHandler.captureNpcPose((PlayerModel<?>) (Object) this, npc, limbSwingAmount);
        }
    }
}