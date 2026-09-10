package com.weebtrazh.ancientcombat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.models.parts.EMFModelPartVanilla;

import java.util.Map;

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
        if (context == null) return;
        EMFEntityRenderState state = context.activeState();
        EMFModelPartRoot root = context.animatingModelRoot();
        if (state == null || root == null) return;

        Entity entity = FACompatHandler.entityFromEmfState(state);
        if (!(entity instanceof NpcBase)) return;
        if (!FACompatHandler.hasCombatAttributes(((NpcBase) entity).getMainHandItem())
                && !FACompatHandler.shouldKeepCombatPose((NpcBase) entity)) {
            return;
        }

        Map<String, EMFModelPartVanilla> byName = root.getAllVanillaPartsByNameEMF();
        if (byName == null) return;

        glueChildren(byName.get("left_arm"));
        glueChildren(byName.get("right_arm"));
        glueChildren(byName.get("left_sleeve"));
        glueChildren(byName.get("right_sleeve"));
    }

    private static void glueChildren(EMFModelPartVanilla arm) {
        if (arm == null) return;
        try {
            arm.getAllParts().forEach(part -> {
                if (part == arm) return;
                part.xRot = 0.0F;
                part.yRot = 0.0F;
                part.zRot = 0.0F;
            });
        } catch (Throwable ignored) {
        }
    }
}