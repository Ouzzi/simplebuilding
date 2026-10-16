package com.simplebuilding.modules.simpleblocks.forge;

/** Adds the Ancient City pool once the loot tables are loaded (Forge's LootTableLoadEvent comes too early). */
public final class ModuleLoot {
    public static void inject(net.minecraft.core.HolderLookup.Provider context, net.minecraft.core.RegistryAccess.Frozen loaded) {
        loaded.lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE).listElements()
                .forEach(holder -> com.simpleblocks.BlocksLoot.apply(holder.key(), pool -> holder.value().addPool(pool.build())));
    }

    private ModuleLoot() {}
}
