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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(NpcAIAttackMeleeLongRange.class)
public abstract class NpcAIAttackMeleeLongRangeMixin {

    private static final float MAX_YAW_ERROR = 30.0F;
    private static final float MAX_YAW_ERROR_CLOSE = 60.0F;
    private static final float MAX_YAW_ERROR_SMALL = 40.0F;
    private static final float BODY_TURN_SPEED = 16.0F;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientCombat$doAttack(double distanceToEntity, CallbackInfo ci) {
        NpcAIAttackMeleeLongRange self = (NpcAIAttackMeleeLongRange) (Object) this;
        NpcBase npc = ancientCombat$getNpc(self);
        if (npc == null) {
            return;
        }

        // Fists / non-BC: do nothing. AW runs full doAttack (anim + hurt + getCoolDown).
        if (!BcWeapon.hasCompatibleWeapon(npc)) {
            return;
        }

        WeaponAttributes attributes = BcWeapon.getAttributes(npc);
        if (attributes == null) {
            return;
        }

        // BC weapon only: replace entire AW attack this tick
        ci.cancel();

        Mob mob = (Mob) npc;
        LivingEntity target = self.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        ancientCombat$faceTarget(npc, target);

        if (self.getAttackDelay() > 0) {
            return;
        }

        if (!MobCombatHelper.isAttackReady(mob)) {
            self.setAttackDelay(1);
            return;
        }

        double dist = npc.distanceTo(target);
        double reach = ancientCombat$effectiveReach(npc, attributes);
        double maxDist = ancientCombat$bodyRadius(npc) + ancientCombat$bodyRadius(target) + reach;

        if (dist > maxDist) {
            self.setAttackDelay(1);
            return;
        }

        float yawLimit = ancientCombat$yawLimit(npc, dist, maxDist);
        if (!ancientCombat$isFacingTarget(npc, target, yawLimit)) {
            self.setAttackDelay(1);
            return;
        }

        ((MobAttackWindup) mob).bettermobcombat$startUpswing(attributes, null);
        self.setAttackDelay(1);
    }

    @Inject(method = "shouldCloseOnTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientCombat$shouldClose(double distanceToEntitySq, CallbackInfoReturnable<Boolean> cir) {
        NpcAIAttackMeleeLongRange self = (NpcAIAttackMeleeLongRange) (Object) this;
        NpcBase npc = ancientCombat$getNpc(self);
        if (npc == null) {
            return;
        }

        if (!BcWeapon.hasCompatibleWeapon(npc)) {
            return;
        }

        WeaponAttributes attributes = BcWeapon.getAttributes(npc);
        if (attributes == null) {
            return;
        }

        LivingEntity target = self.getTarget();
        if (target == null) {
            cir.setReturnValue(true);
            return;
        }

        ancientCombat$faceTarget(npc, target);

        double reach = ancientCombat$effectiveReach(npc, attributes);
        double factor = ancientCombat$closeFactor(npc);
        double closeEnough = ancientCombat$bodyRadius(npc) + ancientCombat$bodyRadius(target) + reach * factor;

        boolean needClose = distanceToEntitySq > closeEnough * closeEnough
                || !npc.getSensing().hasLineOfSight(target);

        cir.setReturnValue(needClose);
    }

    private static double ancientCombat$effectiveReach(NpcBase npc, WeaponAttributes attributes) {
        double r = attributes.attackRange();
        if (r <= 0.1) {
            r = 1.75;
        }

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
        if (h <= CompatConfig.TINY_HEIGHT.get()) {
            return CompatConfig.CLOSE_FACTOR_TINY.get();
        }
        if (h <= CompatConfig.SMALL_HEIGHT.get()) {
            return CompatConfig.CLOSE_FACTOR_SMALL.get();
        }
        return CompatConfig.CLOSE_FACTOR_NORMAL.get();
    }

    private static float ancientCombat$yawLimit(NpcBase npc, double dist, double maxDist) {
        if (dist < maxDist * 0.5) {
            return MAX_YAW_ERROR_CLOSE;
        }
        if (npc.getBbHeight() <= CompatConfig.SMALL_HEIGHT.get()) {
            return MAX_YAW_ERROR_SMALL;
        }
        return MAX_YAW_ERROR;
    }

    private static void ancientCombat$faceTarget(NpcBase npc, LivingEntity target) {
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() * 0.5;
        double tz = target.getZ();

        npc.getLookControl().setLookAt(tx, ty, tz, 60.0F, 60.0F);

        double dx = tx - npc.getX();
        double dz = tz - npc.getZ();
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
        float delta = Mth.wrapDegrees(desiredYaw - npc.getYRot());
        float step = Mth.clamp(delta, -BODY_TURN_SPEED, BODY_TURN_SPEED);
        npc.setYRot(npc.getYRot() + step);
        npc.yBodyRot = npc.getYRot();
    }

    private static boolean ancientCombat$isFacingTarget(NpcBase npc, LivingEntity target, float maxYawError) {
        double dx = target.getX() - npc.getX();
        double dz = target.getZ() - npc.getZ();
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
        return Math.abs(Mth.wrapDegrees(npc.yHeadRot - desiredYaw)) <= maxYawError;
    }

    private static NpcBase ancientCombat$getNpc(NpcAIAttackMeleeLongRange self) {
        try {
            Class<?> c = self.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("npc");
                    f.setAccessible(true);
                    Object v = f.get(self);
                    if (v instanceof NpcBase) {
                        return (NpcBase) v;
                    }
                } catch (NoSuchFieldException ignored) {
                }
                c = c.getSuperclass();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}