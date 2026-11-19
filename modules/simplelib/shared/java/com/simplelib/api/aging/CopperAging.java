package com.simplelib.api.aging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Copper-like aging for shape families that register every stage themselves (N86-7: the copper octets of Simple Octets, the
 * copper half slabs of Simple Slabs): links them into {@link AgingChain}s by Vanilla's copper names, so they age, take wax and
 * get scraped exactly like Vanilla copper (same chance, neighbour rule, honeycomb, axe, dispenser).
 *
 * <p>Name rule (Vanilla's): stage 0 is the plain path ({@code cut_copper_half_slab}, {@code copper_block_octet}), the aged
 * stages put {@code exposed_}, {@code weathered_}, {@code oxidized_} in front, with {@code copper_block} shortened to
 * {@code copper} ({@code exposed_copper_octet}); the waxed copy of each stage has {@code waxed_} in front. A family with a stage
 * missing is skipped; without all four waxed copies it ages without wax.
 */
public final class CopperAging {
    /** One front for every linked copper family (a younger copper shape near an older one holds it back, like Vanilla copper). */
    public static final Identifier GROUP = Identifier.fromNamespaceAndPath("simplelib", "copper");
    private static final List<String> PREFIXES = List.of("", "exposed_", "weathered_", "oxidized_");

    private CopperAging() {}

    /** Path of stage {@code stage} (0..3) of the family whose plain path is {@code base}. */
    public static String stagePath(String base, int stage) {
        return stage == 0 ? base : PREFIXES.get(stage) + base.replace("copper_block", "copper");
    }

    /**
     * Links every copper family found in {@code blocks} (registered path in {@code namespace} -> block) and returns the chains.
     * Call once, after the blocks are registered.
     */
    public static List<AgingChain> link(String namespace, Map<String, Block> blocks) {
        List<AgingChain> out = new ArrayList<>();
        for (Map.Entry<String, Block> e : blocks.entrySet()) {
            String base = e.getKey();
            if (!base.contains("copper") || base.startsWith("waxed_") || PREFIXES.stream().skip(1).anyMatch(base::startsWith)) continue;
            List<Block> aged = new ArrayList<>();
            List<Block> waxed = new ArrayList<>();
            for (int stage = 0; stage < PREFIXES.size(); stage++) {
                String path = stagePath(base, stage);
                if (stage > 0) {
                    Block block = blocks.get(path);
                    if (block == null) break;
                    aged.add(block);
                }
                Block wax = blocks.get("waxed_" + path);
                if (wax != null) waxed.add(wax);
            }
            if (aged.size() != PREFIXES.size() - 1) continue;
            AgingChain chain = AgingChain.builder(Identifier.fromNamespaceAndPath(namespace, base), e.getValue()).group(GROUP)
                    .adopt(aged, waxed.size() == PREFIXES.size() ? waxed : List.of()).build();
            chain.registerBlocks();
            out.add(chain);
        }
        return out;
    }
}
