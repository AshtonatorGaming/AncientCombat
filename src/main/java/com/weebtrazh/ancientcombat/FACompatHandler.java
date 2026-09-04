package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {

    private static boolean resolved;
    private static boolean emfPresent;
    private static Method mpaIsAnimating;
    private static Method registerPause;
    private static Method registerVanilla;
    private static Method registerBoolVar;
    private static Method getCurrentEntity;
    private static Method emfEntityOf;

    private static final Map<HumanoidModel<?>, float[]> LAST_POSE = new WeakHashMap<>();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            emfPresent = ModList.get().isLoaded("entity_model_features");
            resolveHelpers();
            if (!emfPresent) {
                AncientCombat.LOGGER.info("EMF not present — AW NPC fancy-body compat skipped");
                return;
            }
            boolean pause = registerPauseCondition();
            boolean vanilla = registerVanillaCondition();
            boolean vars = registerAnimatorVars();
            AncientCombat.LOGGER.info("AW NPC EMF hooks pause={} vanillaLock={} vars={}", pause, vanilla, vars);
        });
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
            return attributes != null && attributes.pose() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean currentEmfEntityNeedsCombatLock() {
        Entity e = currentEntity();
        return e instanceof LivingEntity living && shouldKeepCombatPose(living);
    }

    public static void pauseEmfFor(LivingEntity entity) {
    }

    public static void resumeEmfFor(LivingEntity entity) {
    }

    public static void captureHumanoidCombatParts(HumanoidModel<?> human) {
        LAST_POSE.put(human, new float[]{
                human.rightArm.xRot, human.rightArm.yRot, human.rightArm.zRot,
                human.leftArm.xRot, human.leftArm.yRot, human.leftArm.zRot,
                human.body.xRot, human.body.yRot, human.body.zRot
        });
    }

    public static void restoreHumanoidCombatParts(HumanoidModel<?> human) {
        float[] p = LAST_POSE.get(human);
        if (p == null) {
            captureHumanoidCombatParts(human);
            p = LAST_POSE.get(human);
        }
        apply(human.rightArm, p[0], p[1], p[2]);
        apply(human.leftArm, p[3], p[4], p[5]);
        apply(human.body, p[6], p[7], p[8]);
        if (human instanceof PlayerModel<?> player) {
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.jacket.copyFrom(player.body);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
    }

    private static void apply(ModelPart part, float x, float y, float z) {
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

    private static Entity currentEntity() {
        if (getCurrentEntity == null) return null;
        try {
            return unwrap(getCurrentEntity.invoke(null));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void resolveHelpers() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> helper = Class.forName("me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper");
            mpaIsAnimating = helper.getMethod("isAnimating", LivingEntity.class);
        } catch (Throwable ignored) {
        }
        if (!emfPresent) return;
        try {
            Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
            getCurrentEntity = api.getMethod("getCurrentEntity");
            try {
                emfEntityOf = api.getMethod("emfEntityOf", Entity.class);
            } catch (Throwable ignored) {
            }
            registerPause = api.getMethod("registerPauseCondition", Function.class);
            registerVanilla = api.getMethod("registerVanillaModelCondition", Function.class);
            registerBoolVar = api.getMethod(
                    "registerSingletonAnimationVariable",
                    String.class, String.class, String.class, BooleanSupplier.class
            );
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("EMF API resolve failed", t);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean registerPauseCondition() {
        if (registerPause == null) return false;
        try {
            Function fn = (Object emfEntity) -> {
                Entity e = unwrap(emfEntity);
                return e instanceof LivingEntity living && shouldKeepCombatPose(living);
            };
            registerPause.invoke(null, fn);
            return true;
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("registerPauseCondition failed", t);
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean registerVanillaCondition() {
        if (registerVanilla == null) return false;
        try {
            Function fn = (Object emfEntity) -> {
                Entity e = unwrap(emfEntity);
                return e instanceof LivingEntity living && shouldKeepCombatPose(living);
            };
            registerVanilla.invoke(null, fn);
            return true;
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("registerVanillaModelCondition failed", t);
            return false;
        }
    }

    private static boolean registerAnimatorVars() {
        if (registerBoolVar == null) return false;
        BooleanSupplier supplier = FACompatHandler::currentEmfEntityNeedsCombatLock;
        boolean any = false;
        any |= registerVar("is_playeranimator_active", supplier);
        any |= registerVar("is_playeranimator_animation_active", supplier);
        any |= registerVar("is_mob_playeranimator_active", supplier);
        return any;
    }

    private static boolean registerVar(String name, BooleanSupplier supplier) {
        try {
            registerBoolVar.invoke(null, AncientCombat.MOD_ID, name, name, supplier);
            return true;
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("Failed to register EMF var {}", name, t);
            return false;
        }
    }

    private static Entity unwrap(Object emfEntity) {
        if (emfEntity instanceof Entity e) return e;
        if (emfEntity == null) return null;
        try {
            for (Method m : emfEntity.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (Entity.class.isAssignableFrom(m.getReturnType())) {
                    Object inner = m.invoke(emfEntity);
                    if (inner instanceof Entity e) return e;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}