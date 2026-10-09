package com.simplebuilding.modules.simplemaps.forge;

/** Adds the map pools once the loot tables are loaded (Forge's LootTableLoadEvent comes too early). */
public final class ModuleLoot {
    public static void inject(net.minecraft.core.HolderLookup.Provider context, net.minecraft.core.RegistryAccess.Frozen loaded) {
        var lookup = net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.concat(context.listRegistries(), loaded.listRegistries()));
        loaded.lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE).listElements()
                .forEach(holder -> com.simplemaps.MapsLoot.apply(holder.key(), pool -> holder.value().addPool(pool.build()), lookup));
    }

    private ModuleLoot() {}
}
