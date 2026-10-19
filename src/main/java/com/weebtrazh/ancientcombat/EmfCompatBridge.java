package com.weebtrazh.ancientcombat;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;
import strm.emfcompat.bettercombat.EMFCompatBetterCombatMod;
import strm.emfcompat.bettercombat.compat.AttackPauseOverride;
import strm.emfcompat.core.PoseManager;
import strm.emfcompat.core.PoseSnapshot;
import strm.emfcompat.core.SavedPoses;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NPC side of the EMF Compat pose API.
 *
 * <p>EMF Compat 2.3 (and the 2.0 jars this project compiles against — the methods below are
 * already on them) restores a snapshot after the resource-pack animation. The Better Combat
 * addon only does that for {@code AbstractClientPlayer}, so an NPC's arms never get put back
 * and the pack's body twist is all that remains.</p>
 *
 * <p>Signatures this class calls, kept here so the next EMF Compat break is a one-file edit:</p>
 * <ul>
 *   <li>{@code PoseManager.savePoses(UUID, String, PoseSnapshot, PoseSnapshot, Map, Vector3f)}</li>
 *   <li>{@code PoseManager.clearPoses(UUID, String)}</li>
 *   <li>{@code PoseManager.setSourcePriority(String, int)}</li>
 *   <li>{@code PoseSnapshot(ModelPart)} and {@code PoseSnapshot(ModelPart, boolean rotationOnly)}</li>
 *   <li>sources: {@code "better_combat"} (attacks, priority 0) and
 *       {@code "better_combat_pose"} (holds, priority -10)</li>
 *   <li>{@code EMFCompatBetterCombatMod.isBodyFollow()} / {@code isAttackLegs()}</li>
 *   <li>{@code AttackPauseOverride.markAttackActive} / {@code tickCooldown} / {@code isUnpaused}</li>
 * </ul>
 *
 * <p>If {@code emf_compat_core} is not installed, every method no-ops and Better Combat keeps
 * the normal Player Animator path.</p>
 */
public final class EmfCompatBridge {
    /** Attack swings. Priority 0, so a swing takes the arms from a hold. */
    public static final String ATTACK_SOURCE = "better_combat";
    /** Two-handed / weapon hold. Lower than an attack. Matches the Better Combat addon. */
    public static final String STANCE_SOURCE = "better_combat_pose";

    private static final int STANCE_PRIORITY = -10;
    private static final int LOCAL_LINGER_FRAMES = 5;
    private static final float LEG_MOVE_THRESHOLD = 0.15F;

    private static boolean ready;
    private static boolean failed;
    private static final Map<UUID, Integer> LOCAL_LINGER = new HashMap<>();

    private EmfCompatBridge() {}

    public static boolean available() {
        if (failed) return false;
        if (ready) return true;
        try {
            if (!ModList.get().isLoaded("emf_compat_core")) {
                failed = true;
                return false;
            }
            // Hold loses to an attack when both are saved. The addon sets the same priority;
            // setting it again is a no-op when the addon already did.
            PoseManager.setSourcePriority(STANCE_SOURCE, STANCE_PRIORITY);
            ready = true;
            AncientCombat.LOGGER.info(
                    "AW NPC pose capture using EMF Compat sources {} and {}",
                    ATTACK_SOURCE, STANCE_SOURCE);
            return true;
        } catch (Throwable t) {
            failed = true;
            AncientCombat.LOGGER.warn("EMF Compat pose API mismatch, NPC pose capture off: {}", t.toString());
            return false;
        }
    }

    public static void saveAttack(UUID uuid, PlayerModel<?> model, float limbSwingAmount) {
        if (!available()) return;
        try {
            Map<String, PoseSnapshot> parts = null;
            if (attackLegs() && limbSwingAmount < LEG_MOVE_THRESHOLD) {
                parts = new HashMap<>();
                parts.put("left_leg", new PoseSnapshot(model.leftLeg, true));
                parts.put("right_leg", new PoseSnapshot(model.rightLeg, true));
            }
            PoseManager.savePoses(
                    uuid, ATTACK_SOURCE,
                    new PoseSnapshot(model.leftArm),
                    new PoseSnapshot(model.rightArm),
                    parts,
                    bodyBase(model)
            );
        } catch (Throwable t) {
            fail(t);
        }
    }

