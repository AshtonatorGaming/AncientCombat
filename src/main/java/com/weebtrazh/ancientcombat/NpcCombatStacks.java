package com.weebtrazh.ancientcombat;

import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.bettercombat.client.animation.PoseSubStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the Better Combat sub-stacks the way EMF Compat's {@code BetterCombatCompat} does for
 * players. Those lookups are hard-wired to fields declared on {@code AbstractClientPlayer}, so an
 * NPC never passes them. The stacks themselves are the same types, whether this mod attached them
 * or Better Mob Combat did.
 */
public final class NpcCombatStacks {
    private static final Map<Class<?>, Stacks> CACHE = new ConcurrentHashMap<>();

    private NpcCombatStacks() {}

    public static boolean attackActive(LivingEntity entity) {
        if (entity == null || !betterCombatLoaded()) return false;
        for (Field field : stacks(entity).attacks) {
            AttackAnimationSubStack stack = get(field, entity, AttackAnimationSubStack.class);
            if (stack == null || stack.base == null) continue;
            var animation = stack.base.getAnimation();
            if (animation != null && animation.isActive()) return true;
        }
        return false;
    }

    public static boolean poseActive(LivingEntity entity) {
        if (entity == null || !betterCombatLoaded()) return false;
        for (Field field : stacks(entity).poses) {
            PoseSubStack stack = get(field, entity, PoseSubStack.class);
            if (stack != null && stack.base != null && stack.base.isActive()) return true;
        }
        return false;
    }

    private static boolean betterCombatLoaded() {
        try {
            return ModList.get().isLoaded("bettercombat");
        } catch (Throwable t) {
            return false;
        }
    }

    private static Stacks stacks(LivingEntity entity) {
        return CACHE.computeIfAbsent(entity.getClass(), Stacks::scan);
    }

    private static <T> T get(Field field, LivingEntity entity, Class<T> type) {
        try {
            Object value = field.get(entity);
            if (type.isInstance(value)) return type.cast(value);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private record Stacks(List<Field> attacks, List<Field> poses) {
        static Stacks scan(Class<?> type) {
            List<Field> attacks = new ArrayList<>();
            List<Field> poses = new ArrayList<>();
            for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                for (Field field : cursor.getDeclaredFields()) {
                    if (field.getType() == AttackAnimationSubStack.class) {
                        field.setAccessible(true);
                        attacks.add(field);
                    } else if (field.getType() == PoseSubStack.class) {
                        field.setAccessible(true);
                        poses.add(field);
                    }
                }
            }
            return new Stacks(attacks, poses);
        }
    }
}
