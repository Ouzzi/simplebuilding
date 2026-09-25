package com.simplebuilding.blueprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Blocksuche fuer den Editor (Hilfe-Blockliste und Einfuege-Leiste): findet Bloecke ueber den
 * angezeigten Namen (sprachabhaengig, vom Aufrufer geliefert) oder die Id und liefert, was in den
 * Code gehoert ({@link #codeName}).
 *
 * <p>Rangfolge: exakter Treffer (Id, Pfad oder Name), dann Anfang von Id-Pfad oder Name, dann
 * Wortanfang im Namen, dann irgendwo enthalten; innerhalb gleich guter Treffer der kuerzere Name
 * zuerst, dann nach Id.
 */
public final class BlueprintBlockSearch {
    private BlueprintBlockSearch() {
    }

    public static List<Block> search(String query, Function<Block, String> displayName, int limit) {
        String q = query.strip().toLowerCase(Locale.ROOT);
        List<Hit> hits = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (block.defaultBlockState().isAir()) {
                continue;
            }
            String name = displayName.apply(block);
            String lower = name.toLowerCase(Locale.ROOT);
            int rank = rank(q, id, lower);
            if (rank >= 0) {
                hits.add(new Hit(block, id.toString(), name, rank));
            }
        }
        hits.sort(Comparator.comparingInt(Hit::rank).thenComparingInt(h -> h.name.length()).thenComparing(Hit::id));
        List<Block> out = new ArrayList<>();
        for (int i = 0; i < hits.size() && i < limit; i++) {
            out.add(hits.get(i).block);
        }
        return out;
    }

    private static int rank(String q, Identifier id, String name) {
        if (q.isEmpty()) {
            return 4;
        }
        String full = id.toString();
        String path = id.getPath();
        if (full.equals(q) || path.equals(q) || name.equals(q)) {
            return 0;
        }
        if (path.startsWith(q) || full.startsWith(q) || name.startsWith(q)) {
            return 1;
        }
        if (name.contains(" " + q) || path.contains("_" + q)) {
            return 2;
        }
        if (full.contains(q) || name.contains(q)) {
            return 3;
        }
        return -1;
    }

    /** Was fuer diesen Block in den Code eingefuegt wird: die Id, ohne {@code minecraft:}. */
    public static String codeName(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private record Hit(Block block, String id, String name, int rank) {
    }
}
