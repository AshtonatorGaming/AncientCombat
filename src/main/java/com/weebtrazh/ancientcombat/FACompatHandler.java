package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {

    private static final Map<LivingEntity, float[]> LAST_ARMS = new WeakHashMap<>();
    private static final ThreadLocal<LivingEntity> EMF_NPC = new ThreadLocal<>();

    private static boolean resolved;
    private static Method mpaIsAnimating;
    private static Method getCurrentEntity;

    public static void setEmfNpc(LivingEntity entity) {
        EMF_NPC.set(entity);
    }

    public static boolean shouldKeepCombatPose(LivingEntity entity) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return false;
        if (entity instanceof Player) return false;
        if (!(entity instanceof NpcBase)) return false;
        try {
            if (mpaIsAnimating != null && Boolean.TRUE.equals(mpaIsAnimating.invoke(null, entity))) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return hasCombatAttributes(entity.getMainHandItem());
    }

    public static boolean hasCombatAttributes(ItemStack stack) {
        try {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
            if (attributes == null) return false;
            if (attributes.pose() != null) return true;
            WeaponAttributes.Attack[] attacks = attributes.attacks();
            return attacks != null && attacks.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean currentEmfEntityNeedsCombatLock() {
        LivingEntity local = EMF_NPC.get();
        if (local != null && shouldKeepCombatPose(local)) return true;
        if (getCurrentEntity == null) return false;
        try {
            Entity e = unwrap(getCurrentEntity.invoke(null));
            return e instanceof LivingEntity living && shouldKeepCombatPose(living);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void captureForEmfCompat(PlayerModel<?> model, LivingEntity entity) {
        if (!shouldKeepCombatPose(entity)) {
            LAST_ARMS.remove(entity);
            return;
        }
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.jacket.copyFrom(model.body);
        LAST_ARMS.put(entity, new float[]{
                model.leftArm.xRot, model.leftArm.yRot, model.leftArm.zRot,
                model.rightArm.xRot, model.rightArm.yRot, model.rightArm.zRot
        });
    }

    public static void applyCapturedArms(HumanoidModel<?> model, LivingEntity entity) {
    }

    private static void applyArm(ModelPart root, float xRot, float yRot, float zRot) {
        if (root == null) return;
        root.xRot = xRot;
        root.yRot = yRot;
        root.zRot = zRot;
    }

    public static Entity entityFromEmfState(Object state) {
        if (state == null) return null;
        Entity direct = unwrap(state);
        if (direct != null) return direct;
        try {
            for (Method m : state.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Object v = m.invoke(state);
                Entity e = unwrap(v);
                if (e != null) return e;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Entity unwrap(Object emfEntity) {
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

    public static void pauseEmfFor(LivingEntity entity) {
    }

    public static void resumeEmfFor(LivingEntity entity) {
    }

    public static void captureHumanoidCombatParts(HumanoidModel<?> human) {
    }

    public static void restoreHumanoidCombatParts(HumanoidModel<?> human) {
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> helper = Class.forName("me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper");
            mpaIsAnimating = helper.getMethod("isAnimating", LivingEntity.class);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
            getCurrentEntity = api.getMethod("getCurrentEntity");
            Method registerBool = api.getMethod(
                    "registerSingletonAnimationVariable",
                    String.class, String.class, String.class, BooleanSupplier.class
            );
            BooleanSupplier supplier = FACompatHandler::currentEmfEntityNeedsCombatLock;
            registerBool.invoke(null, AncientCombat.MOD_ID, "is_playeranimator_active", "is_playeranimator_active", supplier);
            registerBool.invoke(null, AncientCombat.MOD_ID, "is_playeranimator_animation_active", "is_playeranimator_animation_active", supplier);
            registerBool.invoke(null, AncientCombat.MOD_ID, "is_mob_playeranimator_active", "is_mob_playeranimator_active", supplier);
            AncientCombat.LOGGER.info("AW NPC FAPE vars registered");
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("EMF var register failed: {}", t.toString());
        }
    }

    static {
        resolve();
    }
}