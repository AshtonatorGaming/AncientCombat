package com.weebtrazh.ancientcombat.mixin;

import me.Thelnfamous1.bettermobcombat.logic.MobTargetFinder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTargetFinder.class, remap = false)
public class MobTargetFinderMixin {

    private static final float TALL_HEIGHT = 2.4F;
    private static final double ORIGIN_HEIGHT_FROM_FEET = 1.15;

    @Inject(method = "getInitialTracingPoint", at = @At("RETURN"), cancellable = true, remap = false)
    private static void awBetterCombat$lowerOrigin(LivingEntity mob, CallbackInfoReturnable<Vec3> cir) {
        if (!(mob instanceof NpcBase)) return;
        if (mob.getBbHeight() < TALL_HEIGHT) return; // hobbits unchanged

        Vec3 original = cir.getReturnValue();
        if (original == null) return;

        cir.setReturnValue(new Vec3(original.x, mob.getY() + ORIGIN_HEIGHT_FROM_FEET, original.z));
    }
}