package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/**
 * Fresh Animations keeps an arm's rotation only while {@code is_playeranimator_active} is true.
 * Real players get that from Player Animator. NPCs get it from the same player stacks: an attack
 * layer, a weapon hold, or the few frames EMF Compat still holds the attack snapshot.
 */
@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {
    private static final ThreadLocal<LivingEntity> EMF_NPC = new ThreadLocal<>();

    private static boolean resolved;
    private static Method getCurrentEntity;

    public static void setEmfNpc(LivingEntity entity) {
        if (entity == null) {
            EMF_NPC.remove();
        } else {
            EMF_NPC.set(entity);
        }
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
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return false;
        LivingEntity living = currentLiving();
        if (!(living instanceof NpcBase npc)) return false;
        // Empty main hand still publishes fist attributes. A hold is a pose layer, not "has attacks".
        return NpcCombatStacks.attackActive(npc)
                || NpcCombatStacks.poseActive(npc)
                || EmfCompatBridge.isLingering(npc.getUUID());
    }

    private static LivingEntity currentLiving() {
        if (getCurrentEntity != null) {
            try {
                Entity entity = unwrap(getCurrentEntity.invoke(null));
                if (entity instanceof Player) return null;
                if (entity instanceof LivingEntity living) return living;
            } catch (Throwable ignored) {
            }
        }
        LivingEntity local = EMF_NPC.get();
        if (local instanceof Player) return null;
        return local;
    }

    private static Entity unwrap(Object emfEntity) {
        if (emfEntity instanceof Entity entity) return entity;
        if (emfEntity == null) return null;
        try {
            for (Method method : emfEntity.getClass().getMethods()) {
                if (method.getParameterCount() != 0) continue;
                if (Entity.class.isAssignableFrom(method.getReturnType())) {
                    Object inner = method.invoke(emfEntity);
                    if (inner instanceof Entity entity) return entity;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
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
