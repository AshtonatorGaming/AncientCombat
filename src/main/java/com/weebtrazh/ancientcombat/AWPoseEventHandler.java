package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

@Mod.EventBusSubscriber(modid = "awbettercombatcompat", value = Dist.CLIENT)
public class AWPoseEventHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Run only every 4 ticks for performance
        tickCounter++;
        if (tickCounter < 4) return;
        tickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof NpcBase)) continue;

            NpcBase npc = (NpcBase) entity;
            if (!(npc instanceof Mob)) continue;

            Mob mob = (Mob) npc;
            if (!(mob instanceof PlayerAttackAnimatable)) continue;

            PlayerAttackAnimatable animatable = (PlayerAttackAnimatable) mob;
            ItemStack mainHand = mob.getMainHandItem();
            WeaponAttributes attributes = WeaponRegistry.getAttributes(mainHand);

            if (attributes != null && attributes.pose() != null) {
                animatable.updateAnimationsOnTick();
            }
        }
    }
}