    public static void saveStance(UUID uuid, PlayerModel<?> model) {
        if (!available()) return;
        try {
            PoseManager.savePoses(
                    uuid, STANCE_SOURCE,
                    new PoseSnapshot(model.leftArm),
                    new PoseSnapshot(model.rightArm),
                    null,
                    bodyBase(model)
            );
        } catch (Throwable t) {
            fail(t);
        }
    }

    public static void clearAttack(UUID uuid) {
        clear(uuid, ATTACK_SOURCE);
    }

    public static void clearStance(UUID uuid) {
        clear(uuid, STANCE_SOURCE);
    }

    public static void clearNpc(UUID uuid) {
        clearAttack(uuid);
        clearStance(uuid);
        LOCAL_LINGER.remove(uuid);
    }

    /**
     * Puts the saved attack/hold rotation back on the vanilla arm parts.
     * Position is left alone so a body-follow restore from EMF Compat is not undone.
     * Returns false when nothing was stored.
     */
    public static boolean reapplyArmRotation(UUID uuid, ModelPart leftArm, ModelPart rightArm) {
        if (!available()) return false;
        try {
            SavedPoses poses = PoseManager.getSavedPoses(uuid);
            if (poses == null) return false;
            boolean applied = false;
            if (poses.leftArm() != null && leftArm != null) {
                poses.leftArm().applyRotation(leftArm);
                applied = true;
            }
            if (poses.rightArm() != null && rightArm != null) {
                poses.rightArm().applyRotation(rightArm);
                applied = true;
            }
            return applied;
        } catch (Throwable t) {
            fail(t);
            return false;
        }
    }

    public static void markAttack(UUID uuid) {
        if (addonLoaded()) {
            try {
                AttackPauseOverride.markAttackActive(uuid);
                return;
            } catch (Throwable t) {
                AncientCombat.LOGGER.debug("AttackPauseOverride.markAttackActive failed: {}", t.toString());
            }
        }
        LOCAL_LINGER.put(uuid, LOCAL_LINGER_FRAMES);
    }

    public static void tickLinger(UUID uuid) {
        if (addonLoaded()) {
            try {
                AttackPauseOverride.tickCooldown(uuid);
                return;
            } catch (Throwable t) {
                AncientCombat.LOGGER.debug("AttackPauseOverride.tickCooldown failed: {}", t.toString());
            }
        }
        LOCAL_LINGER.computeIfPresent(uuid, (id, frames) -> frames <= 1 ? null : frames - 1);
    }

    public static boolean isLingering(UUID uuid) {
        if (addonLoaded()) {
            try {
                return AttackPauseOverride.isUnpaused(uuid);
            } catch (Throwable t) {
                AncientCombat.LOGGER.debug("AttackPauseOverride.isUnpaused failed: {}", t.toString());
            }
        }
        return LOCAL_LINGER.containsKey(uuid);
    }

    private static void clear(UUID uuid, String source) {
        if (!available()) return;
        try {
            PoseManager.clearPoses(uuid, source);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** Null when the addon is on rotation-only. Arms then keep EMF's arm position. */
    private static Vector3f bodyBase(PlayerModel<?> model) {
        if (!bodyFollow() || model.body == null) return null;
        return new Vector3f(model.body.x, model.body.y, model.body.z);
    }

    private static boolean bodyFollow() {
        if (!addonLoaded()) return true;
        try {
            return EMFCompatBetterCombatMod.isBodyFollow();
        } catch (Throwable t) {
            return true;
        }
    }

    /** Off unless the Better Combat addon is installed and its "Attack legs" option is on. */
    private static boolean attackLegs() {
        if (!addonLoaded()) return false;
        try {
            return EMFCompatBetterCombatMod.isAttackLegs();
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

    private static void fail(Throwable t) {
        failed = true;
        ready = false;
        AncientCombat.LOGGER.warn("EMF Compat pose call failed, NPC pose capture off: {}", t.toString());
    }
}
