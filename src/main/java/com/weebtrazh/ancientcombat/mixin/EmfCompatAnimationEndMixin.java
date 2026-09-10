package com.weebtrazh.ancientcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "strm.emfcompat.core.EMFCompatAnimationHook", remap = false)
public abstract class EmfCompatAnimationEndMixin {

    @Inject(method = "onAnimationEnd", at = @At("TAIL"), remap = false)
    private void ancientcombat$afterEmf(@Coerce Object context, boolean wasCancelledByHook, CallbackInfo ci) {
    }
}