package com.weebtrazh.ancientcombat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import strm.emfcompat.core.PoseManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Every 200 frames {@code PoseManager} drops snapshots whose id is not in {@code ClientLevel.players()}.
 * An NPC hold would disappear on that tick. The three {@code retainAll} calls are the player-id filter.
 */
@Mixin(value = PoseManager.class, remap = false)
public abstract class PoseManagerNpcCleanupMixin {

    @Redirect(
            method = "cleanupIfNeeded",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;retainAll(Ljava/util/Collection;)Z"),
            require = 3,
            remap = false
    )
    private static boolean ancientcombat$keepNpcPoses(Set<?> keys, Collection<?> playerIds) {
        Set<Object> keep = new HashSet<>();
        if (playerIds != null) keep.addAll(playerIds);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof NpcBase) keep.add(entity.getUUID());
            }
        }
        return keys.retainAll(keep);
    }
}
