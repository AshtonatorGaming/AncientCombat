package com.weebtrazh.ancientcombat;

import net.minecraft.world.InteractionHand;

/**
 * Client tick and swing for an NPC that owns the player animation stack.
 * LivingEntity declares {@code tick} and {@code swing}; NpcBase's jar does not, so the injects
 * live on LivingEntity and call through here.
 */
public interface NpcClientAttacker {
    void ancientcombat$clientTick();

    void ancientcombat$clientSwing(InteractionHand hand);
}
