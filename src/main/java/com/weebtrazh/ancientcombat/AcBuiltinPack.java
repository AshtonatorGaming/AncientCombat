package com.weebtrazh.ancientcombat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = AncientCombat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AcBuiltinPack {

    @SubscribeEvent
    public static void addPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        try {
            Path root = ModList.get().getModFileById(AncientCombat.MOD_ID).getFile()
                    .findResource("resourcepacks/ac_fape_npc");
            Pack pack = Pack.readMetaAndCreate(
                    "ancientcombat_fape_npc",
                    Component.literal("Ancient Combat FAPE NPC"),
                    true,
                    name -> new PathPackResources(name, root, true),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        } catch (Throwable t) {
            AncientCombat.LOGGER.warn("Could not register FAPE NPC pack: {}", t.toString());
        }
    }
}