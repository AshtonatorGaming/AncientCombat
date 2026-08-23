package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.CompatConfig;
import com.weebtrazh.ancientcombat.combat.BcWeapon;
import me.Thelnfamous1.bettermobcombat.api.MobAttackWindup;
import me.Thelnfamous1.bettermobcombat.logic.MobCombatHelper;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackMeleeLongRange;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NpcAIAttackMeleeLongRange.class)
public abstract class NpcAIAttackMeleeLongRangeMixin {

    @Unique
    private int ancientCombat$inRangeTicks;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientCombat$doAttack(double distanceToEntity, CallbackInfo ci) {
        if (!CompatConfig.ENABLE_BC_MELEE.get()) return;

        NpcAIAttackMeleeLongRange self = (NpcAIAttackMeleeLongRange) (Object) this;
        NpcBase npc = ((AccessorNpcAI) this).ancientcombat$getNpc();
        if (npc == null) return;

        if (!BcWeapon.hasCompatibleWeapon(npc)) return;

        if (BcWeapon.shouldHoldRange(npc) && !CompatConfig.BC_MELEE_ON_THROWABLE_IN_RANGE.get()) {
            return;
        }

        WeaponAttributes attributes = BcWeapon.getAttributes(npc);
        if (attributes == null) return;

        ci.cancel();

        Mob mob = (Mob) npc;
        LivingEntity target = self.getTarget();
        if (target == null || !target.isAlive()) {
            ancientCombat$inRangeTicks = 0;
            return;
        }

        double dist = npc.distanceTo(target);
        double reach = ancientCombat$effectiveReach(npc, attributes);
        double maxDist = ancientCombat$bodyRadius(npc) + ancientCombat$bodyRadius(target) + reach;

        // Face every attack tick while we have a target (body-oriented BC hitboxes)
        ancientCombat$faceTarget(npc, target, dist < maxDist * 1.25);

        if (self.getAttackDelay() > 0) return;

        if (!MobCombatHelper.isAttackReady(mob)) {
            self.setAttackDelay(1);
            return;
        }

        if (dist > maxDist) {
            ancientCombat$inRangeTicks = 0;
            self.setAttackDelay(1);
            return;
        }

        ancientCombat$inRangeTicks++;

        double yawErr = dist < maxDist * 0.5
                ? CompatConfig.MAX_YAW_ERROR_CLOSE.get()
                : CompatConfig.MAX_YAW_ERROR.get();
        float yawLimit = (float) yawErr;

        boolean facing = ancientCombat$isBodyFacingTarget(npc, target, yawLimit);
        int forceAfter = CompatConfig.FORCE_SWING_TICKS.get();
        if (!facing && (forceAfter <= 0 || ancientCombat$inRangeTicks < forceAfter)) {
            self.setAttackDelay(1);
            return;
        }

        ((MobAttackWindup) mob).bettermobcombat$startUpswing(attributes, null);
        self.setAttackDelay(1);
        ancientCombat$inRangeTicks = 0;
    }

    @Inject(method = "shouldCloseOnTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientCombat$shouldClose(double distanceToEntitySq, CallbackInfoReturnable<Boolean> cir) {
        NpcAIAttackMeleeLongRange self = (NpcAIAttackMeleeLongRange) (Object) this;
        NpcBase npc = ((AccessorNpcAI) this).ancientcombat$getNpc();
        if (npc == null) return;

        LivingEntity target = self.getTarget();
        if (target != null && target.isAlive()) {
            ancientCombat$faceTarget(npc, target, false);
        }

        if (CompatConfig.HOLD_RANGE_FOR_THROWABLES.get() && BcWeapon.shouldHoldRange(npc)) {
            cir.setReturnValue(false);
            return;
        }

        if (!CompatConfig.ENABLE_BC_CLOSER.get()) return;
        if (!BcWeapon.hasCompatibleWeapon(npc)) return;

        WeaponAttributes attributes = BcWeapon.getAttributes(npc);
        if (attributes == null) return;

        if (target == null) {
            cir.setReturnValue(true);
            return;
        }

        double reach = ancientCombat$effectiveReach(npc, attributes);
        double factor = ancientCombat$closeFactor(npc);
        double closeEnough = ancientCombat$bodyRadius(npc) + ancientCombat$bodyRadius(target) + reach * factor;

        boolean needClose = distanceToEntitySq > closeEnough * closeEnough
                || !npc.getSensing().hasLineOfSight(target);

        cir.setReturnValue(needClose);
    }

    private static double ancientCombat$effectiveReach(NpcBase npc, WeaponAttributes attributes) {
        double r = attributes.attackRange();
        if (r <= 0.1) r = 1.75;

        float h = npc.getBbHeight();
        float w = npc.getBbWidth();

        if (h >= CompatConfig.TALL_HEIGHT.get() || w >= CompatConfig.WIDE_WIDTH.get()) {
            r += Math.max(w, h * 0.25);
            return r;
        }
        if (h <= CompatConfig.TINY_HEIGHT.get()) {
            return r * CompatConfig.TINY_REACH_MULT.get();
        }
        if (h <= CompatConfig.SMALL_HEIGHT.get()) {
            return r * CompatConfig.SMALL_REACH_MULT.get();
        }
        return r;
    }

    private static double ancientCombat$bodyRadius(LivingEntity e) {
        return Math.max(e.getBbWidth() * 0.5, 0.25);
    }

    private static double ancientCombat$closeFactor(NpcBase npc) {
        float h = npc.getBbHeight();
        if (h <= CompatConfig.TINY_HEIGHT.get()) return CompatConfig.CLOSE_FACTOR_TINY.get();
        if (h <= CompatConfig.SMALL_HEIGHT.get()) return CompatConfig.CLOSE_FACTOR_SMALL.get();
        return CompatConfig.CLOSE_FACTOR_NORMAL.get();
    }

    private static void ancientCombat$faceTarget(NpcBase npc, LivingEntity target, boolean aggressive) {
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() * 0.5;
        double tz = target.getZ();

        npc.getLookControl().setLookAt(tx, ty, tz, 60.0F, 60.0F);

        double dx = tx - npc.getX();
        double dz = tz - npc.getZ();
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
        float delta = Mth.wrapDegrees(desiredYaw - npc.getYRot());

        float maxStep = (float) CompatConfig.BODY_TURN_SPEED.get().doubleValue();
        if (aggressive) maxStep *= 1.5F;

        float step = Mth.clamp(delta, -maxStep, maxStep);
        npc.setYRot(npc.getYRot() + step);
        npc.yBodyRot = npc.getYRot();
        npc.yHeadRot = npc.getYRot();
    }

    /** Body yaw drives BC hitbox orientation. */
    private static boolean ancientCombat$isBodyFacingTarget(NpcBase npc, LivingEntity target, float maxYawError) {
        double dx = target.getX() - npc.getX();
        double dz = target.getZ() - npc.getZ();
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
        return Math.abs(Mth.wrapDegrees(npc.yBodyRot - desiredYaw)) <= maxYawError;
    }
}