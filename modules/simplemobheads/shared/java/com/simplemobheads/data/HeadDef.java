package com.simplemobheads.data;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One head of {@link HeadTable} (generated from {@code data/mobs.json}). Plain data, no Minecraft class: the registry reads
 * the ids, the drop code the mob and rules, the client the {@link Look}s.
 *
 * @param id               head id ({@code cat}); item {@code simplemobheads:<item>}, wall block {@code simplemobheads:<wall>}
 * @param mob              entity type id whose death by a charged creeper drops it ({@code minecraft:cat}, {@code simplemobs:dread})
 * @param group            monster, nether, passive, water, boss or mod (tab order and wiki)
 * @param sounds           note block sound, first entry wins (Vanilla ids only, so the sound exists without the mob's mod)
 * @param from             namespace the head moved in from ({@code simplefun}, {@code simplebuilding}) or empty
 * @param ability          ability id ({@code blaze}, {@code bee}, ...) or empty: only camouflage, note block and greeting
 * @param trial            in {@code #simplemobheads:trial_chamber_heads} (SimpleBuilding's Chunk Loader I / Launchpad I)
 * @param freezeImmune     in {@code #minecraft:freeze_immune_wearables}
 * @param drop             {@code default}, {@code wither} (boss: server switch, default off), {@code warden} (switch, default on),
 *                         {@code ritual} (Herobrine), {@code tribute} (Technoblade), {@code giant}, {@code giant_cow} (EH21 easter eggs)
 * @param variantComponent entity data component that names the variant ({@code minecraft:cat/variant}) or empty
 * @param variantSource    special variant reader ({@code panda}, {@code copper_golem}, {@code strider}, {@code entity_type}) or empty
 * @param firstVariant     variant id the base look shows (first of mobs.json); empty without variants
 * @param looks            the base look first (variant id empty), then one look per further variant
 * @param countsAs         worn, the head also counts as this head ({@code cow}: its ability works) or, with {@code variant}, as
 *                         the head named like its variant ({@code giant/husk} counts as the husk head); empty = only itself
 * @param camouflage       further mobs (besides {@link #mob}) that see the wearer only half as far
 */
public record HeadDef(String id, String item, String wall, String mob, String group, List<String> sounds, String from, String ability,
                      boolean trial, boolean freezeImmune, String drop, String variantComponent, String variantSource, String firstVariant, List<Look> looks,
                      String countsAs, List<String> camouflage) {

    /** The variant ids in order: the first variant (the base look) and every further one; empty when the head has none. */
    public List<String> variantIds() {
        if (looks.size() < 2) {
            return List.of();
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        out.add(firstVariant());
        for (Look look : looks.subList(1, looks.size())) {
            out.add(look.variant());
        }
        return List.copyOf(out);
    }

    /** Whether the placed head is a giant head (EH21: the look is drawn bigger than a block, so the block is a full one). */
    public boolean big() {
        return looks.get(0).scale() > 1.0F;
    }

    /** The look of {@code variant} (base look for an unknown or empty variant). */
    public Look look(@Nullable String variant) {
        if (variant != null && !variant.isEmpty()) {
            for (Look look : looks) {
                if (look.variant().equals(variant)) {
                    return look;
                }
            }
        }
        return looks.get(0);
    }

    /**
     * How one variant looks. {@code custom} names hand-made geometry (the 16 heads that moved in keep their accepted look);
     * otherwise {@code layer} ({@code minecraft:cat#main}) and {@code part} (child name, '' = whole model) of the mob's own
     * model are baked. Render: cutout, translucent, emissive. Tint: '' or {@code dye:<color>}.
     */
    public record Look(String variant, String kind, String custom, String layer, String part, String texture, String render, String tint,
                       List<String> hide, float @Nullable [] rot, float scale, List<Pass> passes) {
    }

    /** An extra render pass over the head: eyes, clothes, wool, overlay; render cutout, translucent, eyes, breeze_eyes. */
    public record Pass(String layer, String part, String texture, String render, String tint) {
    }
}
