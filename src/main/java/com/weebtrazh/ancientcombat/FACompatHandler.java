package com.weebtrazh.ancientcombat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import me.Thelnfamous1.bettermobcombat.api.MobAttackAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {

    /**
     * NPCs whose arms must beat the FAPE idle clip this frame: an attack, the few frames
     * after one, or a weapon hold pose. Not "this item has Better Combat attributes".
     * An empty hand still has fist attributes, and treating that as a lock left unarmed
     * NPCs stuck in a combat pose.
     */
    private static final Set<UUID> COMBAT_LOCK = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<LivingEntity> EMF_NPC = new ThreadLocal<>();

    private static boolean resolved;
    private static Method getCurrentEntity;

    public static void setEmfNpc(LivingEntity entity) {
        if (entity == null) {
            EMF_NPC.remove();
        } else {
            EMF_NPC.set(entity);
        }
    }

    public static boolean shouldKeepCombatPose(LivingEntity entity) {
        if (entity instanceof Player) return false;
        if (!(entity instanceof NpcBase)) return false;
        return isCombatLocked(entity.getUUID());
    }

    public static boolean isCombatLocked(UUID uuid) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return false;
        return uuid != null && COMBAT_LOCK.contains(uuid);
    }

    private static Method stateUuid;
    private static boolean stateUuidResolved;

    /**
     * {@code EMFEntityRenderState.uuid()} lives on the ETF parent, which is not a compile dep.
     * The lookup is cached; this runs once per animated entity per frame.
     */
    public static UUID uuidFromState(Object state) {
        if (state == null) return null;
        if (!stateUuidResolved) {
            stateUuidResolved = true;
            try {
                stateUuid = state.getClass().getMethod("uuid");
            } catch (Throwable ignored) {
                stateUuid = null;
            }
        }
        if (stateUuid == null) return null;
        try {
            Object value = stateUuid.invoke(state);
            if (value instanceof UUID id) return id;
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Called at the end of {@code PlayerModel.setupAnim}, after Player Animator (priority 2000)
     * and the EMF Compat player mixin (priority 2500). The arm rotations on the model are the
     * Better Combat pose. EMF then overwrites them; {@link EmfCompatBridge} puts them back.
     */
    public static void captureNpcPose(PlayerModel<?> model, NpcBase npc, float limbSwingAmount) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) {
            release(npc);
            return;
        }
        UUID uuid = npc.getUUID();
        boolean armed = isArmed(npc);
        boolean attack = isAttackPlaying(npc);
        // A hold pose on an empty hand is a ghost weapon. Fist swings are attacks, not holds.
        // Hold snapshots are for weapons that actually pose the body (spears, claymores).
        // A one-handed sword keeps EMF's idle arms between swings.
        boolean stance = armed && needsTwoHandSnapshot(npc.getMainHandItem()) && isHoldPosePlaying(npc);

        if (attack || stance) {
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
        }

        if (attack) {
            EmfCompatBridge.markAttack(uuid);
            EmfCompatBridge.saveAttack(uuid, model, limbSwingAmount);
            if (stance) {
                EmfCompatBridge.saveStance(uuid, model);
            } else {
                EmfCompatBridge.clearStance(uuid);
            }
            COMBAT_LOCK.add(uuid);
            return;
        }

        EmfCompatBridge.tickLinger(uuid);
        if (EmfCompatBridge.isLingering(uuid)) {
            if (stance) {
                EmfCompatBridge.saveStance(uuid, model);
            } else {
                EmfCompatBridge.clearStance(uuid);
            }
            COMBAT_LOCK.add(uuid);
            return;
        }

        EmfCompatBridge.clearAttack(uuid);
        if (stance) {
            EmfCompatBridge.saveStance(uuid, model);
            COMBAT_LOCK.add(uuid);
        } else {
            EmfCompatBridge.clearStance(uuid);
            COMBAT_LOCK.remove(uuid);
        }
    }

    private static void release(NpcBase npc) {
        COMBAT_LOCK.remove(npc.getUUID());
        EmfCompatBridge.clearNpc(npc.getUUID());
    }

    /** A real weapon. Empty main hand is unarmed even when Better Combat publishes fist attributes. */
    private static boolean isArmed(NpcBase npc) {
        ItemStack stack = npc.getMainHandItem();
        return stack != null && !stack.isEmpty() && hasCombatAttributes(stack);
    }

    private static boolean isAttackPlaying(LivingEntity entity) {
        return entity instanceof MobAttackAnimation anim && anim.bettermobcombat$hasActiveAttackAnimation();
    }

    private static boolean isHoldPosePlaying(LivingEntity entity) {
        if (!(entity instanceof MobAttackAnimation anim)) return false;
        return anim.bettermobcombat$hasActiveMainHandItemPose()
                || anim.bettermobcombat$hasActiveOffHandItemPose()
                || anim.bettermobcombat$hasActiveMainHandBodyPose()
                || anim.bettermobcombat$hasActiveOffHandBodyPose();
    }

    public static boolean hasCombatAttributes(ItemStack stack) {
        try {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
            if (attributes == null) return false;
            if (attributes.pose() != null) return true;
            WeaponAttributes.Attack[] attacks = attributes.attacks();
            return attacks != null && attacks.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean needsTwoHandSnapshot(ItemStack stack) {
        try {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
            if (attributes == null) return false;
            if (attributes.pose() != null) return true;
            return attributes.isTwoHanded();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean currentEmfEntityNeedsCombatLock() {
        if (getCurrentEntity != null) {
            try {
                Entity e = unwrap(getCurrentEntity.invoke(null));
                if (e instanceof Player) return false;
                if (e instanceof LivingEntity living) {
                    return shouldKeepCombatPose(living);
                }
            } catch (Throwable ignored) {
            }
        }
        LivingEntity local = EMF_NPC.get();
        if (local instanceof Player) return false;
        return local instanceof NpcBase && shouldKeepCombatPose(local);
    }

    public static void captureForEmfCompat(PlayerModel<?> model, LivingEntity entity) {
        if (entity instanceof NpcBase npc) {
            captureNpcPose(model, npc, 0.0F);
        }
    }

    /** Pose restore is {@link EmfCompatBridge}, after EMF's animation. Nothing to re-apply here. */
    public static void applyCapturedArms(HumanoidModel<?> model, LivingEntity entity) {
    }

    public static Entity entityFromEmfState(Object state) {
        if (state == null) return null;
        Entity direct = unwrap(state);
        if (direct != null) return direct;
        try {
            for (Method m : state.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Object v = m.invoke(state);
                Entity e = unwrap(v);
                if (e != null) return e;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Entity unwrap(Object emfEntity) {
        if (emfEntity instanceof Entity e) return e;
        if (emfEntity == null) return null;
        try {
            for (Method m : emfEntity.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (Entity.class.isAssignableFrom(m.getReturnType())) {
                    Object inner = m.invoke(emfEntity);
                    if (inner instanceof Entity e) return e;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * EMF Compat 2.3 lifts its own pause while a pose is saved ({@code PauseOverride}),
     * which is what lets the pack keep the body and legs. Pausing the whole model here
     * brings back the stuck torso twist, so these stay empty on purpose.
     */
    public static void pauseEmfFor(LivingEntity entity) {
    }

    public static void resumeEmfFor(LivingEntity entity) {
    }

    public static void captureHumanoidCombatParts(HumanoidModel<?> human) {
    }

    public static void restoreHumanoidCombatParts(HumanoidModel<?> human) {
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
            getCurrentEntity = api.getMethod("getCurrentEntity");
            Method registerBool = api.getMethod(
                    "registerSingletonAnimationVariable",
                    String.class, String.class, String.class, BooleanSupplier.class
            );
            BooleanSupplier supplier = FACompatHandler::currentEmfEntityNeedsCombatLock;
            registerBool.invoke(null, AncientCombat.MOD_ID, "is_playeranimator_active", "is_playeranimator_active", supplier);
            registerBool.invoke(null, AncientCombat.MOD_ID, "is_playeranimator_animation_active", "is_playeranimator_animation_active", supplier);
            registerBool.invoke(null, AncientCombat.MOD_ID, "is_mob_playeranimator_active", "is_mob_playeranimator_active", supplier);
            AncientCombat.LOGGER.info("AW NPC FAPE vars registered");
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("EMF var register failed: {}", t.toString());
        }
    }

    static {
        resolve();
    }
}