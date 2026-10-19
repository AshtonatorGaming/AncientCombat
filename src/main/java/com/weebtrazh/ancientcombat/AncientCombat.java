package com.weebtrazh.ancientcombat;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AncientCombat.MOD_ID)
public class AncientCombat {
    public static final String MOD_ID = "ancientcombat";
    public static final Logger LOGGER = LogManager.getLogger("AncientCombat");

    public AncientCombat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC);
        LOGGER.info("Ancient Combat loaded — BC melee for AW NPCs; independent of Ancient Tactics");
    }
}