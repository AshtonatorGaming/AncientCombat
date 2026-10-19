package com.weebtrazh.ancientcombat.mixin;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.bettercombat.BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.animation.AnimationRegistry;
import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.bettercombat.client.animation.CustomAnimationPlayer;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.client.animation.PoseSubStack;
import net.bettercombat.client.animation.StateCollectionHelper;
import net.bettercombat.client.animation.modifier.HarshAdjustmentModifier;
import net.bettercombat.client.animation.modifier.TransmissionSpeedModifier;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import com.weebtrazh.ancientcombat.AncientCombat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import com.weebtrazh.ancientcombat.NpcClientAttacker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The client player animation stack, on {@link NpcBase}, used when Better Mob Combat is not
 * installed. Same layers Better Combat registers on {@code AbstractClientPlayer}: pose layers
 * 1–4 and the attack layer at 2000. Player Animator then plays them, and EMF Compat snapshots
 * them through {@link EmfPlayerModelNpcMixin}. Applied only in that case; with Better Mob Combat
 * loaded the mob already has this stack and a second copy would fight it.
 */
@Mixin(NpcBase.class)
public abstract class NpcPlayerStackMixin extends PathfinderMob implements IAnimatedPlayer, PlayerAttackAnimatable, NpcClientAttacker {
    @Unique
    private final Map<ResourceLocation, IAnimation> ancientcombat$modAnimationData = new HashMap<>();
    @Unique
    private AnimationStack ancientcombat$stack;
    @Unique
    private AnimationApplier ancientcombat$applier;
    @Unique
    private AttackAnimationSubStack ancientcombat$attackAnimation;
    @Unique
    private PoseSubStack ancientcombat$mainHandBodyPose;
    @Unique
    private PoseSubStack ancientcombat$mainHandItemPose;
    @Unique
    private PoseSubStack ancientcombat$offHandBodyPose;
    @Unique
    private PoseSubStack ancientcombat$offHandItemPose;
    @Unique
    private boolean ancientcombat$layersAdded;
    @Unique
    private int ancientcombat$combo;
    @Unique
    private long ancientcombat$lastSwingGameTime = Long.MIN_VALUE;
    @Unique
    private Item ancientcombat$lastItem;

    protected NpcPlayerStackMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ancientcombat$init(EntityType<?> type, Level level, CallbackInfo ci) {
        if (!this.level().isClientSide) return;
        ancientcombat$installLayers();
    }

    @Override
    public void ancientcombat$clientTick() {
        if (!this.level().isClientSide || !ancientcombat$layersAdded) return;
        getAnimationStack().tick();
        updateAnimationsOnTick();
    }

    /**
     * Better Mob Combat's attack packet is what normally calls {@code playAttackAnimation}.
     * Without that mod, the vanilla swing the server already syncs is the same signal players
     * turn into a Better Combat clip.
     */
    @Override
    public void ancientcombat$clientSwing(InteractionHand hand) {
        if (!this.level().isClientSide || !ancientcombat$layersAdded) return;
        ancientcombat$playSwing(hand);
    }

    @Unique
    private void ancientcombat$installLayers() {
        if (ancientcombat$layersAdded) return;
        ancientcombat$mainHandItemPose = new PoseSubStack(null, false, true);
        ancientcombat$offHandItemPose = new PoseSubStack(null, false, true);
        ancientcombat$offHandBodyPose = new PoseSubStack(null, true, false);
        ancientcombat$mainHandBodyPose = new PoseSubStack(ancientcombat$createPoseAdjustment(), true, true);
        ancientcombat$attackAnimation = new AttackAnimationSubStack(ancientcombat$createAttackAdjustment());

        AnimationStack stack = getAnimationStack();
        stack.addAnimLayer(1, ancientcombat$offHandItemPose.base);
        stack.addAnimLayer(2, ancientcombat$offHandBodyPose.base);
        stack.addAnimLayer(3, ancientcombat$mainHandItemPose.base);
        stack.addAnimLayer(4, ancientcombat$mainHandBodyPose.base);
        stack.addAnimLayer(2000, ancientcombat$attackAnimation.base);
        ancientcombat$mainHandBodyPose.configure = this::ancientcombat$updateAnimationByCurrentActivity;
        ancientcombat$offHandBodyPose.configure = this::ancientcombat$updateAnimationByCurrentActivity;
        ancientcombat$layersAdded = true;
    }

