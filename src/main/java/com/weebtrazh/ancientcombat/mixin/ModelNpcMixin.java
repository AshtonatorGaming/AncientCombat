package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.CompatConfig;
import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.render.ModelNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelNpc.class)
public abstract class ModelNpcMixin {

    @Inject(method = "m_6973_", at = @At("RETURN"), remap = false)
    private void ancientcombat$syncSleeves(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return;
        if (!(entity instanceof NpcBase npc)) return;
        if (!FACompatHandler.shouldKeepCombatPose(npc)) return;
        PlayerModel<?> self = (PlayerModel<?>) (Object) this;
        self.rightSleeve.copyFrom(self.rightArm);
        self.leftSleeve.copyFrom(self.leftArm);
        self.jacket.copyFrom(self.body);
        self.rightPants.copyFrom(self.rightLeg);
        self.leftPants.copyFrom(self.leftLeg);
    }
}