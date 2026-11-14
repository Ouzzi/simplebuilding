package com.simplebuilding.modules.simpleoptmemory.fabric;

import com.simplebuilding.modules.simpleoptmemory.MemoryOpt;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Loader adapter like Simple Opt Render's: decides before game classes load, applies area mixins, runs the target check. */
public final class MemoryOptMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {
        FabricLoader loader = FabricLoader.getInstance();
        MemoryOpt.boot(loader.getConfigDir(), loader::isModLoaded);
        // simpleoptmemory-2 (PLAN40): like Simple Opt Render, a mixin that does not apply switches its area off and visible (never a crash).
        org.spongepowered.asm.mixin.Mixins.registerErrorHandlerClass(MixinGuard.Handler.class.getName());
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return MemoryOpt.RUNTIME.shouldApply(targetClassName, mixinClassName);
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        MemoryOpt.RUNTIME.scan(OptTargetScan.targetsByMod(), OptTargetScan::ignored, MemoryOpt.LOG::warn);
    }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        MixinGuard.check(targetClass, mixinClassName, mixinInfo);
    }
}
