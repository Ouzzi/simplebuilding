package com.simplebuilding.datafix;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin config plugin of {@code simplebuilding.mixins.json} and {@code simplebuilding.forge.mixins.json}: where
 * SimpleLib resolves moved ids ({@link IdMovesHook#LIB_ALIASES}, 26.3), the mod's own registry alias mixins are not
 * applied - one alias layer, not two. Where Simple Ovens owns the furnaces ({@code McVersion.OVENS_IN_SB} false), the
 * furnace menu mixin is Simple Ovens' and SimpleBuilding's copy stays out; likewise the copper chests' upgrade click where
 * Simple Storage owns the chests ({@code McVersion.STORAGE_IN_SB} false). Every other mixin applies as before.
 */
public final class AliasMixinPlugin implements IMixinConfigPlugin {
    static final Set<String> OWN_ALIAS_MIXINS = Set.of(
            "com.simplebuilding.mixin.MappedRegistryAliasMixin",
            "com.simplebuilding.mixin.forge.NamespacedWrapperAliasMixin");
    static final Set<String> OVENS_MIXINS = Set.of("com.simplebuilding.mixin.AbstractFurnaceMenuMixin");
    /**
     * Split stage 8b: the copper chests' upgrade click is Simple Storage's ({@code ChestUpgradeClickMixin}) where it owns the
     * chests; 8c-1: likewise the quivers' bow, crossbow and arrow pickup hooks and the deposit into a placed bundle (Simple
     * Storage brings its own; two redirects on the same call would clash).
     */
    static final Set<String> STORAGE_MIXINS = Set.of("com.simplebuilding.mixin.ChestBlockMixin",
            "com.simplebuilding.mixin.BowItemMixin", "com.simplebuilding.mixin.CrossbowItemMixin",
            "com.simplebuilding.mixin.AbstractArrowPickupMixin", "com.simplebuilding.mixin.ItemStackUseOnMixin");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Split stage 4: where Simple Ovens owns the furnaces (26.3) it also brings the furnace menu mixin. The constant
        // is inlined by javac, so no game class loads here.
        if (!com.simplebuilding.version.McVersion.OVENS_IN_SB && OVENS_MIXINS.contains(mixinClassName)) return false;
        if (!com.simplebuilding.version.McVersion.STORAGE_IN_SB && STORAGE_MIXINS.contains(mixinClassName)) return false;
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
