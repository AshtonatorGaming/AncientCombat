package com.weebtrazh.ancientcombat.mixin;

import com.weebtrazh.ancientcombat.AncientCombat;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Player Animator writes the emote in {@code setEmote}, and that method returns without touching
 * the model unless the entity is an {@code AbstractClientPlayer}. The cast after that check is to
 * {@code IAnimatedPlayer}, which an NPC with the player stack already is. Priority 2200 runs after
 * Player Animator (2000) has cleared the NPC, and before EMF Compat's capture (2500).
 */
@Mixin(value = PlayerModel.class, priority = 2200)
public abstract class PlayerModelNpcAnimatorMixin {
    private static Field ancientcombat$emoteSupplier;
    private static boolean ancientcombat$emoteSupplierLookup;

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ancientcombat$applyNpcEmote(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                              float ageInTicks, float netHeadYaw, float headPitch,
                                              CallbackInfo ci) {
        if (!(entity instanceof NpcBase)) return;
        if (!(entity instanceof IAnimatedPlayer animated)) return;
        AnimationApplier applier = animated.playerAnimator_getAnimation();
        if (applier == null || !applier.isActive()) return;

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        ancientcombat$setEmoteSupplier(model, applier);
        applier.updatePart("head", model.head);
        model.hat.copyFrom(model.head);
        applier.updatePart("leftArm", model.leftArm);
        applier.updatePart("rightArm", model.rightArm);
        applier.updatePart("leftLeg", model.leftLeg);
        applier.updatePart("rightLeg", model.rightLeg);
        applier.updatePart("torso", model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.jacket.copyFrom(model.body);
    }

    /**
     * The supplier lives on Player Animator's mixin, so it is not a compile-time field of
     * {@code PlayerModel}. Bend bones read it while the parts update.
     */
    @SuppressWarnings("unchecked")
    private static void ancientcombat$setEmoteSupplier(PlayerModel<?> model, AnimationApplier applier) {
        if (!ancientcombat$emoteSupplierLookup) {
            ancientcombat$emoteSupplierLookup = true;
            for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField("emoteSupplier");
                    field.setAccessible(true);
                    ancientcombat$emoteSupplier = field;
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (ancientcombat$emoteSupplier == null) {
                AncientCombat.LOGGER.warn("Player Animator emote supplier not found; NPC bends stay off");
            }
        }
        if (ancientcombat$emoteSupplier == null) return;
        try {
            Object supplier = ancientcombat$emoteSupplier.get(model);
            if (supplier instanceof SetableSupplier<?> setable) {
                ((SetableSupplier<Object>) setable).set(applier);
            }
        } catch (Throwable ignored) {
        }
    }
}
