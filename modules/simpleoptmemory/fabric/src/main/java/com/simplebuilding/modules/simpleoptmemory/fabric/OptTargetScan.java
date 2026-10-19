package com.simplebuilding.modules.simpleoptmemory.fabric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

/**
 * Fabric side of the target check (concept §4.5): the target classes of every prepared mixin config, by the mod that
 * registered it (Fabric decorates each config with its mod id). Our own family never counts as a conflict: mixin package
 * {@code com.simplebuilding.*} or {@code com.<simple-mod-id>.*}, or Ouzzi/azarfane in the mod's authors or contact.
 * The loader, Fabric API and MixinExtras are ignored too. Same helper in each Simple Optimization sub-mod, since
 * sub-mods share no classes.
 */
final class OptTargetScan {
    private OptTargetScan() {}

    static Map<String, Set<String>> targetsByMod() {
        Map<String, Set<String>> out = new HashMap<>();
        for (Config c : Mixins.getConfigs()) {
            IMixinConfig config = c.getConfig();
            if (config == null) continue;
            String mod = FabricUtil.getModId(config, "unknown");
            if (family(mod, config.getMixinPackage())) continue;
            out.computeIfAbsent(mod, k -> new HashSet<>()).addAll(config.getTargets());
        }
        return out;
    }

    static boolean ignored(String modId) {
        return modId.startsWith("fabric") || modId.equals("minecraft") || modId.equals("java") || modId.equals("mixinextras");
    }

    private static boolean family(String modId, String mixinPackage) {
        String pkg = mixinPackage == null ? "" : mixinPackage;
        if (pkg.startsWith("com.simplebuilding.") || modId.startsWith("simple") && pkg.startsWith("com." + modId + ".")) return true;
        return FabricLoader.getInstance().getModContainer(modId).map(m -> {
            var meta = m.getMetadata();
            String who = meta.getAuthors().toString() + meta.getContact().asMap();
            return who.contains("Ouzzi") || who.contains("azarfane");
        }).orElse(false);
    }
}
