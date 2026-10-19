package com.simplebuilding.modules.simpleoptserver.fabric;

import com.simplebuilding.modules.simpleoptserver.ServerOpt;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Loader adapter like Simple Opt Render's: decides before game classes load, applies area mixins, runs the target check. */
public final class ServerOptMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {
        FabricLoader loader = FabricLoader.getInstance();
        ServerOpt.boot(loader.getConfigDir(), loader::isModLoaded);
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return ServerOpt.RUNTIME.shouldApply(targetClassName, mixinClassName);
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        ServerOpt.RUNTIME.scan(OptTargetScan.targetsByMod(), OptTargetScan::ignored, ServerOpt.LOG::warn);
    }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
