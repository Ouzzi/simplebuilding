package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public ModItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    /**
     * MC 26.2: {@code valueLookupBuilder(...)} ist entfallen (Ersatz: {@code builder(...)}) und
     * {@link net.minecraft.data.tags.TagAppender} nimmt nur noch {@link ResourceKey}s statt Item-Instanzen
     * entgegen (Vanilla nutzt dafuer die Konstanten aus {@code net.minecraft.references.ItemIds}).
     * Dieser Helfer liefert den Registry-Key zu einer Item-Instanz, damit die Tag-Inhalte
     * unveraendert bleiben.
     */
    private static ResourceKey<Item> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            builder(ModTags.Items.REPAIRS_RESONANCE_ROD).add(key(Items.AMETHYST_SHARD));
        }
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            for (var chest : ModBlocks.trappedCopperChests()) {
                builder(net.minecraft.tags.TagKey.<Item>create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "chests/trapped"))).add(key(chest.asItem()));
                builder(net.minecraft.tags.TagKey.<Item>create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "chests"))).add(key(chest.asItem()));
            }
            for (var chest : java.util.List.of(ModBlocks.REINFORCED_TRAPPED_CHEST, ModBlocks.NETHERITE_TRAPPED_CHEST, ModBlocks.ENDERITE_TRAPPED_CHEST)) {
                builder(net.minecraft.tags.TagKey.<Item>create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "chests/trapped"))).add(key(chest.asItem()));
                builder(net.minecraft.tags.TagKey.<Item>create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "chests"))).add(key(chest.asItem()));
            }
        }
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            // Schallplatten (2026-10-03): ablegbar (B-Seite per Vorschlaghammer); die Oberwelt-Platten fallen wie die
            // Vanilla-Oberwelt-Platten, wenn ein Skelett einen Creeper toetet.
            for (Item disc : com.simplebuilding.util.MusicDiscs.items()) {
                builder(ModTags.Items.PLACEABLE_SMALL).add(key(disc));
            }
            builder(ItemTags.CREEPER_DROP_MUSIC_DISCS).add(key(ModItems.MUSIC_DISC_DRIFTWOOD)).add(key(ModItems.MUSIC_DISC_DAYBREAK));
        }
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            builder(ItemTags.RAILS).add(key(ModItems.ASTRAL_RAIL)).add(key(ModItems.NIHIL_RAIL));
        }
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            builder(ModTags.Items.PLACEABLE_SMALL).add(key(ModItems.YARN_BALL));
            builder(BlockItemTags.SMALL_FLOWERS.item()).add(key(ModItems.SILENT_DANDELION));
        }
        builder(ModTags.Items.CHISEL_TOOLS)
                .add(key(ModItems.STONE_CHISEL))
                .add(key(ModItems.COPPER_CHISEL))
                .add(key(ModItems.IRON_CHISEL))
                .add(key(ModItems.GOLD_CHISEL))
                .add(key(ModItems.DIAMOND_CHISEL))
                .add(key(ModItems.NETHERITE_CHISEL))
                .add(key(ModItems.ENDERITE_CHISEL));

        var octantBuilder = builder(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(key(ModItems.OCTANT));

        for (Item coloredRangefinder : ModItems.COLORED_OCTANT_ITEMS.values()) {
            octantBuilder.add(key(coloredRangefinder));
        }

        builder(ModTags.Items.CHISEL_AND_MINING_TOOLS)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .forceAddTag(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE);
        // Reichweite am Amboss: dazu Attractor (Zugradius) und Messuhr (Hoehenmesser), ohne Blockreichweite (RangeReach).
        builder(ModTags.Items.RANGE_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_AND_MINING_TOOLS)
                .add(key(ModItems.MAGNET))
                .add(key(ModItems.VELOCITY_GAUGE));

        // Mob-Koepfe (docs/MOBKOEPFE.md): Koepfe wie die Vanilla-Koepfe (Fluch der Bindung/des Verschwindens
        // ueber #equippable_enchantable/#vanishing_enchantable).
        var skulls = builder(ItemTags.SKULLS);
        for (Item head : com.simplebuilding.tweaks.item.TweaksItems.heads()) {
            skulls.add(key(head));
        }
        // Trial-Chamber-Koepfe: eine beliebige Zutat von Chunk-Loader I und Launchpad I (Vanilla-Zombie/-Skelett + Mod).
        var trialHeads = builder(com.simplebuilding.tweaks.heads.ModHeads.TRIAL_CHAMBER_HEADS);
        for (Item head : com.simplebuilding.tweaks.heads.ModHeads.trialChamberHeads()) {
            trialHeads.add(key(head));
        }
        // Eiswanderer-Schaedel: Pulverschnee friert nicht ein - wie ein Lederhelm (geheime Kopf-Faehigkeit).
        builder(ItemTags.FREEZE_IMMUNE_WEARABLES).add(key(com.simplebuilding.tweaks.item.TweaksItems.STRAY_SKULL));
        // Handbuecher aufs Lesepult und ins gemeisselte Buecherregal wie jedes beschriebene Buch.
        var lecternBooks = builder(ItemTags.LECTERN_BOOKS);
        var bookshelfBooks = builder(ItemTags.BOOKSHELF_BOOKS);
        for (com.simplebuilding.guide.GuideBooks.Book book : com.simplebuilding.guide.GuideBooks.Book.values()) {
            lecternBooks.add(key(com.simplebuilding.guide.GuideBooks.item(book)));
            bookshelfBooks.add(key(com.simplebuilding.guide.GuideBooks.item(book)));
        }

        // Rotator: Unbreaking ja, Mending nein (Besitzer 2026-09-28) - EnchantmentMixin liest diesen Tag.
        builder(ModTags.Items.XP_REPAIR_INCOMPATIBLE)
                .add(key(ModItems.ROTATOR));

        builder(ItemTags.DURABILITY_ENCHANTABLE)
                // Echo-Kompass (Simple Tweaks): Unbreaking/Mending wirken, siehe EchoCompassItem.
                .add(key(com.simplebuilding.tweaks.item.TweaksItems.ECHO_COMPASS))
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(key(ModItems.ORE_DETECTOR))
                .add(key(ModItems.ROTATOR))
                .add(key(ModItems.ENDERITE_SPEAR))
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        builder(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        builder(ModTags.Items.RADIUS_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .add(key(ModItems.ORE_DETECTOR));

        builder(ItemTags.MINING_LOOT_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        builder(ItemTags.VANISHING_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS);

        // Alle Stufen beider Behaelter (drei Buendel, vier Koecher) stehen in allen drei
        // Verzauberungs-Tags: diese Tags sind das supported_items von Drawer, Deep Pockets, Funnel,
        // Master Builder, Colour Palette und Constructor's Touch, und genau danach fragt der Amboss. Fehlt eine Stufe hier, nimmt sie
        // am Amboss nichts an - eine Aufwertung darf aber nichts wegnehmen.
        builder(ModTags.Items.BUNDLE_ENCHANTABLE)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.ENDERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.REINFORCED_QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER))
                .add(key(ModItems.ENDERITE_QUIVER));

        builder(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.ENDERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.REINFORCED_QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER))
                .add(key(ModItems.ENDERITE_QUIVER));

        builder(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.ENDERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.REINFORCED_QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER))
                .add(key(ModItems.ENDERITE_QUIVER))
                .addTag(ModTags.Items.BACKPACKS)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(key(ModItems.VELOCITY_GAUGE))
                // Amethystlinse: misst mit Beruehrung des Konstrukteurs (LaserPointerItem#measures)
                .add(key(com.simplebuilding.tweaks.item.TweaksItems.LASER_POINTER))
                .add(key(ModItems.ORE_DETECTOR))
                .add(key(ModItems.MAGNET))
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                // Keine Shulkerkiste: vorerst nicht verzauberbar (Besitzer 2026-10-02, ShulkerBoxEnchantTests).
                .add(key(Items.STICK));

        // Layout-Platzhalter der Kreativ-Tabs: in JEI, REI und EMI versteckt (Konventions-Tag).
        builder(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "hidden_from_recipe_viewers")))
                .add(key(ModItems.CREATIVE_SPACER))
                // Alte Spachtel (vor der Umbenennung in Meissel): kein Rezept, nur fuer alte Welten -
                // LegacySpatulaMigration macht sie beim Beitreten zu Meisseln (Besitzer 2026-09-28).
                .add(key(ModItems.STONE_SPATULA))
                .add(key(ModItems.COPPER_SPATULA))
                .add(key(ModItems.IRON_SPATULA))
                .add(key(ModItems.GOLD_SPATULA))
                .add(key(ModItems.DIAMOND_SPATULA))
                .add(key(ModItems.NETHERITE_SPATULA))
                // Alte, abgeloeste Stufenbloecke (netherite_flypad, enderite_flypad): nur fuer alte Welten.
                .add(key(com.simplebuilding.tweaks.block.TweaksBlocks.NETHERITE_FLYPAD.asItem()))
                .add(key(com.simplebuilding.tweaks.block.TweaksBlocks.ENDERITE_FLYPAD.asItem()))
                // Alte Spawn-Teleporter III und IV (bis 2026-09-28 fuenf Stufen)
                .add(key(com.simplebuilding.tweaks.block.TweaksBlocks.SPAWN_TELEPORTER_TIER_3.asItem()))
                .add(key(com.simplebuilding.tweaks.block.TweaksBlocks.SPAWN_TELEPORTER_TIER_4.asItem()))
                // Alte Elytra-Pads II und V (bis 2026-10-07 fuenf Stufen)
                .add(key(com.simplebuilding.tweaks.block.TweaksBlocks.REINFORCED_ELYTRA_PAD.asItem()))
                .add(key(com.simplebuilding.tweaks.block.TweaksBlocks.FINE_ELYTRA_PAD.asItem()))
                // Versteckt, Ende der Easter-Kette (docs/SIMPLETWEAKS-UEBERNAHME.md, Spoiler).
                .add(key(com.simplebuilding.tweaks.easter.EasterEggs.funnyStick()));

        // End-Paletten: Treppen, Stufen und Mauern auch als Item-Tags wie bei Vanilla.
        for (ModBlocks.EndPalette palette : ModBlocks.END_PALETTES) {
            palette.stairs().forEach(block -> builder(BlockItemTags.STAIRS.item()).add(key(block.asItem())));
            palette.slabs().forEach(block -> builder(BlockItemTags.SLABS.item()).add(key(block.asItem())));
            palette.walls().forEach(block -> builder(BlockItemTags.WALLS.item()).add(key(block.asItem())));
        }

        // Treppen und Stufen der Schachbretter (Schach 2026-10-06) ebenso.
        for (ModBlocks.CheckerShapes shapes : ModBlocks.CHECKER_SHAPES) {
            builder(BlockItemTags.STAIRS.item()).add(key(shapes.stairs().asItem()));
            builder(BlockItemTags.SLABS.item()).add(key(shapes.slab().asItem()));
        }

        // Alle acht Vanilla-Kupfertruhen (Oxidationsstufen, gewachst): Zutat der Verstaerkten Truhe.
        var copperChests = builder(ModTags.Items.COPPER_CHESTS);
        net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof net.minecraft.world.item.BlockItem blockItem
                        && blockItem.getBlock() instanceof net.minecraft.world.level.block.CopperChestBlock)
                .sorted(java.util.Comparator.comparing(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()))
                .forEach(item -> copperChests.add(key(item)));

        // Rucksaecke: eigene Tags fuer Tiefe Taschen, Trichter und Meisterbauer, damit Schublade
        // (bundle_enchantable) und Farbpalette (extra_inventory_items) sie nicht mitbekommen.
        builder(ModTags.Items.BACKPACKS)
                .add(key(ModItems.BACKPACK))
                .add(key(ModItems.REINFORCED_BACKPACK))
                .add(key(ModItems.NETHERITE_BACKPACK))
                .add(key(ModItems.ENDERITE_BACKPACK));
        builder(ModTags.Items.DEEP_POCKETS_ENCHANTABLE)
                .addTag(ModTags.Items.BUNDLE_ENCHANTABLE)
                .addTag(ModTags.Items.BACKPACKS);
        builder(ModTags.Items.FUNNEL_ENCHANTABLE)
                .addTag(ModTags.Items.BUNDLE_ENCHANTABLE)
                .addTag(ModTags.Items.BACKPACKS);
        builder(ModTags.Items.MASTER_BUILDER_ENCHANTABLE)
                .addTag(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)
                .addTag(ModTags.Items.BACKPACKS);
        // Gefaerbte Rucksaecke, Buendel und Koecher (minecraft:dyed_color) waescht der Wasserkessel wie
        // Lederruestung: Vanillas Kessel-Verhalten fuer diesen Tag nimmt nur die Farbe weg.
        var dyeable = com.simplebuilding.version.McVersion.VANILLA_DYEING
                ? builder(ModTags.Items.DYEABLE_STORAGE) : builder(ItemTags.CAULDRON_CAN_REMOVE_DYE);
        // 26.3 (VANILLA_DYEING): wie Vanillas Buendel - feste Farben, kein Auswaschen; der Tag speist die Faerberezepte.
        dyeable.addTag(ModTags.Items.BACKPACKS)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.ENDERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.REINFORCED_QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER))
                .add(key(ModItems.ENDERITE_QUIVER));
        builder(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .add(key(ModItems.STONE_SLEDGEHAMMER))
                .add(key(ModItems.COPPER_SLEDGEHAMMER))
                .add(key(ModItems.IRON_SLEDGEHAMMER))
                .add(key(ModItems.GOLD_SLEDGEHAMMER))
                .add(key(ModItems.DIAMOND_SLEDGEHAMMER))
                .add(key(ModItems.NETHERITE_SLEDGEHAMMER))
                .add(key(ModItems.ENDERITE_SLEDGEHAMMER));

        builder(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(key(ModItems.COPPER_BUILDING_WAND))
                .add(key(ModItems.IRON_BUILDING_WAND))
                .add(key(ModItems.GOLD_BUILDING_WAND))
                .add(key(ModItems.DIAMOND_BUILDING_WAND))
                .add(key(ModItems.NETHERITE_BUILDING_WAND))
                .add(key(ModItems.ENDERITE_BUILDING_WAND));

        builder(ModTags.Items.VEINMINE_ENCHANTABLE)
                .forceAddTag(ItemTags.PICKAXES)
                .forceAddTag(ItemTags.AXES);

        // Enderit-Werkzeuge und -Ruestung in die Vanilla-Werkzeug-/Ruestungs-Tags: aus ihnen leiten sich
        // die enchantable/*-Tags ab, ohne sie liess sich keine Verzauberung (ausser Haltbarkeit) anbringen.
        // (Den Speer traegt schon das handgeschriebene data/minecraft/tags/item/spears.json.)
        builder(ItemTags.SWORDS).add(key(ModItems.ENDERITE_SWORD));
        builder(ItemTags.PICKAXES).add(key(ModItems.ENDERITE_PICKAXE));
        builder(ItemTags.AXES).add(key(ModItems.ENDERITE_AXE));
        builder(ItemTags.SHOVELS).add(key(ModItems.ENDERITE_SHOVEL));
        builder(ItemTags.HOES).add(key(ModItems.ENDERITE_HOE));
        builder(ItemTags.HEAD_ARMOR).add(key(ModItems.ENDERITE_HELMET));
        builder(ItemTags.CHEST_ARMOR).add(key(ModItems.ENDERITE_CHESTPLATE));
        builder(ItemTags.LEG_ARMOR).add(key(ModItems.ENDERITE_LEGGINGS));
        builder(ItemTags.FOOT_ARMOR).add(key(ModItems.ENDERITE_BOOTS));

        TagKey<Item> TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("trim_templates"));

        builder(TRIM_TEMPLATES)
                .add(key(ModItems.GLOWING_TRIM_TEMPLATE))
                .add(key(ModItems.EMITTING_TRIM_TEMPLATE))
                .add(key(ModItems.PULSATING_TRIM_TEMPLATE));

        // Optional: Damit der Leuchtbeutel generell als "Trim Material" erkannt wird (hilft bei der GUI-Validierung)
        builder(ItemTags.TRIM_MATERIALS)
                .add(key(ModItems.ASTRALIT_DUST))
                .add(key(ModItems.NIHILITH_SHARD))
                .add(key(ModItems.ENDERITE_INGOT))
                .add(key(Items.GLOW_INK_SAC))
                .add(key(Items.GLOWSTONE_DUST));

        builder(ItemTags.TRIMMABLE_ARMOR)
                .add(key(ModItems.ENDERITE_HELMET))
                .add(key(ModItems.ENDERITE_CHESTPLATE))
                .add(key(ModItems.ENDERITE_LEGGINGS))
                .add(key(ModItems.ENDERITE_BOOTS));

        // No double experience for the lossless cracked-diamond crafting cycle.
        builder(ModTags.Items.FURNACE_BONUS_EXCLUDED)
                .add(key(ModItems.CRACKED_DIAMOND));

        addVoidProtected();
        addPlaceableSmall();
        addEnderiteIngotTier();
    }

    /**
     * Befuellt {@link ModTags.Items#ENDERITE_ITEMS} deterministisch aus der Item-Registry statt
     * aus einer handgepflegten Liste: alles, was {@link ModTags.Items#isEnderiteItemByRule(Identifier)}
     * akzeptiert. Neue Enderit-Items sind damit automatisch gegen den Void geschuetzt und liegen
     * doppelt so lange, ohne dass jemand daran denken muss. {@link ModTags.Items#VOID_PROTECTED} und
     * {@link ModTags.Items#DOUBLE_DESPAWN_TIME} enthalten nur diesen Tag.
     */
    /** Ablegbare Kleinteile (2026-10-02): Vanilla-Barren, -Klumpen, -Edelsteine, Stock, Ziegel, Feuerstein und die Mod-Teile. */
    private void addPlaceableSmall() {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            return;
        }
        var tag = builder(ModTags.Items.PLACEABLE_SMALL);
        for (net.minecraft.world.item.Item item : java.util.List.of(
                net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.FLINT,
                net.minecraft.world.item.Items.BRICK, net.minecraft.world.item.Items.NETHER_BRICK, net.minecraft.world.item.Items.RESIN_BRICK,
                net.minecraft.world.item.Items.COPPER_INGOT, net.minecraft.world.item.Items.IRON_INGOT, net.minecraft.world.item.Items.GOLD_INGOT,
                net.minecraft.world.item.Items.NETHERITE_INGOT, net.minecraft.world.item.Items.COPPER_NUGGET, net.minecraft.world.item.Items.IRON_NUGGET,
                net.minecraft.world.item.Items.GOLD_NUGGET, net.minecraft.world.item.Items.DIAMOND, net.minecraft.world.item.Items.EMERALD,
                net.minecraft.world.item.Items.LAPIS_LAZULI, net.minecraft.world.item.Items.AMETHYST_SHARD, net.minecraft.world.item.Items.QUARTZ,
                net.minecraft.world.item.Items.PRISMARINE_SHARD, net.minecraft.world.item.Items.ECHO_SHARD, net.minecraft.world.item.Items.FIRE_CHARGE,
                com.simplebuilding.items.ModItems.STONE_PEBBLE, com.simplebuilding.items.ModItems.FLINT_CHIP,
                com.simplebuilding.items.ModItems.OBSIDIAN_CHIP, com.simplebuilding.items.ModItems.FIRE_CHIP,
                com.simplebuilding.items.ModItems.ICE_CHIP,
                com.simplebuilding.items.ModItems.DIAMOND_PEBBLE, com.simplebuilding.items.ModItems.NETHERITE_NUGGET,
                com.simplebuilding.items.ModItems.ENDERITE_NUGGET, com.simplebuilding.items.ModItems.ENDERITE_INGOT)) {
            tag.add(BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        }
        // Placeables v2 (Besitzer 2026-10-03): weitere vanilla-nahe Kleinteile ohne eigenes Setzen (keine Bloecke, keine
        // Wurf-Items). Toepferscherben bewusst nicht (23 Varianten, gehoeren auf den Krug). Kerzen und Seegurken stehen
        // NICHT im Tag: sie bleiben allein Vanilla und mischen nur (PlacedSmallParts#isBlockPart).
        for (net.minecraft.world.item.Item item : java.util.List.of(
                net.minecraft.world.item.Items.BONE, net.minecraft.world.item.Items.FEATHER,
                net.minecraft.world.item.Items.ARROW, net.minecraft.world.item.Items.SPECTRAL_ARROW,
                net.minecraft.world.item.Items.BLAZE_ROD, net.minecraft.world.item.Items.BREEZE_ROD,
                net.minecraft.world.item.Items.GLOWSTONE_DUST, net.minecraft.world.item.Items.GLOW_INK_SAC,
                net.minecraft.world.item.Items.PRISMARINE_CRYSTALS, net.minecraft.world.item.Items.NETHER_STAR,
                net.minecraft.world.item.Items.RABBIT_FOOT, net.minecraft.world.item.Items.TURTLE_SCUTE,
                net.minecraft.world.item.Items.ARMADILLO_SCUTE, net.minecraft.world.item.Items.DISC_FRAGMENT_5,
                net.minecraft.world.item.Items.GHAST_TEAR)) {
            tag.add(BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        }
        // Eigene Kleinmaterialien, damit alle eigenen Kleinteile untereinander mischen (nur registrierte).
        for (net.minecraft.world.item.Item item : java.util.Arrays.asList(
                com.simplebuilding.items.ModItems.NIHILITH_SHARD, com.simplebuilding.items.ModItems.ASTRALIT_DUST,
                com.simplebuilding.items.ModItems.ENDER_QUARTZ, com.simplebuilding.items.ModItems.RAW_ENDERITE,
                com.simplebuilding.items.ModItems.ENDERITE_SCRAP, com.simplebuilding.items.ModItems.CRACKED_DIAMOND,
                com.simplebuilding.items.ModItems.SAGE_ORB)) {
            if (item != null) {
                tag.add(BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
            }
        }
        // Shulkerschalen (Vanilla und Stufen) liegen abgelegt, damit ein Klumpen sie aufwerten kann (ShulkerShells).
        tag.add(BuiltInRegistries.ITEM.getResourceKey(net.minecraft.world.item.Items.SHULKER_SHELL).orElseThrow());
        for (net.minecraft.world.item.Item shell : com.simplebuilding.util.ShulkerShells.tierShells()) {
            tag.add(BuiltInRegistries.ITEM.getResourceKey(shell).orElseThrow());
        }
    }

    private void addVoidProtected() {
        // Sortiert, damit die erzeugte JSON unabhaengig von der Registrierungsreihenfolge ist.
        Set<Identifier> ids = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (ModTags.Items.isEnderiteItemByRule(id)) {
                ids.add(id);
            }
        }

        var enderite = builder(ModTags.Items.ENDERITE_ITEMS);
        for (Identifier id : ids) {
            enderite.add(ResourceKey.create(Registries.ITEM, id));
        }
        builder(ModTags.Items.VOID_PROTECTED).addTag(ModTags.Items.ENDERITE_ITEMS);
        builder(ModTags.Items.DOUBLE_DESPAWN_TIME).addTag(ModTags.Items.ENDERITE_ITEMS);
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            for (net.minecraft.world.item.Item scrap : java.util.List.of(com.simplebuilding.items.ModItems.DIMENSIONAL_SCRAP_ITEM,
                    com.simplebuilding.items.ModItems.NETHER_DIMENSIONAL_SCRAP_ITEM, com.simplebuilding.items.ModItems.END_DIMENSIONAL_SCRAP_ITEM)) {
                builder(ModTags.Items.QUADRUPLE_DESPAWN_TIME).add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getResourceKey(scrap).orElseThrow());
                builder(ModTags.Items.INDESTRUCTIBLE).add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getResourceKey(scrap).orElseThrow());
                builder(ModTags.Items.VOID_PROTECTED).add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getResourceKey(scrap).orElseThrow());
            }
        }
    }

    /** Befuellt {@link ModTags.Items#ENDERITE_INGOT_TIER} nach derselben Art wie den Void-Tag. */
    private void addEnderiteIngotTier() {
        Set<Identifier> ids = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (ModTags.Items.isEnderiteIngotTierByRule(id)) {
                ids.add(id);
            }
        }

        var tier = builder(ModTags.Items.ENDERITE_INGOT_TIER);
        for (Identifier id : ids) {
            tier.add(ResourceKey.create(Registries.ITEM, id));
        }
    }
}
