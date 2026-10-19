package com.weebtrazh.ancientcombat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import traben.entity_model_features.EMFAnimationApi;

@OnlyIn(Dist.CLIENT)
public final class AcNpcEmfHook extends EMFAnimationApi.EMFAnimationHook {

    public static void register() {
        try {
            EMFAnimationApi.registerAnimationHook(new AcNpcEmfHook());
            AncientCombat.LOGGER.info("AW NPC EMF onAnimationEnd hook registered");
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("AW NPC EMF hook failed: {}", t.toString());
        }
    }

    @Override
    public void onAnimationEnd(AnimationContext context, boolean wasCancelledByHook) {
    }
}