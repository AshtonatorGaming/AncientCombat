package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.NpcClientAttacker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Players tick their Better Combat poses from {@code Player.tick}. NPCs do the same when this mod
 * owns the stack. Skipped when Better Mob Combat is loaded, because that mod already ticks mobs.
 */
@Mixin(LivingEntity.class)
public abstract class NpcPlayerTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ancientcombat$npcTick(CallbackInfo ci) {
        if ((Object) this instanceof NpcClientAttacker attacker) {
            attacker.ancientcombat$clientTick();
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("TAIL"))
    private void ancientcombat$npcSwing(InteractionHand hand, CallbackInfo ci) {
        if ((Object) this instanceof NpcClientAttacker attacker) {
            attacker.ancientcombat$clientSwing(hand);
        }
    }
}
