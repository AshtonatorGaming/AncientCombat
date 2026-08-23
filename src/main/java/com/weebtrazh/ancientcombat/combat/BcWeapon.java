package com.weebtrazh.ancientcombat.combat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BcWeapon {
    private BcWeapon() {}

    public static boolean hasCompatibleWeapon(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        ItemStack stack = entity.getMainHandItem();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            return false;
        }

        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null) {
            return false;
        }
        if (attributes.attacks() == null || attributes.attacks().length == 0) {
            return false;
        }
        if (attributes.attackRange() <= 0.1) {
            return false;
        }

        return true;
    }

    public static WeaponAttributes getAttributes(LivingEntity entity) {
        if (!hasCompatibleWeapon(entity)) {
            return null;
        }
        return WeaponRegistry.getAttributes(entity.getMainHandItem());
    }
}