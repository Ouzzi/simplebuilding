package com.simplebuilding.modules.simpleoptrender.fabric;

import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Loader adapter (concept §4.2): decides every area before game classes load, with Fabric's mod list
 * ({@code FabricLoader.isModLoaded}, no class lookups on other mods), and applies a mixin only while its area is on.
 * {@code acceptTargets} runs the target check (§4.5) once every mixin config is prepared ({@link OptTargetScan}).
 * A mixin that does not apply switches its area off ({@link MixinGuard}, §4.4); it never crashes the game.
 */
public final class RenderOptMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {
        FabricLoader loader = FabricLoader.getInstance();
        RenderOpt.boot(loader.getConfigDir(), loader::isModLoaded);
        Mixins.registerErrorHandlerClass(MixinGuard.Handler.class.getName());
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return RenderOpt.RUNTIME.shouldApply(targetClassName, mixinClassName);
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        RenderOpt.RUNTIME.scan(OptTargetScan.targetsByMod(), OptTargetScan::ignored, RenderOpt.LOG::warn);
    }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        MixinGuard.check(targetClass, mixinClassName, mixinInfo);
    }
}
