package com.weebtrazh.ancientcombat.combat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Method;

/**
 * BC weapon gate for AW NPCs.
 * Throwables stay eligible for BC swings when already in melee.
 * Hold-range is separate so we do not path into the target while ammo remains.
 * Spartan lookups are cached (optional soft dependency).
 */
public final class BcWeapon {
    private BcWeapon() {}

    private static boolean spartanResolved;
    private static Class<?> throwingWeaponClass;
    private static String ammoNbtKey = "AmmoUsed";
    private static Method getMaxAmmoMethod;

    public static boolean hasCompatibleWeapon(LivingEntity entity) {
        if (entity == null) return false;
        ItemStack stack = entity.getMainHandItem();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) return false;

        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null) return false;
        if (attributes.attacks() == null || attributes.attacks().length == 0) return false;
        if (attributes.attackRange() <= 0.1) return false;
        return true;
    }

    public static WeaponAttributes getAttributes(LivingEntity entity) {
        if (!hasCompatibleWeapon(entity)) return null;
        return WeaponRegistry.getAttributes(entity.getMainHandItem());
    }

    public static boolean shouldHoldRange(LivingEntity entity) {
        return entity != null && isThrowableWithAmmo(entity.getMainHandItem());
    }

    public static boolean isThrowableWithAmmo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        resolveSpartan();
        if (throwingWeaponClass == null || getMaxAmmoMethod == null) return false;
        if (!throwingWeaponClass.isInstance(stack.getItem())) return false;

        try {
            int used = stack.getOrCreateTag().getInt(ammoNbtKey);
            int max = (Integer) getMaxAmmoMethod.invoke(stack.getItem(), stack);
            return used < max;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void resolveSpartan() {
        if (spartanResolved) return;
        spartanResolved = true;
        try {
            throwingWeaponClass = Class.forName("com.oblivioussp.spartanweaponry.item.ThrowingWeaponItem");
            try {
                Object key = throwingWeaponClass.getField("NBT_AMMO_USED").get(null);
                if (key instanceof String s) ammoNbtKey = s;
            } catch (ReflectiveOperationException ignored) {
            }
            getMaxAmmoMethod = throwingWeaponClass.getMethod("getMaxAmmo", ItemStack.class);
        } catch (Throwable t) {
            throwingWeaponClass = null;
            getMaxAmmoMethod = null;
        }
    }
}