package com.simplebuilding.tweaks.easter;

import com.mojang.serialization.Codec;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.TweaksFamilies;
import com.simplebuilding.tweaks.block.TweaksFamilies.Family;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Die versteckte Kette ueber den Endstufen der Pad-Familien (Besitzer 2026-09-27, Spoiler-Abschnitt in
 * docs/SIMPLETWEAKS-UEBERNAHME.md). Die Endstufe einer Familie laesst sich im Schmiedetisch zurueck auf
 * Stufe I schmieden: heraus kommt der gewoehnliche Stufe-I-Block, aber mit der Item-Komponente
 * {@link #EASTER_STAGE} = 1 und dem Namen "Don't do it". Jede weitere Aufwertung kostet dasselbe wie
 * die normale Stufe und hebt Stufe und Easter-Stufe gemeinsam; die letzte Easter-Stufe (= Zahl der
 * Stufen der Familie) ist die Endstufe mit verschleiertem Namen und doppelter Kraft
 * ({@link #isBoosted}). Mit Netheritbarren wird sie zum {@link FunnyStickItem}.
 *
 * <p>Warum eine Komponente statt eigener Bloecke: jede Easter-Stufe <b>ist</b> der Block ihrer Stufe
 * (Verhalten, Textur, Drops), nur die Komponente und ein daraus abgeleiteter {@code item_name} sind
 * anders. Gesetzt merkt sich die Block-Entity die Stufe ({@link OwnedBlockEntity#easterStage()}),
 * beim Abbau schreibt {@code PadBlock#getDrops} sie wieder aufs Item. Ein Amboss setzt nur
 * {@code custom_name}, nie die Komponente: ein umbenanntes normales Pad zaehlt nie als Easter-Pad.
 */
public final class EasterEggs {

    /** Easter-Stufe 1..{@link #stageCount}; fehlt die Komponente, ist es ein normales Pad. */
    public static final DataComponentType<Integer> EASTER_STAGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            SimpleTweaks.id("easter_stage"),
            DataComponentType.<Integer>builder().persistent(Codec.intRange(1, 5)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Hoechstens so viele Easter-Stufen hat eine Familie (bei weniger Stufen entsprechend weniger). */
    public static final int MAX_STAGES = 5;

    /** Die Advancements der Kette (alle versteckt, eigener Tab erst nach dem ersten). */
    public static final Identifier ADV_WHAT_HAVE_YOU_DONE = SimpleTweaks.id("easter/what_have_you_done");
    public static final Identifier ADV_SERIOUSLY = SimpleTweaks.id("easter/seriously");
    public static final Identifier ADV_WORTH_IT = SimpleTweaks.id("easter/it_was_worth_it");
    public static final Identifier ADV_FUNNY_STICK = SimpleTweaks.id("easter/all_that_for_a_stick");

    private static @Nullable Item funnyStick;

    private EasterEggs() {
    }

    // ---------------------------------------------------------------------------------------------
    // Registrierung (TweaksContent)
    // ---------------------------------------------------------------------------------------------

    /** Laedt die Klasse und damit {@link #EASTER_STAGE} (NeoForge/Forge: im RegisterEvent der Komponenten). */
    public static void registerComponents() {
    }

    public static void registerItems() {
        if (funnyStick != null) {
            return;
        }
        Identifier id = SimpleTweaks.id("funny_stick");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        funnyStick = Registry.register(BuiltInRegistries.ITEM, id, new FunnyStickItem(new Item.Properties().setId(key)
                .stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    }

    /** Der "Funny Stick"; erst nach {@link #registerItems()} vorhanden. */
    public static Item funnyStick() {
        if (funnyStick == null) {
            throw new IllegalStateException("funny_stick is not registered yet");
        }
        return funnyStick;
    }

    // ---------------------------------------------------------------------------------------------
    // Familien und Stufen
    // ---------------------------------------------------------------------------------------------

    /** Die Pad-Familien mit Kette; die Druckplatten sind keine Pads (keine Kraft zum Verdoppeln). */
    public static List<Family> families() {
        return List.of(Family.ELYTRA_PAD, Family.FLYPAD, Family.SPAWN_TELEPORTER, Family.LAUNCHPAD, Family.CHUNK_LOADER);
    }

    /** Zahl der Easter-Stufen: 5, oder weniger, wenn die Familie weniger Stufen hat (Flypad: 3). */
    public static int stageCount(Family family) {
        return Math.min(MAX_STAGES, TweaksFamilies.tiers(family).size());
    }

    /** Familie eines Stufenblocks, oder null (Druckplatten, alte Bloecke, fremde Bloecke). */
    public static @Nullable Family familyOf(Block block) {
        for (Family family : families()) {
            if (TweaksFamilies.tiers(family).contains(block)) {
                return family;
            }
        }
        return null;
    }

    /** Der Block einer Easter-Stufe: Stufe s ist der Block der Stufe s. */
    public static Block blockFor(Family family, int stage) {
        return TweaksFamilies.tiers(family).get(stage - 1);
    }

    public static int stageOf(ItemStack stack) {
        Integer stage = stack.get(EASTER_STAGE);
        return stage == null ? 0 : stage;
    }

    /** Ob die Easter-Stufe zum Block passt (Stufe s = Block der Stufe s einer Familie mit Kette). */
    public static boolean fits(Block block, int stage) {
        Family family = familyOf(block);
        return family != null && stage >= 1 && stage <= stageCount(family) && blockFor(family, stage) == block;
    }

    /** Die letzte Easter-Stufe: doppelte Kraft, verschleierter Name. */
    public static boolean isFinal(Block block, int stage) {
        Family family = familyOf(block);
        return family != null && stage == stageCount(family) && fits(block, stage);
    }

    /** Easter-Stufe eines gesetzten Pads (0 = normal). */
    public static int stageAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned.easterStage() : 0;
    }

    /** Ob das gesetzte Pad die letzte Easter-Stufe ist und darum doppelt so stark wirkt. */
    public static boolean isBoosted(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof OwnedBlockEntity owned && isFinal(owned.getBlockState().getBlock(), owned.easterStage());
    }

    // ---------------------------------------------------------------------------------------------
    // Items
    // ---------------------------------------------------------------------------------------------

    /** Ein Easter-Pad der Familie auf der Stufe (Tests, Testzentrale-freie Befehle). */
    public static ItemStack create(Family family, int stage) {
        return mark(new ItemStack(blockFor(family, stage)), stage);
    }

    /**
     * Setzt Easter-Stufe und Namen; die Stufe muss zum Block passen, sonst bleibt der Stapel
     * unveraendert (ein falsch zusammengesetztes Rezept kann so kein halbes Easter-Pad bauen).
     */
    public static ItemStack mark(ItemStack stack, int stage) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) || !fits(blockItem.getBlock(), stage)) {
            return stack;
        }
        stack.set(EASTER_STAGE, stage);
        stack.set(DataComponents.ITEM_NAME, nameFor(blockItem.getBlock(), stage));
        return stack;
    }

    /**
     * Anzeigename der Easter-Stufe: Stufen vor der letzten heissen fuer alle Familien gleich
     * ({@code item.simplebuilding.easter.stage_N}), die letzte heisst wie die Endstufe der Familie,
     * mit Farbverlauf, fett und zwei verschleierten Zeichen ({@code item.simplebuilding.easter.final.<familie>},
     * Formatcodes in der Sprachdatei, gleich viele sichtbare Zeichen wie der normale Name).
     */
    public static Component nameFor(Block block, int stage) {
        Family family = familyOf(block);
        if (family != null && isFinal(block, stage)) {
            return Component.translatable(finalNameKey(family));
        }
        return Component.translatable(stageNameKey(stage));
    }

    public static String stageNameKey(int stage) {
        return "item.simplebuilding.easter.stage_" + stage;
    }

    public static String finalNameKey(Family family) {
        return "item.simplebuilding.easter.final." + family.name().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------------------------------------
    // Die Kette: Kosten je Schritt = Kosten der normalen Stufe
    // ---------------------------------------------------------------------------------------------

    /**
     * Ein Schmiedeschritt der Kette. {@code fromStage} 0 heisst: die normale Endstufe (ohne Komponente)
     * wird zu Easter-Stufe 1. {@code result} ist der Block der Zielstufe oder, beim letzten Schritt,
     * der Funny Stick ({@code toStage} 0).
     */
    public record Step(Family family, List<Item> templates, Item base, int fromStage, @Nullable Item addition,
                       Item result, int toStage) {
    }

    /**
     * Alle Schritte der Familie: der Einstieg kostet, was Stufe I kostet (ihre Vorlage und ihre
     * Hauptzutat - beim Elytra-Pad die Elytra), jeder Folgeschritt die Vorlage und Zutat der normalen
     * Aufwertung auf dieselbe Stufe; zuletzt Netherit-Vorlage + Netheritbarren zum Funny Stick.
     */
    public static List<Step> steps(Family family) {
        List<Block> tiers = TweaksFamilies.tiers(family);
        List<Item> any = anyTemplate();
        List<Item> netherite = List.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        List<Item> enderite = List.of(ModItems.ENDERITE_UPGRADE_TEMPLATE);
        Item diamondPlate = TweaksBlocks.DIAMOND_PRESSURE_PLATE.asItem();
        Item netheritePlate = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        Item enderitePlate = TweaksBlocks.ENDERITE_PRESSURE_PLATE.asItem();

        // Vorlage und Zutat je Zielstufe 1..n, wie in ModRecipeProvider#buildTweaksRecipes.
        List<List<Item>> templates = new ArrayList<>();
        List<Item> additions = new ArrayList<>();
        switch (family) {
            case ELYTRA_PAD -> {
                add(templates, additions, any, Items.ELYTRA);
                add(templates, additions, any, diamondPlate);
                add(templates, additions, netherite, netheritePlate);
                add(templates, additions, enderite, enderitePlate);
                add(templates, additions, netherite, Items.NETHER_STAR);
            }
            case FLYPAD -> {
                add(templates, additions, enderite, ModItems.ENDERITE_CORE);
                add(templates, additions, enderite, enderitePlate);
                add(templates, additions, enderite, TweaksBlocks.REINFORCED_FLYPAD.asItem());
            }
            case SPAWN_TELEPORTER -> {
                add(templates, additions, any, Items.DIAMOND_BLOCK);
                add(templates, additions, netherite, netheritePlate);
                add(templates, additions, netherite, netheritePlate);
                add(templates, additions, netherite, netheritePlate);
                add(templates, additions, enderite, enderitePlate);
            }
            case LAUNCHPAD, CHUNK_LOADER -> {
                add(templates, additions, any, diamondPlate);
                add(templates, additions, netherite, netheritePlate);
                add(templates, additions, enderite, enderitePlate);
            }
            default -> throw new IllegalArgumentException(family + " has no easter chain");
        }

        List<Step> steps = new ArrayList<>();
        int count = stageCount(family);
        Item last = tiers.get(tiers.size() - 1).asItem();
        steps.add(new Step(family, templates.get(0), last, 0, additions.get(0), tiers.get(0).asItem(), 1));
        for (int stage = 1; stage < count; stage++) {
            steps.add(new Step(family, templates.get(stage), tiers.get(stage - 1).asItem(), stage, additions.get(stage),
                    tiers.get(stage).asItem(), stage + 1));
        }
        steps.add(new Step(family, netherite, tiers.get(count - 1).asItem(), count, Items.NETHERITE_INGOT, funnyStick(), 0));
        return steps;
    }

    private static void add(List<List<Item>> templates, List<Item> additions, List<Item> template, Item addition) {
        templates.add(template);
        additions.add(addition);
    }

    /** "Beliebige Vorlage" der Einstiegsstufen (dieselbe Liste wie ModRecipeProvider#buildTweaksRecipes). */
    public static List<Item> anyTemplate() {
        return List.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
    }
}
