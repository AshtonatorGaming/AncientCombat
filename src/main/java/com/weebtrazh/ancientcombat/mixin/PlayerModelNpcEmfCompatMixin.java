package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the NPC EMF is currently posing so the FAPE arm gate can see the player animation stacks.
 * The pose snapshot itself is {@link EmfPlayerModelNpcMixin}.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelNpcEmfCompatMixin {

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void ancientcombat$markNpc(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                       float ageInTicks, float netHeadYaw, float headPitch,
                                       CallbackInfo ci) {
        if (entity instanceof NpcBase) {
            FACompatHandler.setEmfNpc(entity);
        } else {
            FACompatHandler.setEmfNpc(null);
        }
    }
}
