package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.CompatConfig;
import com.weebtrazh.ancientcombat.FACompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(value = PlayerModel.class, priority = 2502)
public abstract class PlayerModelNpcEmfCompatMixin {

    private static final String SOURCE = "better_combat";
    private static final String POSE_SOURCE = "better_combat_pose";

    private static boolean resolved;
    private static Method savePoses;
    private static Method clearPoses;
    private static Constructor<?> snapshotCtor;
    private static Constructor<?> snapshotRotOnly;
    private static Method compatEnabled;
    private static Method compatBodyFollow;

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

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ancientcombat$captureTwoHand(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                              float ageInTicks, float netHeadYaw, float headPitch,
                                              CallbackInfo ci) {
        if (!CompatConfig.ENABLE_EMF_NPC_COMPAT.get()) return;
        if (!(entity instanceof NpcBase)) return;

        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> living)) return;
        if (living.getModel() != (Object) this) return;

        resolve();
        if (savePoses == null) return;

        UUID uuid = entity.getUUID();
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;

        if (!enabled()) {
            clear(uuid, SOURCE);
            clear(uuid, POSE_SOURCE);
            return;
        }

        clear(uuid, POSE_SOURCE);

        if (!FACompatHandler.needsTwoHandSnapshot(entity.getMainHandItem())) {
            clear(uuid, SOURCE);
            return;
        }

        Vector3f bodyBase = bodyFollow()
                ? new Vector3f(model.body.x, model.body.y, model.body.z)
                : null;

        try {
            savePoses.invoke(null, uuid, SOURCE,
                    snapshot(model.leftArm, false),
                    snapshot(model.rightArm, false),
                    null,
                    bodyBase);
        } catch (Throwable ignored) {
        }
    }

    private static Object snapshot(ModelPart part, boolean rotationOnly) throws Exception {
        if (rotationOnly && snapshotRotOnly != null) {
            return snapshotRotOnly.newInstance(part, Boolean.TRUE);
        }
        return snapshotCtor.newInstance(part);
    }

    private static void clear(UUID uuid, String source) {
        if (clearPoses == null) return;
        try {
            clearPoses.invoke(null, uuid, source);
        } catch (Throwable ignored) {
        }
    }

    private static boolean enabled() {
        return flag(compatEnabled, true);
    }

    private static boolean bodyFollow() {
        return flag(compatBodyFollow, true);
    }

    private static boolean flag(Method m, boolean fallback) {
        if (m == null) return fallback;
        try {
            return Boolean.TRUE.equals(m.invoke(null));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> mod = Class.forName("strm.emfcompat.bettercombat.EMFCompatBetterCombatMod");
            compatEnabled = mod.getMethod("isEnabled");
            compatBodyFollow = mod.getMethod("isBodyFollow");
        } catch (Throwable ignored) {
        }
        try {
            Class<?> manager = Class.forName("strm.emfcompat.core.PoseManager");
            Class<?> snapshot = Class.forName("strm.emfcompat.core.PoseSnapshot");
            snapshotCtor = snapshot.getConstructor(ModelPart.class);
            try {
                snapshotRotOnly = snapshot.getConstructor(ModelPart.class, boolean.class);
            } catch (Throwable ignored) {
                snapshotRotOnly = null;
            }
            savePoses = manager.getMethod(
                    "savePoses",
                    UUID.class,
                    String.class,
                    snapshot,
                    snapshot,
                    Map.class,
                    Vector3f.class
            );
            clearPoses = manager.getMethod("clearPoses", UUID.class, String.class);
        } catch (Throwable ignored) {
        }
    }
}