    @Override
    public AnimationStack getAnimationStack() {
        if (ancientcombat$stack == null) {
            ancientcombat$stack = new AnimationStack();
            ancientcombat$applier = new AnimationApplier(ancientcombat$stack);
        }
        return ancientcombat$stack;
    }

    @Override
    public AnimationApplier playerAnimator_getAnimation() {
        getAnimationStack();
        return ancientcombat$applier;
    }

    @Override
    public IAnimation playerAnimator_getAnimation(ResourceLocation location) {
        return ancientcombat$modAnimationData.get(location);
    }

    @Override
    public IAnimation playerAnimator_setAnimation(ResourceLocation location, IAnimation animation) {
        if (animation == null) return ancientcombat$modAnimationData.remove(location);
        return ancientcombat$modAnimationData.put(location, animation);
    }

    @Override
    public void updateAnimationsOnTick() {
        if (!this.level().isClientSide || !ancientcombat$layersAdded) return;
        boolean leftHanded = this.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        ItemStack mainHand = this.getMainHandItem();
        // Same gates as AbstractClientPlayerEntityMixin. Spell-casting is a Player-only check there.
        if (this.swinging
                || this.isSwimming()
                || this.isUsingItem()
                || CrossbowItem.isCharged(mainHand)) {
            ancientcombat$clearPoses(leftHanded);
            return;
        }

        KeyframeAnimation mainPose = null;
        WeaponAttributes mainAttributes = WeaponRegistry.getAttributes(mainHand);
        if (mainAttributes != null && mainAttributes.pose() != null) {
            mainPose = AnimationRegistry.animations.get(mainAttributes.pose());
        }

        KeyframeAnimation offPose = null;
        if (ancientcombat$isDualWielding()) {
            WeaponAttributes offAttributes = WeaponRegistry.getAttributes(this.getOffhandItem());
            if (offAttributes != null && offAttributes.offHandPose() != null) {
                offPose = AnimationRegistry.animations.get(offAttributes.offHandPose());
            }
        }
        ancientcombat$mainHandItemPose.setPose(mainPose, leftHanded);
        ancientcombat$offHandItemPose.setPose(offPose, leftHanded);

        // One-handed weapons drop the body pose while moving, so idle walk is not a combat stance.
        // An empty hand has no pose() and stays on the normal idle.
        if (!ancientcombat$isTwoHanded() && (ancientcombat$isWalking() || this.isShiftKeyDown())) {
            mainPose = null;
            offPose = null;
        }
        ancientcombat$mainHandBodyPose.setPose(mainPose, leftHanded);
        ancientcombat$offHandBodyPose.setPose(offPose, leftHanded);
    }

