package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.CompatConfig;
import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelNpcMixin<T extends LivingEntity> {

    @Unique
    private boolean ac$restore;

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void ancientcombat$mark(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ac$restore = CompatConfig.ENABLE_EMF_NPC_COMPAT.get()
                && entity instanceof NpcBase
                && FACompatHandler.shouldKeepCombatPose(entity);
    }

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ancientcombat$capture(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!ac$restore) return;
        FACompatHandler.captureHumanoidCombatParts((HumanoidModel<?>) (Object) this);
    }
}