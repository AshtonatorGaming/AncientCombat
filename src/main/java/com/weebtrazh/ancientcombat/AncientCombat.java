package com.weebtrazh.ancientcombat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AncientCombat.MOD_ID)
public class AncientCombat {
    public static final String MOD_ID = "ancientcombat";
    public static final Logger LOGGER = LogManager.getLogger("AncientCombat");

    public AncientCombat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        }
        LOGGER.info("Ancient Combat loaded — BC melee for AW NPCs; independent of Ancient Tactics");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class.forName("traben.entity_model_features.EMFAnimationApi");
                AcNpcEmfHook.register();
            } catch (Throwable t) {
                LOGGER.debug("EMF not present, NPC animation hook skipped");
            }
        });
    }
}