    @Override
    public void playAttackAnimation(String name, AnimatedHand animatedHand, float length, float upswing) {
        if (!this.level().isClientSide || !ancientcombat$layersAdded || name == null || length <= 0.0F) return;
        try {
            KeyframeAnimation animation = AnimationRegistry.animations.get(name);
            if (animation == null) return;
            KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
            ancientcombat$updateAnimationByCurrentActivity(copy);
            copy.torso.fullyEnablePart(true);
            copy.head.pitch.setEnabled(false);
            float speed = animation.endTick / length;
            boolean mirror = animatedHand.isOffHand();
            if (this.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT) {
                mirror = !mirror;
            }
            float upswingMultiplier = ancientcombat$configFloat("upswing_multiplier", 0.5F);
            float upswingSpeed = speed / Math.max(upswingMultiplier, 0.05F);
            float tail = upswing >= 0.999F ? 1.0F : upswing / (1.0F - upswing);
            float downwindSpeed = (float) (speed * Mth.lerp(
                    (float) (Math.max(upswingMultiplier - 0.5F, 0.0F) / 0.5F),
                    1.0F - upswing,
                    tail));
            ancientcombat$attackAnimation.speed.set(upswingSpeed, List.of(
                    new TransmissionSpeedModifier.Gear(length * upswing, downwindSpeed),
                    new TransmissionSpeedModifier.Gear(length, speed)
            ));
            ancientcombat$attackAnimation.mirror.setEnabled(mirror);
            CustomAnimationPlayer player = new CustomAnimationPlayer(copy.build(), 0);
            player.setFirstPersonMode(FirstPersonMode.NONE);
            player.setFirstPersonConfiguration(new FirstPersonConfiguration(false, false, false, false));
            ancientcombat$attackAnimation.base.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(copy.beginTick, Ease.INOUTSINE),
                    player);
        } catch (Throwable t) {
            AncientCombat.LOGGER.debug("NPC attack animation failed: {}", t.toString());
        }
    }

    @Override
    public void stopAttackAnimation(float length) {
        if (!this.level().isClientSide || ancientcombat$attackAnimation == null) return;
        IAnimation current = ancientcombat$attackAnimation.base.getAnimation();
        if (current instanceof KeyframeAnimationPlayer) {
            int fadeOut = Math.round(length);
            ancientcombat$attackAnimation.adjustmentModifier.fadeOut(fadeOut);
            ancientcombat$attackAnimation.base.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(fadeOut, Ease.INOUTSINE), null);
        }
    }

    @Unique
    private void ancientcombat$playSwing(InteractionHand hand) {
        ItemStack stack = hand == InteractionHand.OFF_HAND ? this.getOffhandItem() : this.getMainHandItem();
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null || attributes.attacks() == null || attributes.attacks().length == 0) return;
        int index = ancientcombat$nextAttackIndex(stack, attributes.attacks().length);
        WeaponAttributes.Attack attack = attributes.attacks()[index];
        if (attack == null || attack.animation() == null || attack.animation().isEmpty()) return;
        AnimatedHand animatedHand = AnimatedHand.from(hand == InteractionHand.OFF_HAND, attributes.isTwoHanded());
        playAttackAnimation(attack.animation(), animatedHand, ancientcombat$attackLengthTicks(), (float) attack.upswing());
    }

    @Unique
    private int ancientcombat$nextAttackIndex(ItemStack stack, int count) {
        Item item = stack.getItem();
        long now = this.level().getGameTime();
        float resetRate = ancientcombat$configFloat("combo_reset_rate", 2.0F);
        long window = (long) Math.max(1.0F, ancientcombat$attackLengthTicks() * resetRate);
        if (item != ancientcombat$lastItem || now - ancientcombat$lastSwingGameTime > window) {
            ancientcombat$combo = 0;
        }
        ancientcombat$lastItem = item;
        ancientcombat$lastSwingGameTime = now;
        int index = ancientcombat$combo % count;
        ancientcombat$combo++;
        return index;
    }

    /**
     * {@code ServerConfig} implements a Cloth Config type that is not on this compile classpath.
     * The fields are public; reading them by name keeps the player upswing and combo settings.
     */
    @Unique
    private static float ancientcombat$configFloat(String name, float fallback) {
        try {
            Object config = BetterCombat.class.getField("config").get(null);
            if (config == null) return fallback;
            return config.getClass().getField(name).getFloat(config);
        } catch (Throwable t) {
            return fallback;
        }
    }

    @Unique
    private static int ancientcombat$configInt(String name, int fallback) {
        try {
            Object config = BetterCombat.class.getField("config").get(null);
            if (config == null) return fallback;
            return config.getClass().getField(name).getInt(config);
        } catch (Throwable t) {
            return fallback;
        }
    }

    @Unique
    private float ancientcombat$attackLengthTicks() {
        float ticks = 10.0F;
        var attribute = this.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute != null && attribute.getValue() > 0.0) {
            ticks = (float) (20.0 / attribute.getValue());
        }
        int cap = ancientcombat$configInt("attack_interval_cap", 0);
        return Math.max(ticks, cap);
    }

    @Unique
    private void ancientcombat$clearPoses(boolean leftHanded) {
        ancientcombat$mainHandBodyPose.setPose(null, leftHanded);
        ancientcombat$mainHandItemPose.setPose(null, leftHanded);
        ancientcombat$offHandBodyPose.setPose(null, leftHanded);
        ancientcombat$offHandItemPose.setPose(null, leftHanded);
    }

    @Unique
    private boolean ancientcombat$isTwoHanded() {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(this.getMainHandItem());
        return attributes != null && attributes.isTwoHanded();
    }

    @Unique
    private boolean ancientcombat$isDualWielding() {
        WeaponAttributes main = WeaponRegistry.getAttributes(this.getMainHandItem());
        WeaponAttributes off = WeaponRegistry.getAttributes(this.getOffhandItem());
        return main != null && off != null && !main.isTwoHanded() && !off.isTwoHanded();
    }

    @Unique
    private boolean ancientcombat$isWalking() {
        return !this.isDeadOrDying()
                && (this.isSwimming() || this.getDeltaMovement().horizontalDistance() > 0.03);
    }

    @Unique
    private AdjustmentModifier ancientcombat$createAttackAdjustment() {
        LivingEntity entity = this;
        return new AdjustmentModifier(partName -> {
            float pitch = (float) Math.toRadians(entity.getXRot());
            float rotationX = 0.0F;
            float offsetY = 0.0F;
            float offsetZ = 0.0F;
            if (FirstPersonMode.isFirstPersonPass()) {
                if (!"body".equals(partName)) return Optional.empty();
                rotationX -= pitch;
                if (pitch < 0.0F) {
                    double offset = Math.abs(Math.sin(pitch));
                    offsetY += (float) (offset * 0.5);
                    offsetZ -= (float) offset;
                }
            } else {
                switch (partName) {
                    case "body" -> rotationX -= pitch * 0.75F;
                    case "rightArm", "leftArm" -> rotationX += pitch * 0.25F;
                    case "rightLeg", "leftLeg" -> rotationX -= (float) (pitch * 0.75);
                    default -> {
                        return Optional.empty();
                    }
                }
            }
            return Optional.of(new AdjustmentModifier.PartModifier(
                    new Vec3f(rotationX, 0.0F, 0.0F),
                    new Vec3f(0.0F, offsetY, offsetZ)));
        });
    }

    @Unique
    private AdjustmentModifier ancientcombat$createPoseAdjustment() {
        return new HarshAdjustmentModifier(partName -> {
            if (FirstPersonMode.isFirstPersonPass()) return Optional.empty();
            if (!"rightArm".equals(partName) && !"leftArm".equals(partName)) return Optional.empty();
            float offsetY = 0.0F;
            if (!ancientcombat$mainHandItemPose.lastAnimationUsesBodyChannel && this.isShiftKeyDown()) {
                offsetY += 3.0F;
            }
            return Optional.of(new AdjustmentModifier.PartModifier(
                    new Vec3f(0.0F, 0.0F, 0.0F),
                    new Vec3f(0.0F, offsetY, 0.0F)));
        });
    }

    @Unique
    private void ancientcombat$updateAnimationByCurrentActivity(KeyframeAnimation.AnimationBuilder animation) {
        if (this.getPose() == Pose.SWIMMING || this.getVehicle() != null) {
            StateCollectionHelper.configure(animation.rightLeg, false, false);
            StateCollectionHelper.configure(animation.leftLeg, false, false);
        }
    }
}
