package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * NPC-side helpers for Better Combat attribute checks and the EMF render thread-local.
 * Do not register {@code is_playeranimator_active}, {@code is_playeranimator_animation_active},
 * or {@code is_mob_playeranimator_active}. Player Animator, EMF Compat, and FA packs already own
 * those singleton variables. Re-registering them replaces the player values.
 */
@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {
    private static final ThreadLocal<LivingEntity> EMF_NPC = new ThreadLocal<>();

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
}
