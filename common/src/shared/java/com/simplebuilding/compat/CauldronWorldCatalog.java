package com.simplebuilding.compat;

import com.simplebuilding.compat.InWorldRecipeCatalog.Stack;
import com.simplebuilding.crucible.CrucibleCompat;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Things done in the world with cauldrons and crucibles that have no recipe: building the iron crucible,
 * attaching a barrel, turning a cauldron into a reinforced one, and the milk cauldron's butter and cheese.
 * JEI shows them as the "Cauldron and crucible" category ({@code CauldronWorldCategory}).
 *
 * <p>Free of JEI classes (a game test checks it). Like {@link CrucibleRecipeCatalog} it needs SimpleLib, so it
 * is empty unless {@link McVersion#CRUCIBLE}; SimpleLib and SimpleSandwiches are met only by public id, tag and the
 * {@link CrucibleCompat} bridge. Items that do not resolve are skipped, so a missing module only removes its entries.
 * The milk cauldron's ripening times live in its module's config and are deliberately not shown.
 *
 * <p>The cauldron upgrade is built from {@link SledgehammerUpgrades#upgradeOf}; the "machine upgrade" category does
 * not list it (its export only covers the copper chest, the shulker box and SimpleBuilding's own blocks).
 */
public final class CauldronWorldCatalog {

    /** One entry: input slots, the tools (empty: none), the result, an arrow duration (0: static) and the notes underneath. */
    public record Entry(String id, List<Stack> inputs, List<Item> tools, Stack output, int durationTicks, List<Component> notes) {
    }

    private static final String NOTE = "jei.simplebuilding.note.cauldron_world.";

    private CauldronWorldCatalog() {
    }

    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (!McVersion.CRUCIBLE) {
            return out;
        }
        List<Item> hammers = hammers(0);
        crucibleBuild(out, hammers);
        barrelAttach(out, hammers);
        cauldronReinforce(out);
        milk(out);
        return out;
    }

    /** The items that perform or stand for these entries: both cauldrons, every sledgehammer, the four crucibles. */
    public static List<Item> catalysts() {
        Set<Item> out = new LinkedHashSet<>();
        if (!McVersion.CRUCIBLE) {
            return new ArrayList<>(out);
        }
        out.add(Items.CAULDRON);
        addIfPresent(out, item("simplelib:reinforced_cauldron"));
        out.addAll(hammers(0));
        for (String id : List.of("simplelib:iron_crucible", "simplelib:reinforced_crucible", "simplelib:netherite_crucible",
                "simplebuilding:enderite_crucible")) {
            addIfPresent(out, item(id));
        }
        return new ArrayList<>(out);
    }

    private static void crucibleBuild(List<Entry> out, List<Item> hammers) {
        Item iron = item("simplelib:iron_crucible");
        List<Item> walls = tagged("simplelib", "crucible_walls");
        List<Item> handles = tagged("simplelib", "crucible_handles");
        int strikes = CrucibleCompat.buildStrikes();
        int wallStrikes = CrucibleCompat.buildWallStrikes();
        if (iron == null || walls.isEmpty() || handles.isEmpty() || hammers.isEmpty() || strikes <= wallStrikes) {
            return;
        }
        int handleStrikes = strikes - wallStrikes;
        out.add(new Entry("crucible_build",
                List.of(Stack.of(Items.IRON_BLOCK, 1), new Stack(walls, wallStrikes), new Stack(handles, handleStrikes)),
                hammers, Stack.of(iron, 1), 0,
                List.of(Component.translatable(NOTE + "crucible_build.how", strikes),
                        Component.translatable(NOTE + "crucible_build.parts", wallStrikes, handleStrikes),
                        Component.translatable(NOTE + "strike_damage", CrucibleCompat.HAMMER_DAMAGE_PER_STRIKE))));
    }

    private static void barrelAttach(List<Entry> out, List<Item> hammers) {
        List<Item> barrels = items("simplelib:copper_barrel", "simplelib:reinforced_barrel", "simplebuilding:enderite_barrel");
        List<Item> crucibles = items("simplelib:iron_crucible", "simplelib:reinforced_crucible", "simplelib:netherite_crucible",
                "simplebuilding:enderite_crucible");
        int strikes = CrucibleCompat.attachStrikes();
        if (barrels.isEmpty() || crucibles.isEmpty() || hammers.isEmpty() || strikes <= 0) {
            return;
        }
        out.add(new Entry("barrel_attach",
                List.of(new Stack(barrels, 1), new Stack(crucibles, 1)), hammers, new Stack(barrels, 1), 0,
                List.of(Component.translatable(NOTE + "barrel_attach.how", strikes),
                        Component.translatable(NOTE + "barrel_attach.use", CrucibleCompat.attachedBarrelSlots()),
                        Component.translatable(NOTE + "strike_damage", CrucibleCompat.HAMMER_DAMAGE_PER_STRIKE))));
    }

    private static void cauldronReinforce(List<Entry> out) {
        SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(Blocks.CAULDRON);
        if (upgrade == null || upgrade.to().asItem() == Items.AIR) {
            return;
        }
        List<Item> hammers = hammers(upgrade.minHammerRank());
        if (hammers.isEmpty()) {
            return;
        }
        int blows = SledgehammerUpgrades.blows(upgrade);
        int ticks = SledgehammerUpgrades.upgradeTicks(upgrade);
        int perHit = SledgehammerUpgrades.damagePerHit(upgrade);
        out.add(new Entry("cauldron_reinforce",
                List.of(Stack.of(Items.CAULDRON, 1), Stack.of(upgrade.nugget(), upgrade.materialCost())),
                hammers, Stack.of(upgrade.to().asItem(), 1), ticks,
                List.of(Component.translatable("jei.simplebuilding.note.machine_upgrade.how"),
                        Component.translatable("jei.simplebuilding.note.machine_upgrade.time", blows,
                                ticks % 20 == 0 ? Integer.toString(ticks / 20) : Double.toString(ticks / 20.0), ticks),
                        Component.translatable("jei.simplebuilding.note.machine_upgrade.damage", perHit, perHit * blows))));
    }

    private static void milk(List<Entry> out) {
        Item butter = item("simplesandwiches:butter_block");
        Item cheese = item("simplesandwiches:cheese_block");
        if (butter != null) {
            out.add(new Entry("milk_butter", List.of(Stack.of(Items.CAULDRON, 1), Stack.of(Items.MILK_BUCKET, 1)), List.of(),
                    Stack.of(butter, 1), 0,
                    List.of(Component.translatable(NOTE + "milk_butter.fill"), Component.translatable(NOTE + "milk_butter.take"))));
        }
        if (cheese != null) {
            out.add(new Entry("milk_cheese",
                    List.of(Stack.of(Items.CAULDRON, 1), Stack.of(Items.MILK_BUCKET, 1), Stack.of(Items.FERMENTED_SPIDER_EYE, 1)),
                    List.of(), Stack.of(cheese, 1), 0,
                    List.of(Component.translatable(NOTE + "milk_cheese.fill"), Component.translatable(NOTE + "milk_cheese.take"),
                            Component.translatable(NOTE + "milk_cheese.spoil"))));
        }
    }

    /** Every sledgehammer that can do a job of this rank, weakest first. */
    private static List<Item> hammers(int minRank) {
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SledgehammerItem && SledgehammerUpgrades.hammerRank(item) >= minRank) {
                out.add(item);
            }
        }
        out.sort(Comparator.<Item>comparingInt(item -> SledgehammerUpgrades.hammerRank(item))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return out;
    }

    /** All items in {@code namespace:path}'s item tag, by id. */
    private static List<Item> tagged(String namespace, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (new ItemStack(item).is(tag)) {
                out.add(item);
            }
        }
        out.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return out;
    }

    private static List<Item> items(String... ids) {
        List<Item> out = new ArrayList<>();
        for (String id : ids) {
            addIfPresent(out, item(id));
        }
        return out;
    }

    private static void addIfPresent(java.util.Collection<Item> into, @Nullable Item item) {
        if (item != null) {
            into.add(item);
        }
    }

    /** The item with this id, or null when it does not exist (module missing). */
    private static @Nullable Item item(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id.toLowerCase(Locale.ROOT))).orElse(null);
        return item == null || item == Items.AIR ? null : item;
    }
}
