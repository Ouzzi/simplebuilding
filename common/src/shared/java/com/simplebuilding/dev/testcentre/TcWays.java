package com.simplebuilding.dev.testcentre;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.util.InWorldTransformations;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Wege (Rezepte und Umwandlungen in der Welt), die Schilder der Testzentrale beschreiben (TZ22-8, Besitzer 22.10.: "am
 * Eisenblock noch Hammer + 4 -> 2 Iron Poles, obwohl die Herstellung auf den Tiegel umgestellt ist").
 *
 * <p>Ein Schild schreibt einen Weg nicht mehr von Hand, sondern bekommt ihn aus den Daten, die auch das Spiel benutzt
 * (Fabriken unten: die Aufwertungs-Engine aus {@code in_world_upgrades}, {@link InWorldTransformations} wie Wiki und JEI,
 * die Tiegel-API, die Item-Tags) und erzeugt daraus seine Zeilen ({@link Way#lines}, Namen aus der Registry, also
 * uebersetzt). {@link #problem} prueft jeden Weg gegen dieselben Live-Daten bzw. den RecipeManager; der Test
 * {@code everyTransformationSignShowsAnExistingWay} meldet jedes Schild, dessen Weg es nicht (mehr) gibt, und jedes
 * Schild, das einen Weg ("->") ohne Daten zeigt.
 */
public final class TcWays {

    /** Art des Wegs: bestimmt, gegen welche Daten {@link #problem} prueft. */
    public enum Kind {
        /** Aufwertung in der Welt (Hammer, Material in der Nebenhand): {@link SledgehammerUpgrades#upgradeOf}. */
        UPGRADE,
        /** Zerschlagen mit dem Hammer: {@link InWorldTransformations#diamondCrush}. */
        CRUSH,
        /** Schere an Wolle: {@link InWorldTransformations#shearWool}. */
        SHEAR,
        /** Oktant im Wasserkessel waschen: {@link InWorldTransformations#cauldronWash}. */
        WASH,
        /** Schallplatte mit dem Hammer umdrehen: {@link InWorldTransformations#discFlip}. */
        FLIP,
        /** Eisentiegel aus einem Vanilla-Kessel schlagen (Simple Crucibles, ueber {@code CrucibleCompat}). */
        CRUCIBLE_BUILD,
        /** Hochofen-Rezept: RecipeManager. */
        BLASTING
    }

    /**
     * Ein Weg: {@code subject} wird mit {@code inputs} (Material, Mengen im Stapel) und {@code tool} zu {@code result}.
     * Leere Stapel: gibt es nicht.
     */
    public record Way(Kind kind, ItemStack subject, List<ItemStack> inputs, ItemStack tool, ItemStack result) {
        public Way {
            inputs = List.copyOf(inputs);
        }

        /**
         * Die Schildzeilen: {@code title} fett, das Ausgangsstueck, je Material "+ Nx Name", "mit Werkzeug", "-> Ergebnis".
         * Hoechstens vier: erst faellt der Titel weg, dann das Werkzeug (das zeigt die Station im Rahmen/in der Truhe).
         */
        public List<Component> lines(@Nullable Component title) {
            List<Component> out = new ArrayList<>();
            if (title != null) out.add(TcText.bold(title));
            if (!subject.isEmpty()) out.add(amount(subject));
            for (ItemStack input : inputs) out.add(TcText.t("way.input", "+ %s", amount(input)));
            Component toolLine = tool.isEmpty() ? null : TcText.t("way.tool", "with %s", tool.getHoverName());
            if (toolLine != null) out.add(toolLine);
            out.add(resultLine());
            if (out.size() > 4 && title != null) out.removeFirst();
            if (out.size() > 4 && toolLine != null) out.remove(toolLine);
            return out.size() > 4 ? out.subList(out.size() - 4, out.size()) : out;
        }

        /** Nur das Ergebnis ("-> Name"), fuer kleine Schilder ueber Rahmen, die Material und Werkzeug zeigen. */
        public Component resultLine() {
            return TcText.t("way.result", "-> %s", amount(result));
        }

        private static Component amount(ItemStack stack) {
            return stack.getCount() > 1 ? TcText.t("way.count", "%sx %s", stack.getCount(), stack.getHoverName()) : stack.getHoverName();
        }
    }

    private TcWays() {
    }

    // ------------------------------------------------------------------ Fabriken (aus den Daten)

    /** Aufwertung von {@code from} laut Engine, Werkzeug der schwaechste reichende Hammer aus {@code hammers}; sonst null. */
    public static @Nullable Way upgrade(Block from, List<Item> hammers) {
        SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(from);
        if (upgrade == null || upgrade.to().asItem() == Items.AIR) return null;
        ItemStack tool = ItemStack.EMPTY;
        for (Item hammer : hammers) {
            if (SledgehammerUpgrades.hammerRank(hammer) >= upgrade.minHammerRank()) {
                tool = new ItemStack(hammer);
                break;
            }
        }
        return new Way(Kind.UPGRADE, new ItemStack(from), List.of(new ItemStack(upgrade.nugget(), upgrade.materialCost())), tool,
                new ItemStack(upgrade.to()));
    }

    /** Zerschlagen von {@code input} (Diamantblock oder eine der weiteren Hammer-Zerkleinerungen); sonst null. */
    public static @Nullable Way crush(Item input, List<Item> hammers) {
        JsonObject entry = crushEntry(InWorldTransformations.diamondCrush(), id(input));
        if (entry == null) return null;
        Item result = item(entry.get("result").getAsString());
        JsonArray allowed = entry.has("hammers") ? entry.getAsJsonArray("hammers") : null;
        ItemStack tool = ItemStack.EMPTY;
        for (Item hammer : hammers) {
            if (allowed == null || contains(allowed, id(hammer))) {
                tool = new ItemStack(hammer);
                break;
            }
        }
        return new Way(Kind.CRUSH, new ItemStack(input), List.of(), tool, new ItemStack(result, entry.get("count").getAsInt()));
    }

    /** Schere an {@code wool}; sonst null. */
    public static @Nullable Way shear(Block wool) {
        JsonObject o = InWorldTransformations.shearWool();
        if (!contains(o.getAsJsonArray("blocks"), id(wool.asItem()))) return null;
        return new Way(Kind.SHEAR, new ItemStack(wool), List.of(), new ItemStack(item(o.get("tool").getAsString())),
                new ItemStack(item(o.get("result").getAsString()), o.get("count").getAsInt()));
    }

    /** Gefaerbter Oktant {@code octant} im Wasserkessel; sonst null. */
    public static @Nullable Way wash(Item octant) {
        JsonObject o = InWorldTransformations.cauldronWash();
        if (!contains(o.getAsJsonArray("octants"), id(octant))) return null;
        return new Way(Kind.WASH, new ItemStack(octant), List.of(), new ItemStack(item(o.get("cauldron").getAsString())),
                new ItemStack(item(o.get("result").getAsString())));
    }

    /** Schallplatte {@code disc} mit dem Hammer zum naechsten Track; sonst null. */
    public static @Nullable Way flip(Item disc, List<Item> hammers) {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS || disc == null) return null;
        String next = nextTrack(InWorldTransformations.discFlip(), id(disc));
        if (next == null || hammers.isEmpty()) return null;
        return new Way(Kind.FLIP, new ItemStack(disc), List.of(), new ItemStack(hammers.getFirst()), new ItemStack(item(next)));
    }

    /**
     * Eisentiegel (Simple Crucibles): leerer Vanilla-Kessel, je Wandschlag ein Wandmaterial, je Henkelschlag ein
     * Henkelmaterial (SimpleBuilding: die Eisenstange, falls im Tag), Hammer. Null ohne Simple Crucibles.
     */
    public static @Nullable Way crucibleBuild(List<Item> hammers) {
        Item crucible = item(CRUCIBLES + ":iron_crucible");
        int strikes = com.simplebuilding.crucible.CrucibleCompat.buildStrikes();
        int walls = com.simplebuilding.crucible.CrucibleCompat.buildWallStrikes();
        Item wall = tagged(WALLS, Items.IRON_INGOT);
        Item handle = tagged(HANDLES, com.simplebuilding.items.ModItems.IRON_POLE);
        if (crucible == Items.AIR || strikes <= walls || walls <= 0 || wall == null || handle == null) return null;
        return new Way(Kind.CRUCIBLE_BUILD, new ItemStack(Items.CAULDRON),
                List.of(new ItemStack(wall, walls), new ItemStack(handle, strikes - walls)),
                hammers.isEmpty() ? ItemStack.EMPTY : new ItemStack(hammers.getFirst()), new ItemStack(crucible));
    }

    /** Hochofen: {@code input} -> {@code result} (die Rezepte kennt erst der Server; {@link #problem} prueft). */
    public static Way blasting(Item input, Item result) {
        return new Way(Kind.BLASTING, new ItemStack(input), List.of(), ItemStack.EMPTY, new ItemStack(result));
    }

    // ------------------------------------------------------------------ Pruefung (gegen die Live-Daten)

    /** Null, wenn es den Weg so gibt; sonst warum nicht. */
    public static @Nullable String problem(Way way, ServerLevel level) {
        Item subject = way.subject().getItem();
        Item result = way.result().getItem();
        switch (way.kind()) {
            case UPGRADE -> {
                SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(Block.byItem(subject));
                if (upgrade == null) return id(subject) + " has no in-world upgrade";
                if (upgrade.to().asItem() != result) return id(subject) + " upgrades to " + id(upgrade.to().asItem()) + ", not " + id(result);
                ItemStack material = way.inputs().isEmpty() ? ItemStack.EMPTY : way.inputs().getFirst();
                if (material.getItem() != upgrade.nugget() || material.getCount() != upgrade.materialCost()) {
                    return id(subject) + " takes " + upgrade.materialCost() + "x " + id(upgrade.nugget()) + ", the sign says " + material;
                }
                if (!way.tool().isEmpty() && SledgehammerUpgrades.hammerRank(way.tool().getItem()) < upgrade.minHammerRank()) {
                    return id(way.tool().getItem()) + " is too weak to upgrade " + id(subject);
                }
                return null;
            }
            case CRUSH -> {
                JsonObject entry = crushEntry(InWorldTransformations.diamondCrush(), id(subject));
                if (entry == null) return id(subject) + " cannot be crushed";
                if (!entry.get("result").getAsString().equals(id(result)) || entry.get("count").getAsInt() != way.result().getCount()) {
                    return id(subject) + " crushes into " + entry.get("count") + "x " + entry.get("result").getAsString() + ", the sign says " + way.result();
                }
                if (entry.has("hammers") && !way.tool().isEmpty() && !contains(entry.getAsJsonArray("hammers"), id(way.tool().getItem()))) {
                    return id(way.tool().getItem()) + " cannot crush " + id(subject);
                }
                return null;
            }
            case SHEAR -> {
                JsonObject o = InWorldTransformations.shearWool();
                boolean ok = contains(o.getAsJsonArray("blocks"), id(subject)) && o.get("tool").getAsString().equals(id(way.tool().getItem()))
                        && o.get("result").getAsString().equals(id(result)) && o.get("count").getAsInt() == way.result().getCount();
                return ok ? null : "no shears way " + id(subject) + " -> " + way.result();
            }
            case WASH -> {
                JsonObject o = InWorldTransformations.cauldronWash();
                boolean ok = contains(o.getAsJsonArray("octants"), id(subject)) && o.get("result").getAsString().equals(id(result))
                        && o.get("cauldron").getAsString().equals(id(way.tool().getItem()));
                return ok ? null : "no cauldron wash " + id(subject) + " -> " + id(result);
            }
            case FLIP -> {
                if (!com.simplebuilding.version.McVersion.MUSIC_DISCS) return "no disc flips on this line";
                String next = nextTrack(InWorldTransformations.discFlip(), id(subject));
                return id(result).equals(next) ? null : id(subject) + " flips to " + next + ", not " + id(result);
            }
            case CRUCIBLE_BUILD -> {
                if (way.inputs().size() != 2) return "the crucible build needs a wall and a handle material";
                ItemStack wall = way.inputs().get(0), handle = way.inputs().get(1);
                Block base = Block.byItem(subject);
                if (!com.simplebuilding.crucible.CrucibleCompat.canBuildStrike(base.defaultBlockState(), wall)) {
                    return "the crucible build does not start on " + id(subject) + " with " + id(wall.getItem());
                }
                if (!wall.is(WALLS) || !handle.is(HANDLES)) return id(wall.getItem()) + "/" + id(handle.getItem()) + " are no crucible wall/handle materials";
                int walls = com.simplebuilding.crucible.CrucibleCompat.buildWallStrikes();
                int strikes = com.simplebuilding.crucible.CrucibleCompat.buildStrikes();
                if (wall.getCount() != walls || handle.getCount() != strikes - walls) {
                    return "the crucible takes " + walls + " walls + " + (strikes - walls) + " handles, the sign says " + wall.getCount() + " + " + handle.getCount();
                }
                if (!id(result).equals(CRUCIBLES + ":iron_crucible")) return "the build makes the iron crucible, not " + id(result);
                if (!way.tool().isEmpty() && !way.tool().is(HAMMERS)) return id(way.tool().getItem()) + " is no hammer";
                return null;
            }
            case BLASTING -> {
                SingleRecipeInput input = new SingleRecipeInput(way.subject().copyWithCount(1));
                ItemStack out = level.recipeAccess().getRecipeFor(RecipeType.BLASTING, input, level)
                        .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
                return out.getItem() == result && out.getCount() == way.result().getCount() ? null
                        : "the blast furnace makes " + out + " from " + id(subject) + ", not " + way.result();
            }
        }
        return "unknown way";
    }

    // ------------------------------------------------------------------ Hilfen

    private static final String CRUCIBLES = "simplecrucibles";
    private static final TagKey<Item> WALLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CRUCIBLES, "crucible_walls"));
    private static final TagKey<Item> HANDLES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CRUCIBLES, "crucible_handles"));
    private static final TagKey<Item> HAMMERS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplelib", "hammers"));

    /** {@code preferred}, wenn es im Tag steht, sonst das erste Item des Tags (Registry-Reihenfolge); null bei leerem Tag. */
    private static @Nullable Item tagged(TagKey<Item> tag, @Nullable Item preferred) {
        if (preferred != null && new ItemStack(preferred).is(tag)) return preferred;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR && new ItemStack(item).is(tag)) return item;
        }
        return null;
    }

    /** Der Eintrag fuer {@code input}: der Diamantblock oben ({@code block}) oder einer der {@code crushes}. */
    private static @Nullable JsonObject crushEntry(JsonObject crush, String input) {
        if (crush.get("block").getAsString().equals(input)) {
            JsonObject top = new JsonObject();
            top.add("result", crush.get("result"));
            top.add("count", crush.get("count"));
            top.add("hammers", crush.get("hammers"));
            return top;
        }
        for (JsonElement element : crush.getAsJsonArray("crushes")) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.get("input").getAsString().equals(input)) return entry;
        }
        return null;
    }

    /** Der naechste Track im Kreis der Platte {@code disc}, oder null. */
    private static @Nullable String nextTrack(JsonObject flip, String disc) {
        for (JsonElement cycle : flip.getAsJsonArray("cycles")) {
            JsonArray tracks = cycle.getAsJsonArray();
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).getAsString().equals(disc) && tracks.size() > 1) return tracks.get((i + 1) % tracks.size()).getAsString();
            }
        }
        return null;
    }

    private static boolean contains(JsonArray array, String value) {
        for (JsonElement element : array) {
            if (element.getAsString().equals(value)) return true;
        }
        return false;
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(Items.AIR);
    }
}
