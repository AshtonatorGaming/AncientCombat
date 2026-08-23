package com.weebtrazh.ancientcombat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {

    private static boolean emfResolved;
    private static Method getEmfEntityMethod;
    private static Method isAnimatingMethod;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
                Method register = api.getMethod(
                        "registerSingletonAnimationVariable",
                        String.class, String.class, String.class, Supplier.class
                );

                Supplier<Boolean> supplier = FACompatHandler::isAwNpcAnimatorActive;

                register.invoke(null,
                        AncientCombat.MOD_ID,
                        "is_playeranimator_active",
                        "PlayerAnimator active on entity (including AW NPCs)",
                        supplier
                );
                register.invoke(null,
                        AncientCombat.MOD_ID,
                        "is_playeranimator_animation_active",
                        "PlayerAnimator animation active (including AW NPCs)",
                        supplier
                );

                AncientCombat.LOGGER.info("Registered FA/MPA EMF variables for AW NPCs (namespace={})", AncientCombat.MOD_ID);
            } catch (Exception e) {
                AncientCombat.LOGGER.debug("FA/EMF not present or failed to register: {}", e.toString());
            }
        });
    }

    private static boolean isAwNpcAnimatorActive() {
        try {
            resolveEmf();
            if (getEmfEntityMethod == null || isAnimatingMethod == null) return false;

            Object emfEntity = getEmfEntityMethod.invoke(null);
            if (!(emfEntity instanceof Entity entity)) return false;
            if (!(entity instanceof NpcBase)) return false;
            if (!(entity instanceof LivingEntity living)) return false;

            return (boolean) isAnimatingMethod.invoke(null, living);
        } catch (Exception e) {
            return false;
        }
    }

    private static void resolveEmf() {
        if (emfResolved) return;
        emfResolved = true;
        try {
            Class<?> ctx = Class.forName("traben.entity_model_features.models.animation.EMFAnimationEntityContext");
            getEmfEntityMethod = ctx.getMethod("getEMFEntity");
            Class<?> helper = Class.forName("me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper");
            isAnimatingMethod = helper.getMethod("isAnimating", LivingEntity.class);
        } catch (Throwable t) {
            getEmfEntityMethod = null;
            isAnimatingMethod = null;
        }
    }
}