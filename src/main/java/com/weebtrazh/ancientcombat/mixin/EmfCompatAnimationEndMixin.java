package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.AcNpcEmfHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import strm.emfcompat.core.EMFCompatAnimationHook;
import traben.entity_model_features.EMFAnimationApi;

/**
 * EMF Compat 2.3 restores saved poses inside {@code EMFCompatAnimationHook.onAnimationEnd}.
 * Zeroing arm children has to happen after that restore, or the idle clip on the child
 * mesh is what the player sees. Applied only when emf_compat_core is installed
 * ({@link AncientCombatMixinPlugin}).
 */
@Mixin(value = EMFCompatAnimationHook.class, remap = false)
public abstract class EmfCompatAnimationEndMixin {

    @Inject(method = "onAnimationEnd", at = @At("TAIL"), remap = false)
    private void ancientcombat$afterEmf(EMFAnimationApi.EMFAnimationHook.AnimationContext context,
                                        boolean wasCancelledByHook, CallbackInfo ci) {
        AcNpcEmfHook.afterRestore(context);
    }
}