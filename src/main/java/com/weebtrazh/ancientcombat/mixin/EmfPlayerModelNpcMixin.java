package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.EmfCompatBridge;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EMF Compat's capture ignores anything that is not an {@code AbstractClientPlayer}. That check
 * returns before the cast, so it does not throw. Priority 2600 runs after the addon (2500) and
 * after {@link PlayerModelNpcAnimatorMixin} has put the player pose on the model. The snapshot
 * uses the addon's pose sources; the addon's own hook restores them.
 */
@Mixin(value = PlayerModel.class, priority = 2600)
public abstract class EmfPlayerModelNpcMixin {

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ancientcombat$captureNpc(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                           float ageInTicks, float netHeadYaw, float headPitch,
                                           CallbackInfo ci) {
        if (!(entity instanceof NpcBase npc)) return;
        EmfCompatBridge.capture((PlayerModel<?>) (Object) this, npc, limbSwingAmount);
    }
}
