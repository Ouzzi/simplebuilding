package com.simplebuilding.loot;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * DW04C-3 (docs/ai/PLAN-B-SHELVES01-2026-11-04.md): with SimpleBuilding the End bookshelves of Simple End are made of Nihilit
 * Books (recipe {@code simplebuilding:<wood>_bookshelf}), so broken without Silk Touch they give 3 Nihilit Books back - Simple End's
 * own table (Vanilla's bookshelf rule, 3 books) is replaced. Only ids, no Simple End class (module independence, rule 3); the loader
 * hook (Fabric {@code LootTableEvents.REPLACE}) calls {@link #replace}.
 */
public final class EndShelfLoot {
    /** The End woods with a bookshelf in Simple End. */
    public static final List<String> WOODS = List.of("ashwood", "duskwood");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simplebuilding/EndShelfLoot");

    private EndShelfLoot() {
    }

    /** The replacement for {@code simpleend:blocks/<wood>_bookshelf}, else null (keep the table). */
    public static @Nullable LootTable replace(ResourceKey<LootTable> key, HolderLookup.Provider registries) {
        Identifier id = key.identifier();
        if (!"simpleend".equals(id.getNamespace()) || !com.simplebuilding.version.McVersion.ASTRAL_ENCHANTING) return null;
        for (String wood : WOODS) {
            if (id.getPath().equals("blocks/" + wood + "_bookshelf")) {
                return LootTable.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), JsonParser.parseString(json(wood)))
                        .resultOrPartial(LOG::error).orElse(null);
            }
        }
        return null;
    }

    /** Vanilla's bookshelf table with the wood's bookshelf and 3 Nihilit Books (the silk-touch predicate inlined: the predicate
     *  registry is not part of the lookup a replacement is parsed with). */
    static String json(String wood) {
        return """
                {"type": "minecraft:block", "random_sequence": "simpleend:blocks/%1$s_bookshelf", "pools": [{"rolls": 1, "entries": [
                  {"type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "name": "simpleend:%1$s_bookshelf", "condition": {"type": "minecraft:match_tool", "predicate": {"predicates":
                      {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}},
                    {"type": "minecraft:item", "name": "simplebuilding:nihilith_book", "modifier": [
                      {"type": "minecraft:set_count", "count": 3}, {"type": "minecraft:explosion_decay"}]}]}]}]}
                """.formatted(wood);
    }

    /** Key of an End bookshelf's table (tests). */
    public static ResourceKey<LootTable> key(String wood) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("simpleend", "blocks/" + wood + "_bookshelf"));
    }
}
