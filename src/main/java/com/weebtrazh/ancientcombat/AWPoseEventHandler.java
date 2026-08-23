package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT)
public class AWPoseEventHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!CompatConfig.ENABLE_POSE_TICK.get()) return;

        tickCounter++;
        if (tickCounter < CompatConfig.POSE_TICK_INTERVAL.get()) return;
        tickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        double r = CompatConfig.POSE_SCAN_RADIUS.get();
        AABB box = player.getBoundingBox().inflate(r);

        for (Entity entity : mc.level.getEntities(player, box, e -> e instanceof NpcBase)) {
            NpcBase npc = (NpcBase) entity;
            // NpcBase already extends Mob — no instanceof Mob check
            if (!(npc instanceof PlayerAttackAnimatable animatable)) continue;

            ItemStack mainHand = npc.getMainHandItem();
            WeaponAttributes attributes = WeaponRegistry.getAttributes(mainHand);
            if (attributes != null && attributes.pose() != null) {
                animatable.updateAnimationsOnTick();
            }
        }
    }
}