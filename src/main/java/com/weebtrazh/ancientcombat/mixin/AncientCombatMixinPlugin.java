package com.weebtrazh.ancientcombat.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Drops the EMF Compat mixin when that mod is not installed, so Ancient Combat still loads
 * and NPCs keep the plain Better Combat path.
 */
public class AncientCombatMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        LoadingModList list = LoadingModList.get();
        if (mixinClassName.endsWith("PlayerModelNpcAnimatorMixin")) {
            return loaded(list, "playeranimator");
        }
        if (mixinClassName.endsWith("EmfPlayerModelNpcMixin")) {
            return loaded(list, "emf_compat_better_combat");
        }
        if (mixinClassName.endsWith("PoseManagerNpcCleanupMixin")) {
            return loaded(list, "emf_compat_core");
        }
        // Better Mob Combat already puts the player stack on every mob. A second stack on NpcBase
        // would register the same layers twice. Without it, NpcPlayerStackMixin is the player stack.
        if (mixinClassName.endsWith("NpcPlayerStackMixin") || mixinClassName.endsWith("NpcPlayerTickMixin")) {
            return loaded(list, "bettercombat")
                    && loaded(list, "playeranimator")
                    && !loaded(list, "bettermobcombat");
        }
        return true;
    }

    private static boolean loaded(LoadingModList list, String modId) {
        return list != null && list.getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
