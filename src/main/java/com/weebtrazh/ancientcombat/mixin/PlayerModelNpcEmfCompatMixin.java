package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}