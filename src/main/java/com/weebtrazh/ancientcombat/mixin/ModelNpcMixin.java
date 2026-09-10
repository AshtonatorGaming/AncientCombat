package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.render.ModelNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelNpc.class)
public abstract class ModelNpcMixin {

    @Inject(
            method = "m_6973_",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/PlayerModel;m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"
            ),
            remap = false
    )
    private void ancientcombat$zeroBeforeSuper(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                               float ageInTicks, float netHeadYaw, float headPitch,
                                               CallbackInfo ci) {
        if (!(entity instanceof NpcBase)) return;
        if (!FACompatHandler.hasCombatAttributes(entity.getMainHandItem())) return;
        ((PlayerModel<?>) (Object) this).attackTime = 0.0F;
    }
}