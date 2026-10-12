package com.simplebuilding.datafix;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin config plugin of {@code simplebuilding.mixins.json} and {@code simplebuilding.forge.mixins.json}: where
 * SimpleLib resolves moved ids ({@link IdMovesHook#LIB_ALIASES}, 26.3), the mod's own registry alias mixins are not
 * applied - one alias layer, not two. Every other mixin applies as before.
 */
public final class AliasMixinPlugin implements IMixinConfigPlugin {
    static final Set<String> OWN_ALIAS_MIXINS = Set.of(
            "com.simplebuilding.mixin.MappedRegistryAliasMixin",
            "com.simplebuilding.mixin.forge.NamespacedWrapperAliasMixin");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !(IdMovesHook.LIB_ALIASES && OWN_ALIAS_MIXINS.contains(mixinClassName));
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
