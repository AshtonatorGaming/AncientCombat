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

@Mod.EventBusSubscriber(modid = "awbettercombatcompat", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FACompatHandler {

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

                // Variable the FA resource pack looks for
                register.invoke(null,
                        "awbettercombatcompat",
                        "is_playeranimator_active",
                        "PlayerAnimator active on entity (including AW NPCs)",
                        supplier
                );

                // Variable Mob Player Animator uses
                register.invoke(null,
                        "awbettercombatcompat",
                        "is_playeranimator_animation_active",
                        "PlayerAnimator animation active (including AW NPCs)",
                        supplier
                );

                System.out.println("[AWBetterCombatCompat] Registered FA/MPA EMF variables for AW NPCs");
            } catch (Exception e) {
                System.err.println("[AWBetterCombatCompat] Failed to register variables: " + e.getMessage());
            }
        });
    }

    private static boolean isAwNpcAnimatorActive() {
        try {
            Class<?> ctx = Class.forName("traben.entity_model_features.models.animation.EMFAnimationEntityContext");
            Method get = ctx.getMethod("getEMFEntity");
            Object emfEntity = get.invoke(null);
            if (emfEntity == null) return false;

            Entity entity = (Entity) emfEntity;
            if (!(entity instanceof NpcBase)) return false;
            if (!(entity instanceof LivingEntity living)) return false;

            Class<?> helper = Class.forName("me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper");
            Method isAnimating = helper.getMethod("isAnimating", LivingEntity.class);
            return (boolean) isAnimating.invoke(null, living);
        } catch (Exception e) {
            return false;
        }
    }
}