package com.weebtrazh.ancientcombat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.parts.EMFModelPartRoot;
import traben.entity_model_features.models.parts.EMFModelPartVanilla;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Runs on EMF's animation hook for Ancient Warfare NPCs.
 *
 * <p>Fresh Animations (and the FAPE pack this mod ships) hang the visible arm mesh on children
 * of {@code left_arm}, {@code right_arm}, {@code left_sleeve} and {@code right_sleeve}. Those
 * children keep playing the idle clip after EMF Compat has restored the Better Combat rotation
 * onto the vanilla arm, so the torso still twists and the weapon arm does not. Zeroing the
 * child rotations lets them inherit the restored parent.</p>
 *
 * <p>Also called from {@code EmfCompatAnimationEndMixin} at the tail of EMF Compat's own hook,
 * which is after that mod's pose restore. Doing it here as well covers the case where our hook
 * is registered later and runs last. Update both sites together if EMF Compat moves restore
 * off {@code onAnimationEnd}.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class AcNpcEmfHook extends EMFAnimationApi.EMFAnimationHook {

    /** Official 1.20.1 keeps this private. One lookup, then plain map reads per frame. */
    private static final Field CHILDREN = findChildren();

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
        afterRestore(context);
    }

    public static void afterRestore(AnimationContext context) {
        if (context == null || !CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return;
        // uuid() is what EMF Compat keys poses by. Fall back to the unwrapped NPC if the
        // ETF parent method is not on this EMF build.
        UUID uuid = FACompatHandler.uuidFromState(context.activeState());
        if (uuid == null) {
            Entity entity = FACompatHandler.entityFromEmfState(context.activeState());
            if (entity instanceof NpcBase npc) uuid = npc.getUUID();
        }
        if (!FACompatHandler.isCombatLocked(uuid)) return;

        EMFModelPartRoot root = context.animatingModelRoot();
        if (root == null) return;
        Map<String, EMFModelPartVanilla> byName = root.getAllVanillaPartsByNameEMF();
        if (byName == null) return;

        EMFModelPartVanilla left = byName.get("left_arm");
        EMFModelPartVanilla right = byName.get("right_arm");
        // Rotation only. EMF Compat already applied body-follow position in its own hook.
        EmfCompatBridge.reapplyArmRotation(uuid, left, right);

        zeroChildRotations(left);
        zeroChildRotations(right);
        zeroChildRotations(byName.get("left_sleeve"));
        zeroChildRotations(byName.get("right_sleeve"));
    }

    /**
     * Clears rotation on every descendant, not just the first level. FAPE nests the bend
     * bones under the arm. Positions are left so the mesh stays on the limb.
     */
    private static void zeroChildRotations(ModelPart part) {
        for (ModelPart child : childrenOf(part)) {
            zeroTree(child);
        }
    }

    private static void zeroTree(ModelPart part) {
        if (part == null) return;
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        for (ModelPart child : childrenOf(part)) {
            zeroTree(child);
        }
    }

    @SuppressWarnings("unchecked")
    private static Iterable<ModelPart> childrenOf(ModelPart part) {
        if (part == null || CHILDREN == null) return Collections.emptyList();
        try {
            Object value = CHILDREN.get(part);
            if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                return (Iterable<ModelPart>) map.values();
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    private static Field findChildren() {
        try {
            Field field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("ModelPart.children unavailable, arm-child zero off: {}", t.toString());
            return null;
        }
    }
}
