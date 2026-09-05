package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {

    public static final String EMF_COMPAT_SOURCE = "better_combat";

    private static boolean resolved;
    private static boolean emfCompatPresent;
    private static Method mpaIsAnimating;
    private static Method savePoses;
    private static Method clearPoses;
    private static Constructor<?> snapshotCtor;

    public static boolean emfCompatCoreLoaded() {
        resolve();
        return emfCompatPresent && savePoses != null;
    }

    public static boolean shouldKeepCombatPose(LivingEntity entity) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return false;
        if (!(entity instanceof NpcBase)) return false;
        try {
            if (mpaIsAnimating != null && Boolean.TRUE.equals(mpaIsAnimating.invoke(null, entity))) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(entity.getMainHandItem());
            if (attributes == null) return false;
            if (attributes.pose() != null) return true;
            WeaponAttributes.Attack[] attacks = attributes.attacks();
            return attacks != null && attacks.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void captureForEmfCompat(PlayerModel<?> model, LivingEntity entity) {
        if (!emfCompatCoreLoaded()) return;
        if (!shouldKeepCombatPose(entity)) {
            clearEmfCompatPose(entity.getUUID());
            return;
        }
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.jacket.copyFrom(model.body);
        try {
            Object left = snapshotCtor.newInstance(model.leftArm);
            Object right = snapshotCtor.newInstance(model.rightArm);
            Vector3f bodyBase = new Vector3f(model.body.x, model.body.y, model.body.z);
            savePoses.invoke(null, entity.getUUID(), EMF_COMPAT_SOURCE, left, right, null, bodyBase);
        } catch (Throwable t) {
            AncientCombat.LOGGER.debug("EMF Compat pose save failed: {}", t.toString());
        }
    }

    public static void clearEmfCompatPose(UUID uuid) {
        if (clearPoses == null) return;
        try {
            clearPoses.invoke(null, uuid, EMF_COMPAT_SOURCE);
        } catch (Throwable ignored) {
        }
    }

    public static void pauseEmfFor(LivingEntity entity) {
    }

    public static void resumeEmfFor(LivingEntity entity) {
    }

    public static void captureHumanoidCombatParts(HumanoidModel<?> human) {
    }

    public static void restoreHumanoidCombatParts(HumanoidModel<?> human) {
        if (emfCompatCoreLoaded()) return;
        if (human instanceof PlayerModel<?> player) {
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.jacket.copyFrom(player.body);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> helper = Class.forName("me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper");
            mpaIsAnimating = helper.getMethod("isAnimating", LivingEntity.class);
        } catch (Throwable ignored) {
        }
        emfCompatPresent = ModList.get().isLoaded("emf_compat_core")
                || ModList.get().isLoaded("emf-compat-core");
        if (!emfCompatPresent) {
            try {
                Class.forName("strm.emfcompat.core.PoseManager");
                emfCompatPresent = true;
            } catch (Throwable ignored) {
            }
        }
        if (!emfCompatPresent) return;
        try {
            Class<?> manager = Class.forName("strm.emfcompat.core.PoseManager");
            Class<?> snapshot = Class.forName("strm.emfcompat.core.PoseSnapshot");
            snapshotCtor = snapshot.getConstructor(ModelPart.class);
            savePoses = manager.getMethod(
                    "savePoses",
                    UUID.class,
                    String.class,
                    snapshot,
                    snapshot,
                    Map.class,
                    Vector3f.class
            );
            clearPoses = manager.getMethod("clearPoses", UUID.class, String.class);
            AncientCombat.LOGGER.info("AW NPCs will reuse EMF Compat Core PoseManager source={}", EMF_COMPAT_SOURCE);
        } catch (Throwable t) {
            savePoses = null;
            AncientCombat.LOGGER.warn("EMF Compat Core present but PoseManager API mismatch: {}", t.toString());
        }
    }
}