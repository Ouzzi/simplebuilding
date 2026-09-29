package com.simplebuilding.compat;

import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.tweaks.heads.ModHeads;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Items a mob drops only when it dies in one particular way - no recipe, no chest: the heads a
 * charged creeper's explosion knocks off (vanilla {@code loot_table/charged_creeper/*} plus the
 * mod's twelve heads from {@code ModLootTableModifications#headPool}) and the music discs a
 * creeper drops when a skeleton kills it (vanilla {@code loot_table/entities/creeper}, item tag
 * {@code minecraft:creeper_drop_music_discs}).
 *
 * <p>JEI shows these as the "Mob drops" category ({@code MobDropCategory}); the wiki reads the same
 * facts from the loot tables themselves ({@code wiki/obtain_sources.py}). Free of any JEI class, so
 * the server game test {@code InWorldExportTests#mobDropCatalogMatchesTheGame} can kill the mobs and
 * hold every entry against what really drops.
 */
public final class MobDropCatalog {

    private MobDropCatalog() {
    }

    /** How the mob has to die. */
    public enum Cause {
        CHARGED_CREEPER("charged_creeper"),
        KILLED_BY_SKELETON("killed_by_skeleton");

        private final String id;

        Cause(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        /** Translation key of the line under the slots; {@code %s} is the victim's name. */
        public String noteKey() {
            return "jei.simplebuilding.note.mob_drop." + id;
        }
    }

    /**
     * One drop: the victim (and its spawn egg for the slot), what kills it (and its egg), and the
     * items that can come out - one of them per death.
     */
    public record Drop(String id, Cause cause, EntityType<?> victim, Item victimIcon,
                       EntityType<?> killer, Item killerIcon, List<Item> results) {
    }

    /** All drops as the game has them now; the disc entry needs item tags, so it is missing before they are bound. */
    public static List<Drop> drops() {
        List<Drop> out = new ArrayList<>();
        head(out, EntityTypes.CREEPER, Items.CREEPER_SPAWN_EGG, Items.CREEPER_HEAD);
        head(out, EntityTypes.PIGLIN, Items.PIGLIN_SPAWN_EGG, Items.PIGLIN_HEAD);
        head(out, EntityTypes.SKELETON, Items.SKELETON_SPAWN_EGG, Items.SKELETON_SKULL);
        head(out, EntityTypes.WITHER_SKELETON, Items.WITHER_SKELETON_SPAWN_EGG, Items.WITHER_SKELETON_SKULL);
        head(out, EntityTypes.ZOMBIE, Items.ZOMBIE_SPAWN_EGG, Items.ZOMBIE_HEAD);
        // Die Mod-Koepfe (docs/MOBKOEPFE.md), dann alle nach der Id des Opfers wie bisher.
        for (BlazeHeadType type : BlazeHeadType.values()) {
            head(out, ModHeads.source(type), ModHeads.spawnEgg(type), TweaksItems.head(type));
        }
        out.sort(java.util.Comparator.comparing(Drop::id));
        List<Item> discs = new ArrayList<>();
        for (Holder<Item> disc : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.CREEPER_DROP_MUSIC_DISCS)) {
            discs.add(disc.value());
        }
        if (!discs.isEmpty()) {
            out.add(new Drop(Cause.KILLED_BY_SKELETON.id() + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(EntityTypes.CREEPER),
                    Cause.KILLED_BY_SKELETON, EntityTypes.CREEPER, Items.CREEPER_SPAWN_EGG,
                    EntityTypes.SKELETON, Items.SKELETON_SPAWN_EGG, List.copyOf(discs)));
        }
        return out;
    }

    private static void head(List<Drop> out, EntityType<?> victim, Item egg, Item head) {
        out.add(new Drop(Cause.CHARGED_CREEPER.id() + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(victim),
                Cause.CHARGED_CREEPER, victim, egg, EntityTypes.CREEPER, Items.CREEPER_SPAWN_EGG, List.of(head)));
    }
}
