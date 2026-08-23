package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.CompatConfig;
import me.Thelnfamous1.bettermobcombat.logic.MobTargetFinder;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MobTargetFinder.class, remap = false)
public class MobTargetFinderHitsMixin {

    @Inject(method = "findAttackTargets", at = @At("RETURN"), cancellable = true, remap = false)
    private static void ancientCombat$forceAiTarget(
            LivingEntity mob,
            Entity intendedTarget,
            WeaponAttributes.Attack attack,
            double attackRange,
            CallbackInfoReturnable<List<Entity>> cir
    ) {
        if (!(mob instanceof NpcBase)) return;
        if (mob.getBbHeight() < CompatConfig.TALL_HEIGHT.get()
                && mob.getBbWidth() < CompatConfig.WIDE_WIDTH.get()) {
            return;
        }

        Entity target = intendedTarget;
        if (target == null && mob instanceof Mob m) {
            target = m.getTarget();
        }
        if (target == null || !target.isAlive()) return;

        List<Entity> list = new ArrayList<>(cir.getReturnValue());
        if (list.contains(target)) return;

        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        double maxHoriz = attackRange
                + mob.getBbWidth() * 0.5
                + target.getBbWidth() * 0.5
                + Math.max(0.75, mob.getBbWidth() * 0.35);

        if (horiz <= maxHoriz) {
            list.add(target);
            cir.setReturnValue(list);
        }
    }
}