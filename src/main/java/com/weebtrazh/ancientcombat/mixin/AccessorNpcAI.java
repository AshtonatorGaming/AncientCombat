package com.weebtrazh.ancientcombat.mixin;

import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NpcAI.class)
public interface AccessorNpcAI {
    @Accessor(value = "npc", remap = false)
    NpcBase ancientcombat$getNpc();
}