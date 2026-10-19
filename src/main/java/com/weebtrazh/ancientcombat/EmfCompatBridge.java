package com.weebtrazh.ancientcombat;

import net.minecraft.client.model.PlayerModel;
import net.minecraftforge.fml.ModList;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.joml.Vector3f;
import strm.emfcompat.bettercombat.EMFCompatBetterCombatMod;
import strm.emfcompat.bettercombat.compat.AttackPauseOverride;
import strm.emfcompat.core.PoseManager;
import strm.emfcompat.core.PoseSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Better Combat addon's {@code PlayerModel} capture, for an NPC.
 *
 * <p>The addon method bails out on {@code instanceof AbstractClientPlayer} and then casts. The
 * cast cannot succeed for {@code NpcBase}, and {@code BetterCombatCompat} only reflects fields
 * declared on the player class. Cancelling that method and running this copy is the same snapshot:
 * source {@code better_combat} while the attack layer is playing, source {@code better_combat_pose}
 * while a weapon pose layer is playing, legs only when the addon's attack-legs option is on and the
 * NPC is nearly still, body position only when body-follow is on. EMF Compat's own animation hook
 * puts the snapshot back. This class does not touch arm children.</p>
 */
public final class EmfCompatBridge {
    public static final String ATTACK_SOURCE = "better_combat";
    public static final String STANCE_SOURCE = "better_combat_pose";

    private static final int STANCE_PRIORITY = -10;
    private static final float LEG_MOVE_THRESHOLD = 0.15F;

    private static boolean prioritiesSet;

    private EmfCompatBridge() {}

    /**
     * Called at the head of the addon's capture, after Player Animator has already written the
     * NPC's arm pose onto the model.
     */
    public static void capture(PlayerModel<?> model, NpcBase npc, float limbSwingAmount) {
        UUID uuid = npc.getUUID();
        try {
            captureUnchecked(model, npc, uuid, limbSwingAmount);
        } catch (Throwable t) {
            AncientCombat.LOGGER.debug("NPC pose capture failed: {}", t.toString());
            clear(uuid, ATTACK_SOURCE);
            clear(uuid, STANCE_SOURCE);
        }
    }

    private static void captureUnchecked(PlayerModel<?> model, NpcBase npc, UUID uuid, float limbSwingAmount) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get() || !addonEnabled()) {
            clear(uuid, ATTACK_SOURCE);
            clear(uuid, STANCE_SOURCE);
            return;
        }
        ensurePriorities();

        boolean stance = EMFCompatBetterCombatMod.isWeaponStances() && NpcCombatStacks.poseActive(npc);
        if (stance) {
            save(uuid, STANCE_SOURCE, model, null, bodyBase(model));
        } else {
            clear(uuid, STANCE_SOURCE);
        }

        if (!NpcCombatStacks.attackActive(npc)) {
            AttackPauseOverride.tickCooldown(uuid);
            if (!AttackPauseOverride.isUnpaused(uuid)) {
                clear(uuid, ATTACK_SOURCE);
            }
            return;
        }

        AttackPauseOverride.markAttackActive(uuid);
        Map<String, PoseSnapshot> legs = null;
        if (EMFCompatBetterCombatMod.isAttackLegs() && limbSwingAmount < LEG_MOVE_THRESHOLD) {
            legs = new HashMap<>();
            legs.put("left_leg", new PoseSnapshot(model.leftLeg, true));
            legs.put("right_leg", new PoseSnapshot(model.rightLeg, true));
        }
        save(uuid, ATTACK_SOURCE, model, legs, bodyBase(model));
    }

    public static boolean isLingering(UUID uuid) {
        if (uuid == null || !addonLoaded()) return false;
        try {
            return AttackPauseOverride.isUnpaused(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void save(UUID uuid, String source, PlayerModel<?> model,
                             Map<String, PoseSnapshot> parts, Vector3f body) {
        try {
            PoseManager.savePoses(
                    uuid, source,
                    new PoseSnapshot(model.leftArm),
                    new PoseSnapshot(model.rightArm),
                    parts,
                    body
            );
        } catch (Throwable t) {
            AncientCombat.LOGGER.debug("NPC pose save failed: {}", t.toString());
        }
    }

    private static void clear(UUID uuid, String source) {
        if (!coreLoaded()) return;
        try {
            PoseManager.clearPoses(uuid, source);
        } catch (Throwable t) {
            AncientCombat.LOGGER.debug("NPC pose clear failed: {}", t.toString());
        }
    }

    private static Vector3f bodyBase(PlayerModel<?> model) {
        if (!EMFCompatBetterCombatMod.isBodyFollow() || model.body == null) return null;
        return new Vector3f(model.body.x, model.body.y, model.body.z);
    }

    private static void ensurePriorities() {
        if (prioritiesSet) return;
        prioritiesSet = true;
        try {
            PoseManager.setSourcePriority(STANCE_SOURCE, STANCE_PRIORITY);
        } catch (Throwable ignored) {
        }
    }

    private static boolean addonEnabled() {
        if (!addonLoaded()) return false;
        try {
            return EMFCompatBetterCombatMod.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean addonLoaded() {
        try {
            return ModList.get().isLoaded("emf_compat_better_combat");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean coreLoaded() {
        try {
            return ModList.get().isLoaded("emf_compat_core");
        } catch (Throwable t) {
            return false;
        }
    }
}
