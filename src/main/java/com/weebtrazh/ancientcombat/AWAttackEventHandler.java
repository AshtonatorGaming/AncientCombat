package com.weebtrazh.ancientcombat;

import net.minecraftforge.fml.common.Mod;

/**
 * Attack logic is handled by the AI mixin now.
 * This class is kept only so the mod still has a valid event subscriber package if needed later.
 */
@Mod.EventBusSubscriber(modid = "awbettercombatcompat")
public class AWAttackEventHandler {
    // Intentionally empty — no LivingAttack / LivingHurt canceling.
}