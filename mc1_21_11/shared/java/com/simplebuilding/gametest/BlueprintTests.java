package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintBuilder;
import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintExamples;
import com.simplebuilding.blueprint.BlueprintMaterials;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.blueprint.BlueprintScanner;
import com.simplebuilding.blueprint.BlueprintTiers;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.networking.BlueprintEditPayload;
import com.simplebuilding.networking.BlueprintRotatePayload;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.util.OctantShape;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * Die Blaupause (docs/BLUEPRINT.md): Bausprache, Materialliste, Scan am Kartentisch, Bauen mit
 * dem Baustab, Drehung, Stufengrenze, Editor-Paket und Rezept.
 *
 * <p>Der Bau-Ort ist in allen Bautests derselbe: geklickt wird die Oberseite von {@link #CLICKED}
 * (Obsidian), gebaut wird also ab {@code CLICKED.above()}; das Bauwerk liegt mittig (x) auf dem
 * Zielblock und waechst in Blickrichtung.
 */
public final class BlueprintTests {
    private static final BlockPos CLICKED = new BlockPos(3, 1, 3);
    private static final BlockPos TARGET = CLICKED.above();

    private BlueprintTests() {
    }

    // =====================================================================================
    // BAUSPRACHE
    // =====================================================================================

    /**
     * Der Parser liest die Beispiele der Spezifikation (Eckenform des Besitzers, Achsenform,
     * Eigenschaften, Alias, Wiederholung, Raeumen mit air), meldet Fehler mit Schluessel und Stelle,
     * und der Serialisierer schreibt ein Modell so, dass es genau so zurueckkommt - mit
     * zusammengefassten Quadern statt einer Zeile je Block.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Bereich, der ein Ende auslaesst (a..b nicht
     * inklusiv), Eigenschaften, die beim Schreiben verloren gehen oder beim Lesen nicht greifen,
     * ein Alias, der nicht aufgeloest wird, air, das nicht raeumt, eine Wiederholung mit falscher
     * Schrittweite, ein Serialisierer, der nicht zusammenfasst.
     */
    public static void codeRoundTripsAndParsesTheSpecExamples(GameTestHelper helper) {
        // Eckenform wie im Wunsch des Besitzers: stone 1,1,1..1,1,5 -> fuenf Steine in z.
        BlueprintCode.ParseResult owner = BlueprintCode.parse("stone 1,1,1..1,1,5");
        helper.assertTrue(owner.ok(), "the owner's example did not parse: " + owner.problems());
        helper.assertTrue(owner.model().size() == 5, "stone 1,1,1..1,1,5 should be five blocks, got " + owner.model().size());
        for (int z = 1; z <= 5; z++) {
            helper.assertTrue(owner.model().get(1, 1, z) == Blocks.STONE.defaultBlockState(), "no stone at 1,1," + z);
        }

        String code = String.join("\n",
                "# Huette",
                "$treppe = oak_stairs[facing=east,half=top]",
                "minecraft:cobblestone 0..4,0,0..4",
                "oak_planks 0..4,1..2,0..4",
                "air 1..3,1..2,1..3          # innen hohl",
                "$treppe 0..4,3,0",
                "oak_fence 0,4,0*3@2,0,0");
        BlueprintCode.ParseResult hut = BlueprintCode.parse(code);
        helper.assertTrue(hut.ok(), "the hut example did not parse: " + hut.problems());
        BlueprintModel m = hut.model();
        helper.assertTrue(m.get(2, 0, 2) == Blocks.COBBLESTONE.defaultBlockState(), "floor missing");
        helper.assertTrue(m.get(2, 1, 2) == null, "air did not clear the inside of the hut");
        helper.assertTrue(m.get(0, 2, 2) == Blocks.OAK_PLANKS.defaultBlockState(), "wall missing");
        BlockState stair = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP);
        helper.assertTrue(stair.equals(m.get(4, 3, 0)), "the alias did not carry its properties: " + m.get(4, 3, 0));
        for (int x : new int[]{0, 2, 4}) {
            helper.assertTrue(m.get(x, 4, 0) != null && m.get(x, 4, 0).is(Blocks.OAK_FENCE), "repetition missing a fence at x=" + x);
        }
        helper.assertTrue(m.get(1, 4, 0) == null && m.get(3, 4, 0) == null, "repetition filled the gaps");
        int expected = 25 + 2 * 25 - 2 * 9 + 5 + 3;
        helper.assertTrue(m.size() == expected, "hut has " + m.size() + " blocks instead of " + expected);

        // Fehler: unbekannter Block, Koordinate ausserhalb, falsche Eigenschaft - mit Schluessel und Zeile.
        BlueprintCode.ParseResult bad = BlueprintCode.parse("stne 0,0,0\nstone 0,0,256\noak_stairs[facing=up] 0,0,0\nstone");
        List<String> keys = bad.problems().stream().map(BlueprintCode.Problem::key).toList();
        helper.assertTrue(keys.equals(List.of("unknown_block", "coordinate_range", "bad_value", "no_region")),
                "unexpected problems " + keys);
        helper.assertTrue(bad.problems().get(1).line() == 1 && bad.problems().get(0).start() == 0 && bad.problems().get(0).end() == 4,
                "problem positions are off: " + bad.problems());
        helper.assertTrue(bad.styles()[0] == BlueprintCode.STYLE_ERROR && hut.styles()[0] == BlueprintCode.STYLE_COMMENT,
                "the highlighter did not mark the error / the comment");

        // Rundreise: serialisieren, neu lesen, gleiches Modell - und zusammengefasst.
        String written = BlueprintCode.serialize(m);
        BlueprintCode.ParseResult back = BlueprintCode.parse(written);
        helper.assertTrue(back.ok(), "serialised code does not parse: " + back.problems() + "\n" + written);
        helper.assertTrue(back.model().equals(m), "round trip changed the model:\n" + written);
        helper.assertTrue(written.lines().filter(l -> !l.startsWith("#")).count() <= 10,
                "the serialiser did not merge runs into boxes:\n" + written);
        BlueprintModel slab = BlueprintCode.parse("stone 0..4,0,0..4").model();
        helper.assertTrue(BlueprintCode.countBoxes(slab) == 1, "a 5x1x5 plate should be one box");
        helper.assertTrue(BlueprintCode.serialize(slab).endsWith("stone 0..4,0,0..4"), "unexpected code: " + BlueprintCode.serialize(slab));
        TestCleanup.succeed(helper);
    }

    /**
     * Grenzen: der Code hat eine Hoechstlaenge (sonst passt er nicht mehr in das Item), das Raster
     * reicht von 0 bis 255, das Ausroll-Budget (4 194 304 Stellen) faengt Riesen-Wiederholungen vor
     * dem Ausrollen ab, die Baustab-Stufen schaffen je einen Wuerfel (16, 32, 48, 64, 128, 256), und
     * der Kartentisch nimmt Auswahlen bis 256 je Kante an, 257 nicht.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine fehlende Laengenpruefung, ein Raster, das
     * nicht bis 255 reicht oder darueber hinaus, eine Wiederholung, die erst ausgerollt und dann
     * gezaehlt wird, eine vertauschte Stufenzuordnung, eine falsche Scan-Grenze.
     */
    public static void sizeLimitsCapTheCodeAndMapWandTiers(GameTestHelper helper) {
        String tooLong = "stone 0,0,0\n" + "#".repeat(BlueprintCode.MAX_CODE_LENGTH);
        List<String> keys = BlueprintCode.parse(tooLong).problems().stream().map(BlueprintCode.Problem::key).toList();
        helper.assertTrue(keys.contains("too_long"), "an over-long code was accepted: " + keys);

        BlueprintCode.ParseResult corner = BlueprintCode.parse("stone 255,255,255 0,0,0");
        helper.assertTrue(corner.ok() && corner.model().maxEdge() == 256, "the far corner 255,255,255 is not part of the grid: " + corner.problems());
        List<String> outside = BlueprintCode.parse("stone 0,256,0").problems().stream().map(BlueprintCode.Problem::key).toList();
        helper.assertTrue(outside.equals(List.of("coordinate_range")), "256 was accepted as a coordinate: " + outside);

        long start = System.nanoTime();
        BlueprintCode.ParseResult huge = BlueprintCode.parse("stone 0..255,0..255,0..64");
        helper.assertTrue(huge.problems().stream().anyMatch(p -> p.key().equals("too_many_cells")),
                "a 256x256x65 block (over the budget) was not refused: " + huge.problems());
        helper.assertTrue(huge.model().isEmpty(), "the refused region was applied anyway");
        BlueprintCode.ParseResult repeated = BlueprintCode.parse("stone 0..255,0..255,0..15*5@0,0,16");
        helper.assertTrue(repeated.problems().stream().anyMatch(p -> p.key().equals("too_many_cells")) && repeated.model().isEmpty(),
                "a repetition over the budget was not refused before expanding: " + repeated.problems());
        helper.assertTrue(System.nanoTime() - start < 2_000_000_000L, "refusing the giants took too long - they were expanded first");

        int[][] expected = {
                {edge(ModItems.COPPER_BUILDING_WAND), 16}, {edge(ModItems.IRON_BUILDING_WAND), 32},
                {edge(ModItems.GOLD_BUILDING_WAND), 48}, {edge(ModItems.DIAMOND_BUILDING_WAND), 64},
                {edge(ModItems.NETHERITE_BUILDING_WAND), 128}, {edge(ModItems.ENDERITE_BUILDING_WAND), 256}};
        for (int[] pair : expected) {
            helper.assertTrue(pair[0] == pair[1], "wand edge " + pair[0] + " instead of " + pair[1]);
        }
        helper.assertTrue(BlueprintTiers.tierIndexFor(16) == 0 && BlueprintTiers.tierIndexFor(17) == 1
                        && BlueprintTiers.tierIndexFor(128) == 4 && BlueprintTiers.tierIndexFor(129) == 5
                        && BlueprintTiers.tierIndexFor(256) == 5 && BlueprintTiers.tierIndexFor(257) == -1,
                "tier lookup is off");
        BlueprintModel line = BlueprintCode.parse("stone 0..16,0,0").model();
        helper.assertTrue(line.maxEdge() == 17 && line.sizeX() == 17 && line.sizeY() == 1, "bounding box of a 17 line is wrong");

        // Scan-Grenze: 256 je Kante geht (hoechstens "nicht geladen"), 257 ist zu gross.
        BlockPos table = helper.absolutePos(new BlockPos(3, 1, 3));
        Object ok = BlueprintScanner.start(helper.getLevel(), table,
                octant(helper, new BlockPos(0, 1, 0), new BlockPos(255, 1, 0)), new ItemStack(ModItems.BLUEPRINT));
        helper.assertTrue(!"too_large".equals(scanError(ok)), "a 256 wide selection was refused as too large");
        Object big = BlueprintScanner.start(helper.getLevel(), table,
                octant(helper, new BlockPos(0, 1, 0), new BlockPos(256, 1, 0)), new ItemStack(ModItems.BLUEPRINT));
        helper.assertTrue("too_large".equals(scanError(big)), "a 257 wide selection was accepted: " + scanError(big));
        TestCleanup.succeed(helper);
    }

    /** Der Schluessel einer Scan-Ablehnung ("too_large" ...), sonst {@code null}. */
    private static String scanError(Object started) {
        if (started instanceof BlueprintScanner.Outcome outcome && outcome.error() != null
                && outcome.error().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            return t.getKey().substring(t.getKey().lastIndexOf('.') + 1);
        }
        return null;
    }

    private static int edge(Item wand) {
        return BlueprintTiers.edgeFor((com.simplebuilding.items.custom.BuildingWandItem) wand);
    }

    /**
     * Die Materialliste zaehlt Items, nicht Bloecke: eine doppelte Stufe kostet zwei Stufen, eine
     * Tuer nur ihre untere Haelfte, die Wandfackel eine Fackel; Wasser gibt es nur im
     * Kreativmodus. Sortiert nach Menge, groesste zuerst.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Zaehler, der Zustaende statt Items zaehlt,
     * die fehlende Sonderregel fuer Stufen oder Tueren, eine falsche Sortierung.
     */
    public static void materialListCountsItemsSortedByAmount(GameTestHelper helper) {
        BlueprintModel model = new BlueprintModel();
        for (int x = 0; x < 10; x++) {
            model.set(x, 0, 0, Blocks.STONE.defaultBlockState());
        }
        BlockState doubleSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        model.set(0, 1, 0, doubleSlab);
        model.set(1, 1, 0, doubleSlab);
        model.set(2, 1, 0, Blocks.OAK_SLAB.defaultBlockState());
        model.set(4, 1, 0, Blocks.OAK_DOOR.defaultBlockState());
        model.set(4, 2, 0, Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        model.set(6, 1, 0, Blocks.WALL_TORCH.defaultBlockState());
        model.set(7, 1, 0, Blocks.TORCH.defaultBlockState());
        model.set(8, 1, 0, Blocks.WATER.defaultBlockState());

        List<BlueprintMaterials.Entry> list = BlueprintMaterials.list(model);
        String shown = list.stream().map(e -> (e.block() != null ? e.block().toString() : e.item().toString()) + "=" + e.count()).toList().toString();
        helper.assertTrue(list.size() == 5, "expected stone, slab, torch, door and creative water: " + shown);
        helper.assertTrue(list.get(0).item() == Items.STONE && list.get(0).count() == 10, "stone first with 10: " + shown);
        helper.assertTrue(list.get(1).item() == Items.OAK_SLAB && list.get(1).count() == 5, "5 slabs (2 doubles + 1): " + shown);
        helper.assertTrue(list.get(2).item() == Items.TORCH && list.get(2).count() == 2, "wall torch + torch = 2 torches: " + shown);
        helper.assertTrue(list.get(3).item() == Items.OAK_DOOR && list.get(3).count() == 1, "one door for both halves: " + shown);
        helper.assertTrue(list.get(4).item() == Items.AIR && list.get(4).block() == Blocks.WATER && list.get(4).count() == 1,
                "water should be listed as creative-only: " + shown);
        helper.assertTrue(BlueprintMaterials.totalItems(model) == 18, "total items " + BlueprintMaterials.totalItems(model));
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // SCAN AM KARTENTISCH
    // =====================================================================================

    /**
     * Oktant oben, leere Blaupause unten: rechts liegt die gefuellte Blaupause, deren Code genau die
     * Bloecke der Auswahl beschreibt (Luft uebersprungen, Truheninhalt nicht). Nehmen verbraucht
     * nur die Blaupause. Zu weit entfernter Tisch und signierte Blaupause liefern nichts.
     *
     * <p><strong>Was diesen Test bricht:</strong> das Mixin greift nicht (Vanilla lehnt den Oktanten
     * ab), der Scan verschiebt oder verliert Bloecke oder Zustaende, der Ergebnis-Slot verbraucht
     * den Oktanten, die Entfernungs- oder Signatur-Pruefung fehlt.
     */
    public static void cartographyTableScansTheOctantSelection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, false);
        clear(helper);
        BlockState stair = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 2), stair);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.CHEST);
        if (level.getBlockEntity(helper.absolutePos(new BlockPos(3, 3, 2))) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        }
        BlueprintModel expected = new BlueprintModel();
        expected.set(0, 0, 0, Blocks.STONE.defaultBlockState());
        expected.set(1, 0, 0, Blocks.STONE.defaultBlockState());
        expected.set(0, 1, 0, stair);
        expected.set(1, 1, 0, Blocks.CHEST.defaultBlockState());

        ItemStack octant = octant(helper, new BlockPos(2, 2, 2), new BlockPos(4, 4, 3));
        CartographyTableMenu menu = new CartographyTableMenu(7, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(5, 1, 5))));
        helper.assertTrue(menu.getSlot(0).mayPlace(octant), "the cartography table refuses the octant");
        helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(ModItems.BLUEPRINT)), "the cartography table refuses the blueprint");
        menu.getSlot(0).set(octant);
        menu.getSlot(1).set(new ItemStack(ModItems.BLUEPRINT, 2));
        ItemStack result = menu.getSlot(2).getItem();
        helper.assertTrue(result.is(ModItems.BLUEPRINT) && result.getCount() == 1, "no filled blueprint in the result slot: " + result);
        String code = BlueprintItem.content(result).code();
        BlueprintCode.ParseResult parsed = BlueprintCode.parse(code);
        helper.assertTrue(parsed.ok() && parsed.model().equals(expected), "scanned code does not match the selection:\n" + code
                + "\n" + parsed.model().describe());
        helper.assertTrue(!code.contains("diamond"), "the chest's contents leaked into the code");

        menu.getSlot(2).onTake(player, result);
        helper.assertTrue(menu.getSlot(1).getItem().getCount() == 1, "taking the result did not use exactly one blueprint");
        helper.assertTrue(menu.getSlot(0).getItem().getItem() == ModItems.OCTANT, "taking the result used up the octant");

        // Zu weit weg: Tisch 40 Bloecke neben der Auswahl.
        CartographyTableMenu far = new CartographyTableMenu(8, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(45, 1, 3))));
        far.getSlot(0).set(octant.copy());
        far.getSlot(1).set(new ItemStack(ModItems.BLUEPRINT));
        helper.assertTrue(far.getSlot(2).getItem().isEmpty(), "a table 40 blocks away still scanned");

        // Signierte Blaupause wird nicht ueberschrieben.
        ItemStack signed = new ItemStack(ModItems.BLUEPRINT);
        signed.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent("stone 0,0,0", "Alt", "Someone", true));
        CartographyTableMenu locked = new CartographyTableMenu(9, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(5, 1, 5))));
        locked.getSlot(0).set(octant.copy());
        locked.getSlot(1).set(signed);
        helper.assertTrue(locked.getSlot(2).getItem().isEmpty(), "a signed blueprint was overwritten by a scan");
        clear(helper);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // BAUEN
    // =====================================================================================

    /**
     * Ueberleben: gebaut wird nur, was im Inventar liegt; eine Stelle mit dem richtigen Block und
     * eine belegte Stelle bleiben unberuehrt, fehlendes Material wird ausgelassen und laesst sich
     * spaeter nachholen. Jeder gesetzte Block kostet einen Haltbarkeitspunkt.
     *
     * <p><strong>Was diesen Test bricht:</strong> Bauen ohne Material, Ueberschreiben belegter
     * Stellen, doppeltes Setzen einer schon richtigen Stelle, ein Abbruch statt Auslassen, falsche
     * Verrechnung von Material oder Haltbarkeit.
     */
    public static void buildPlacesOnlyAvailableBlocksAndSkipsExistingOnes(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);
        clear(helper);
        helper.setBlock(TARGET.west(), Blocks.STONE);   // schon richtig
        helper.setBlock(TARGET.east(), Blocks.DIRT);    // belegt
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack stone = new ItemStack(Items.STONE, 5);
        ItemStack blueprint = blueprint("stone 0..2,0,0\nglass 1,1,0");
        hold(player, wand, blueprint, stone);

        // Glas fehlt: der erste Klick warnt nur (Zwei-Klick-Regel), der zweite baut das Vorhandene.
        BlueprintBuilder.Result warning = build(helper, player, wand, blueprint);
        helper.assertTrue(warning != null && warning.warned() && helper.getBlockState(TARGET).isAir(), "the first click did not only warn: " + warning);
        BlueprintBuilder.Result first = build(helper, player, wand, blueprint);
        helper.assertTrue(first != null && !first.warned(), "the build was refused");
        helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE), "the free stone position was not built");
        helper.assertTrue(helper.getBlockState(TARGET.east()).is(Blocks.DIRT), "an occupied position was overwritten");
        helper.assertTrue(helper.getBlockState(TARGET.above()).isAir(), "glass appeared without glass in the inventory");
        helper.assertTrue(first.placed() == 1 && first.already() == 1 && first.occupied() == 1 && first.missing() == 1,
                "unexpected tally " + first);
        helper.assertTrue(stone.getCount() == 4, "stone count " + stone.getCount() + " instead of 4");
        helper.assertTrue(wand.getDamageValue() == 1, "wand damage " + wand.getDamageValue() + " instead of 1");

        player.getInventory().setItem(2, new ItemStack(Items.GLASS, 3));
        BlueprintBuilder.Result second = build(helper, player, wand, blueprint);
        helper.assertTrue(second != null && second.placed() == 1, "coming back did not fill the gap: " + second);
        helper.assertTrue(helper.getBlockState(TARGET.above()).is(Blocks.GLASS), "the missing glass was not placed on the second click");
        helper.assertTrue(stone.getCount() == 4, "the second click spent stone on positions that were already right");
        helper.assertTrue(player.getInventory().getItem(2).getCount() == 2, "glass not consumed");
        helper.assertTrue(wand.getDamageValue() == 2, "wand damage " + wand.getDamageValue() + " instead of 2");
        clear(helper);
        TestCleanup.succeed(helper);
    }

    /**
     * Ausrichtung: nach Sueden blickend entsteht das Bauwerk wie geschrieben; Strg+Mausrad (das
     * Paket) dreht es um Viertel, und auch Blockzustaende drehen mit (Treppe). Nach Osten blickend
     * zeigt die lokale z-Achse nach Osten. Das Paket wirkt nur mit dem Baustab in der Haupthand.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine vertauschte Drehrichtung, Zustaende, die
     * nicht mitgedreht werden, ein Paket-Handler, der ohne Baustab dreht oder nicht zyklisch zaehlt.
     */
    public static void rotationTurnsTheBuildAndTheScrollPacketStepsIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack blueprint = blueprint("stone 0,0,0..0,0,2\noak_stairs[facing=south] 0,1,2");
        hold(player, wand, blueprint);

        clear(helper);
        build(helper, player, wand, blueprint);
        for (int z = 0; z <= 2; z++) {
            helper.assertTrue(helper.getBlockState(TARGET.south(z)).is(Blocks.STONE), "facing south: no stone at +z " + z);
        }
        helper.assertTrue(stairFacing(helper, TARGET.south(2).above()) == Direction.SOUTH, "facing south: stair not facing south");

        ModMessageHandlers.handleBlueprintRotate(new BlueprintRotatePayload(1), player);
        helper.assertTrue(BlueprintBuilder.rotationSteps(blueprint) == 1, "the scroll packet did not turn the blueprint");
        clear(helper);
        build(helper, player, wand, blueprint);
        for (int d = 0; d <= 2; d++) {
            helper.assertTrue(helper.getBlockState(TARGET.west(d)).is(Blocks.STONE), "one quarter: no stone at -x " + d);
        }
        helper.assertTrue(stairFacing(helper, TARGET.west(2).above()) == Direction.WEST, "one quarter: stair did not turn to west");

        ModMessageHandlers.handleBlueprintRotate(new BlueprintRotatePayload(-1), player);
        ModMessageHandlers.handleBlueprintRotate(new BlueprintRotatePayload(-1), player);
        helper.assertTrue(BlueprintBuilder.rotationSteps(blueprint) == 3, "rotation does not wrap around: " + BlueprintBuilder.rotationSteps(blueprint));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        ModMessageHandlers.handleBlueprintRotate(new BlueprintRotatePayload(1), player);
        helper.assertTrue(BlueprintBuilder.rotationSteps(blueprint) == 3, "the blueprint turned without a wand in the main hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        blueprint.remove(ModDataComponentTypes.BLUEPRINT_ROTATION);

        // Jede Blickrichtung: die lokale z-Achse zeigt dorthin, die Treppe (lokal nach Sueden)
        // dreht mit - Osten, Westen und Norden, damit keine Zuordnung unbemerkt kippt.
        float[] yaws = {-90f, 90f, 180f};
        Direction[] facings = {Direction.EAST, Direction.WEST, Direction.NORTH};
        for (int i = 0; i < yaws.length; i++) {
            Direction facing = facings[i];
            player.setYRot(yaws[i]);
            helper.assertTrue(player.getDirection() == facing, "mock player does not face " + facing);
            clear(helper);
            build(helper, player, wand, blueprint);
            for (int d = 0; d <= 2; d++) {
                helper.assertTrue(helper.getBlockState(TARGET.relative(facing, d)).is(Blocks.STONE),
                        "facing " + facing + ": no stone " + d + " blocks ahead");
            }
            helper.assertTrue(stairFacing(helper, TARGET.relative(facing, 2).above()) == facing,
                    "facing " + facing + ": stair not turned to " + facing);
        }
        clear(helper);
        TestCleanup.succeed(helper);
    }

    /**
     * Stufengrenze: ein 17 Bloecke langes Bauwerk ist zu gross fuer den Kupfer-Baustab (16) - der
     * Klick wird abgelehnt und setzt nichts; der Eisen-Baustab (32) baut es.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine fehlende oder um eins verschobene
     * Groessenpruefung, eine Pruefung, die erst nach dem Setzen greift.
     */
    public static void wandTierRefusesBlueprintsLargerThanItsCube(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        clear(helper);
        ItemStack blueprint = blueprint("stone 0,0,0 0,16,0");
        ItemStack copper = new ItemStack(ModItems.COPPER_BUILDING_WAND);
        hold(player, copper, blueprint);
        helper.assertTrue(build(helper, player, copper, blueprint) == null, "the copper wand accepted a 17 block blueprint");
        helper.assertTrue(helper.getBlockState(TARGET).isAir(), "the refused build still placed something");

        ItemStack iron = new ItemStack(ModItems.IRON_BUILDING_WAND);
        hold(player, iron, blueprint);
        BlueprintBuilder.Result result = build(helper, player, iron, blueprint);
        helper.assertTrue(result != null && result.placed() == 2, "the iron wand did not build the 17 block blueprint: " + result);
        helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE) && helper.getBlockState(TARGET.above(16)).is(Blocks.STONE),
                "the iron wand's build is not where it should be");
        helper.setBlock(TARGET.above(16), Blocks.AIR);
        clear(helper);
        TestCleanup.succeed(helper);
    }

    /**
     * Der Scan folgt der Figur des Oktanten, nicht seinem Rahmen: in einem massiven 5x5x5-Steinwuerfel
     * nimmt eine Kugel-Auswahl genau die Bloecke auf, die {@code OctantShape} (dieselbe Quelle wie die
     * Vorschau im Client) zur Kugel zaehlt - die Ecken fehlen, die Mitte ist da; ein liegender
     * Zylinder (Ausrichtung +X) ebenso.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Scan, der die Bounding Box statt der Figur
     * nimmt, eine Form- oder Ausrichtungs-Abfrage, die von der Vorschau abweicht.
     */
    public static void scanFollowsTheOctantShape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        clear(helper);
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos table = helper.absolutePos(new BlockPos(7, 1, 7));
        for (String[] shape : new String[][]{{"SPHERE", "1"}, {"CYLINDER", "0"}}) {
            ItemStack octant = octant(helper, new BlockPos(1, 1, 1), new BlockPos(5, 5, 5));
            CompoundTag nbt = OctantShape.data(octant);
            nbt.putString("Shape", shape[0]);
            nbt.putInt("Orientation", Integer.parseInt(shape[1]));
            octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            java.util.function.Predicate<BlockPos> inShape = OctantShape.of(octant);
            BlueprintModel expected = new BlueprintModel();
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    for (int z = 0; z < 5; z++) {
                        if (inShape.test(helper.absolutePos(new BlockPos(x + 1, y + 1, z + 1)))) {
                            expected.set(x, y, z, Blocks.STONE.defaultBlockState());
                        }
                    }
                }
            }
            helper.assertTrue(expected.size() > 0 && expected.size() < 125, shape[0] + ": the shape predicate selects "
                    + expected.size() + " of 125 blocks - nothing to tell apart from the frame");
            BlueprintScanner.Outcome outcome = BlueprintScanner.scanAtTable(level, table, octant, new ItemStack(ModItems.BLUEPRINT));
            helper.assertTrue(outcome.error() == null, shape[0] + ": scan refused: " + outcome.error());
            BlueprintModel scanned = BlueprintCode.parse(BlueprintItem.content(outcome.result()).code()).model();
            helper.assertTrue(scanned.equals(expected.normalized()), shape[0] + ": scanned " + scanned.size()
                    + " blocks, the octant's figure has " + expected.size());
            if (shape[0].equals("SPHERE")) {
                helper.assertTrue(scanned.get(0, 0, 0) == null && scanned.get(2, 2, 2) != null,
                        "sphere: a corner was scanned or the centre is missing");
            }
        }
        clear(helper);
        TestCleanup.succeed(helper);
    }

    /**
     * Grosse Scans laufen ueber mehrere Ticks: mit einem Budget von 20 Stellen je Tick liegt nach dem
     * Einlegen noch kein Ergebnis im Tisch, der Scan laeuft; die folgenden Ticks (das offene Menue ruft
     * jeden Tick {@code broadcastChanges}) bringen die fertige Blaupause mit genau der Auswahl.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Scan, der das Tick-Budget ignoriert (alles in
     * einem Zug), ein Auftrag, der nie weiterlaeuft oder nie fertig wird, ein Ergebnis, das beim
     * Stueckeln Stellen verliert.
     */
    public static void largeScanRunsOverSeveralTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, false);
        clear(helper);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 5), Blocks.GLASS);
        BlueprintModel expected = new BlueprintModel();
        expected.set(0, 0, 0, Blocks.STONE.defaultBlockState());
        expected.set(4, 2, 4, Blocks.GLASS.defaultBlockState());
        expected.set(2, 0, 2, Blocks.OBSIDIAN.defaultBlockState()); // der geklickte Block aus clear()
        player.addTag(BlueprintScanner.SMALL_BUDGET_TAG);
        CartographyTableMenu menu = new CartographyTableMenu(11, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(7, 1, 7))));
        menu.getSlot(0).set(octant(helper, new BlockPos(1, 1, 1), new BlockPos(5, 3, 5)));
        menu.getSlot(1).set(new ItemStack(ModItems.BLUEPRINT));
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "75 positions at 20 per tick finished in the first tick");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    menu.broadcastChanges();
                    helper.assertTrue(!menu.getSlot(2).getItem().isEmpty(), "the scan has not finished yet");
                })
                .thenExecute(() -> {
                    BlueprintModel scanned = BlueprintCode.parse(BlueprintItem.content(menu.getSlot(2).getItem()).code()).model();
                    helper.assertTrue(scanned.equals(expected), "the scan in slices lost or moved blocks: " + scanned.describe());
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * Grosse Bauten laufen ueber mehrere Ticks: mit 3 Bloecken je Tick setzt der Klick die ersten drei,
     * die Ticks des Baustabs in der Haupthand den Rest; legt der Spieler die Blaupause weg, bricht
     * der Auftrag ab und setzt nichts mehr.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Bau, der das Tick-Budget ignoriert, ein Auftrag,
     * der nie weiterlaeuft, einer, der ohne Blaupause in der Nebenhand weiterbaut.
     */
    public static void largeBuildRunsOverSeveralTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, true);
        clear(helper);
        player.addTag(BlueprintScanner.SMALL_BUDGET_TAG);
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack blueprint = blueprint("stone 0,0,0..0,0,4");
        hold(player, wand, blueprint);
        BlueprintBuilder.Result first = click(helper, player, wand, blueprint);
        helper.assertTrue(first != null && first.placed() == 3 && !first.finished() && BlueprintBuilder.building(player),
                "the first slice is not three blocks with a job left over: " + first);
        helper.startSequence()
                .thenExecuteAfter(1, () -> wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND))
                .thenExecuteAfter(1, () -> wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND))
                .thenExecute(() -> {
                    for (int z = 0; z <= 4; z++) {
                        helper.assertTrue(helper.getBlockState(TARGET.south(z)).is(Blocks.STONE), "block " + z + " was not built by the job");
                    }
                    helper.assertTrue(!BlueprintBuilder.building(player), "the finished job is still registered");
                    clear(helper);
                    BlueprintBuilder.Result again = click(helper, player, wand, blueprint);
                    helper.assertTrue(again != null && again.placed() == 3, "the second build did not start with three blocks");
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    helper.assertTrue(player.getOffhandItem().isEmpty(), "the off hand could not be emptied");
                })
                .thenExecuteAfter(1, () -> wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND))
                .thenExecute(() -> {
                    StringBuilder built = new StringBuilder();
                    for (int z = 0; z <= 4; z++) {
                        built.append(helper.getBlockState(TARGET.south(z)).is(Blocks.STONE) ? '#' : '.');
                    }
                    // Von der Mitte nach aussen: die ersten drei waren z = 1..3, uebrig sind z = 0 und 4.
                    helper.assertTrue(helper.getBlockState(TARGET.south(0)).isAir() && helper.getBlockState(TARGET.south(4)).isAir(),
                            "the job went on without the blueprint in the off hand: " + built + " offhand=" + player.getOffhandItem());
                    helper.assertTrue(!BlueprintBuilder.building(player), "the cancelled job is still registered");
                    clear(helper);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // EDITOR-PAKET UND REZEPT
    // =====================================================================================

    /**
     * Der Server prueft das Editor-Paket wie beim Buch: Code wird gespeichert, Signieren braucht
     * fehlerfreien Code und einen Titel, danach ist die Blaupause schreibgeschuetzt; ein Stapel
     * leerer Blaupausen wird nicht als Ganzes beschrieben.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Handler, der signierte Blaupausen
     * ueberschreibt, fehlerhaften Code signiert, den Autor nicht setzt oder einen ganzen Stapel
     * auf einmal beschreibt.
     */
    public static void editPacketSavesSignsAndLocksTheBlueprint(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT);
        player.getInventory().setItem(0, stack);

        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "stone 0,0,0", false, ""), player);
        helper.assertTrue(BlueprintItem.content(stack).code().equals("stone 0,0,0") && !BlueprintItem.content(stack).signed(),
                "the draft was not saved: " + BlueprintItem.content(stack));
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "stne 0,0,0", true, "Hut"), player);
        helper.assertTrue(!BlueprintItem.content(stack).signed() && BlueprintItem.content(stack).code().equals("stone 0,0,0"),
                "code with errors was signed");
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "stone 0..1,0,0", true, "  "), player);
        helper.assertTrue(!BlueprintItem.content(stack).signed(), "a blank title was accepted");
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "stone 0..1,0,0", true, "Hut"), player);
        BlueprintContent signed = BlueprintItem.content(stack);
        helper.assertTrue(signed.signed() && signed.title().equals("Hut") && signed.author().equals(player.getName().getString())
                && signed.code().equals("stone 0..1,0,0"), "signing failed: " + signed);
        helper.assertTrue(stack.getHoverName().getString().equals("Hut"), "a signed blueprint is not named after its title");
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "dirt 0,0,0", false, ""), player);
        helper.assertTrue(BlueprintItem.content(stack).equals(signed), "a signed blueprint was edited");

        ItemStack pile = new ItemStack(ModItems.BLUEPRINT, 3);
        player.getInventory().setItem(1, pile);
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(1, "stone 0,0,0", false, ""), player);
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 1
                        && BlueprintItem.content(player.getInventory().getItem(1)).code().equals("stone 0,0,0"),
                "the whole pile was written or the written one left the slot: " + player.getInventory().getItem(1));
        TestCleanup.succeed(helper);
    }

    /**
     * Rezept: formlos 1 Enderquarz + 1 Papier + 1 Tintenbeutel ergeben eine leere Blaupause; ohne
     * Tinte nichts.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein fehlendes oder falsch belegtes Rezept.
     */
    public static void recipeCraftsOneBlankBlueprint(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingInput input = CraftingInput.of(2, 2, List.of(
                new ItemStack(Items.INK_SAC), new ItemStack(ModItems.ENDER_QUARTZ),
                ItemStack.EMPTY, new ItemStack(Items.PAPER)));
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), "ender quartz + paper + ink sac crafts nothing");
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(result.is(ModItems.BLUEPRINT) && result.getCount() == 1 && BlueprintItem.content(result).isBlank(),
                "the recipe does not make one blank blueprint: " + result);
        CraftingInput noInk = CraftingInput.of(2, 2, List.of(
                ItemStack.EMPTY, new ItemStack(ModItems.ENDER_QUARTZ), ItemStack.EMPTY, new ItemStack(Items.PAPER)));
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, noInk, level).isEmpty(),
                "a blueprint can be crafted without ink");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // RUNDE 3: BEISPIELE, SUCHE, KOPIEREN, SIGNIERT BAUEN, AUTOSPEICHERN
    // =====================================================================================

    /**
     * Jede Beispielvorlage (Knopf "Beispiel einfuegen") parst fehlerfrei, passt in 16 x 16 x 16 und
     * hat eine Materialliste; jede Biom-Gruppe beginnt mit ihrem Dorfhaus, die Biome gehen auf die
     * richtige Gruppe, und die Materialliste des Ebenen-Hauses stimmt Stueck fuer Stueck.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Tippfehler in einer Vorlage, eine Vorlage ueber
     * 16, eine fehlende Ressource, eine falsche Biom-Zuordnung, ein Zaehlfehler in der Materialliste.
     */
    public static void examplesParseFitAndListTheirMaterials(GameTestHelper helper) {
        int total = 0;
        for (java.util.Map.Entry<String, List<String>> group : BlueprintExamples.TEMPLATES.entrySet()) {
            helper.assertTrue(group.getValue().get(0).equals(group.getKey() + "_house"), group.getKey() + " does not start with its village house");
            helper.assertTrue(group.getValue().size() >= 2, group.getKey() + " has only one template");
            for (String name : group.getValue()) {
                String code = BlueprintExamples.code(name);
                helper.assertTrue(!code.isBlank(), name + ": resource missing");
                helper.assertTrue(code.contains("#"), name + ": the example carries no comments");
                BlueprintCode.ParseResult parsed = BlueprintCode.parse(code);
                helper.assertTrue(parsed.ok(), name + ": " + parsed.problems());
                helper.assertTrue(!parsed.model().isEmpty() && parsed.model().maxEdge() <= 16,
                        name + ": " + parsed.model().sizeX() + "x" + parsed.model().sizeY() + "x" + parsed.model().sizeZ());
                helper.assertTrue(!BlueprintMaterials.list(parsed.model()).isEmpty(), name + ": empty material list");
                total++;
            }
        }
        helper.assertTrue(total >= 14, "only " + total + " templates");
        String[][] biomes = {{"plains", "plains"}, {"cherry_grove", "cherry"}, {"mangrove_swamp", "swamp"}, {"swamp", "swamp"},
                {"snowy_taiga", "snow"}, {"badlands", "desert"}, {"savanna_plateau", "savanna"}, {"old_growth_pine_taiga", "taiga"},
                {"jungle", "swamp"}, {"the_void", "plains"}, {"nether_wastes", "plains"}};
        for (String[] b : biomes) {
            helper.assertTrue(BlueprintExamples.groupFor(b[0]).equals(b[1]), b[0] + " -> " + BlueprintExamples.groupFor(b[0]));
        }
        java.util.Set<String> picked = new java.util.HashSet<>();
        for (int i = 0; i < 3; i++) {
            int index = i;
            picked.add(BlueprintExamples.pick("desert", n -> index));
        }
        helper.assertTrue(picked.equals(new java.util.HashSet<>(BlueprintExamples.TEMPLATES.get("desert"))), "pick does not reach every desert template: " + picked);

        java.util.Map<Item, Integer> counts = new java.util.HashMap<>();
        for (BlueprintMaterials.Entry e : BlueprintMaterials.list(BlueprintCode.parse(BlueprintExamples.code("plains_house")).model())) {
            counts.put(e.item(), e.count());
        }
        java.util.Map<Item, Integer> expected = java.util.Map.of(Items.OAK_PLANKS, 110, Items.COBBLESTONE, 49, Items.OAK_STAIRS, 42,
                Items.OAK_LOG, 12, Items.OAK_SLAB, 7, Items.GLASS_PANE, 4, Items.OAK_DOOR, 1, Items.TORCH, 1, Items.CRAFTING_TABLE, 1);
        helper.assertTrue(counts.equals(expected), "plains house materials " + counts);
        TestCleanup.succeed(helper);
    }

    /**
     * Die Blocksuche des Editors findet ueber den angezeigten Namen (hier der Server-Name und ein
     * nachgestellter "deutscher") und ueber die Id, der genaue Treffer steht vorn, und eingefuegt
     * wird die Id ohne {@code minecraft:}.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine Suche nur ueber die Id oder nur ueber den
     * Namen, eine Rangfolge, die den genauen Treffer nach hinten schiebt.
     */
    public static void blockSearchFindsByNameAndId(GameTestHelper helper) {
        java.util.function.Function<net.minecraft.world.level.block.Block, String> english = b -> b.getName().getString();
        List<net.minecraft.world.level.block.Block> byName = com.simplebuilding.blueprint.BlueprintBlockSearch.search("Glass Pane", english, 10);
        helper.assertTrue(!byName.isEmpty() && byName.get(0) == Blocks.GLASS_PANE, "'Glass Pane' found " + byName);
        List<net.minecraft.world.level.block.Block> byId = com.simplebuilding.blueprint.BlueprintBlockSearch.search("oak_stai", english, 10);
        helper.assertTrue(!byId.isEmpty() && byId.get(0) == Blocks.OAK_STAIRS, "'oak_stai' found " + byId);
        List<net.minecraft.world.level.block.Block> full = com.simplebuilding.blueprint.BlueprintBlockSearch.search("minecraft:stone", english, 5);
        helper.assertTrue(!full.isEmpty() && full.get(0) == Blocks.STONE, "'minecraft:stone' found " + full);
        java.util.function.Function<net.minecraft.world.level.block.Block, String> german = b -> b == Blocks.COBBLESTONE ? "Bruchstein" : b.getName().getString();
        List<net.minecraft.world.level.block.Block> localized = com.simplebuilding.blueprint.BlueprintBlockSearch.search("bruchst", german, 5);
        helper.assertTrue(localized.contains(Blocks.COBBLESTONE), "the display name in another language was not searched: " + localized);
        helper.assertTrue(com.simplebuilding.blueprint.BlueprintBlockSearch.search("zzqq", english, 5).isEmpty(), "nonsense found something");
        helper.assertTrue(com.simplebuilding.blueprint.BlueprintBlockSearch.codeName(Blocks.OAK_STAIRS).equals("oak_stairs"), "code name keeps minecraft:");
        TestCleanup.succeed(helper);
    }

    /**
     * Kopieren am Kartentisch: signierte Blaupause oben, leere unten ergibt eine unsignierte Kopie
     * mit demselben Code und Titel, ohne Autor; nehmen verbraucht nur die leere, das Original bleibt
     * signiert liegen. Eine unsignierte oben oder eine beschriebene unten ergibt nichts.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine Kopie, die signiert bleibt oder den Autor
     * behaelt, ein Ergebnis-Slot, der das Original verbraucht, ein Kopierpfad, der auch
     * unsignierte oder beschriebene Blaupausen annimmt.
     */
    public static void cartographyTableCopiesSignedBlueprints(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, false);
        ItemStack original = new ItemStack(ModItems.BLUEPRINT);
        original.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent("stone 0..2,0,0", "Turm", "Alice", true));
        CartographyTableMenu menu = new CartographyTableMenu(21, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(5, 1, 5))));
        helper.assertTrue(menu.getSlot(0).mayPlace(original), "the table refuses a signed blueprint on top");
        menu.getSlot(0).set(original);
        menu.getSlot(1).set(new ItemStack(ModItems.BLUEPRINT, 3));
        ItemStack copy = menu.getSlot(2).getItem();
        BlueprintContent c = BlueprintItem.content(copy);
        helper.assertTrue(copy.is(ModItems.BLUEPRINT) && copy.getCount() == 1 && !c.signed() && c.author().isEmpty()
                        && c.title().equals("Turm") && c.code().equals("stone 0..2,0,0"),
                "not an unsigned copy with code and title: " + c);
        menu.getSlot(2).onTake(player, copy);
        helper.assertTrue(menu.getSlot(1).getItem().getCount() == 2, "the copy did not use exactly one blank blueprint");
        helper.assertTrue(BlueprintItem.content(menu.getSlot(0).getItem()).signed(), "the original was used up or changed");

        ItemStack unsigned = new ItemStack(ModItems.BLUEPRINT);
        unsigned.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent("stone 0,0,0", "", "", false));
        helper.assertTrue(!menu.getSlot(0).mayPlace(unsigned), "an unsigned blueprint is accepted on top");
        CartographyTableMenu written = new CartographyTableMenu(22, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(5, 1, 5))));
        written.getSlot(0).set(original.copy());
        written.getSlot(1).set(unsigned.copy());
        helper.assertTrue(written.getSlot(2).getItem().isEmpty(), "a written blueprint below took a copy");
        TestCleanup.succeed(helper);
    }

    /**
     * Gebaut wird nur mit einer signierten Blaupause: eine unsignierte in der Nebenhand setzt nichts
     * und verbraucht nichts; dieselbe signiert baut.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine fehlende Signatur-Pruefung beim Bauen.
     */
    public static void buildNeedsTheSignedBlueprint(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);
        clear(helper);
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack stone = new ItemStack(Items.STONE, 8);
        ItemStack draft = new ItemStack(ModItems.BLUEPRINT);
        draft.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent("stone 0,0,0..0,0,2", "", "", false));
        hold(player, wand, draft, stone);
        helper.assertTrue(build(helper, player, wand, draft) == null, "an unsigned blueprint was built");
        helper.assertTrue(helper.getBlockState(TARGET).isAir() && stone.getCount() == 8, "the refused build placed or used something");
        ItemStack signed = blueprint("stone 0,0,0..0,0,2");
        hold(player, wand, signed, stone);
        BlueprintBuilder.Result result = build(helper, player, wand, signed);
        helper.assertTrue(result != null && result.placed() == 3, "the signed blueprint did not build: " + result);
        clear(helper);
        TestCleanup.succeed(helper);
    }

    /**
     * Autospeichern, serverseitig: jedes Edit-Paket steht sofort am Item - auch wenn der Spieler
     * danach die Verbindung verliert; weitere Pakete ueberschreiben der Reihe nach, und ein Stapel
     * leerer Blaupausen behaelt die beschriebene im Slot, damit die naechste Speicherung sie trifft.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Handler, der erst spaeter speichert, ein
     * Stapel-Split, der die naechste Autospeicherung eine weitere Blaupause abspalten laesst.
     */
    public static void editPacketsSaveImmediatelyAndSurviveTheDisconnect(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        ItemStack pile = new ItemStack(ModItems.BLUEPRINT, 3);
        player.getInventory().setItem(0, pile);
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "stone 0,0,0", false, ""), player);
        ItemStack inSlot = player.getInventory().getItem(0);
        helper.assertTrue(inSlot.getCount() == 1 && BlueprintItem.content(inSlot).code().equals("stone 0,0,0"),
                "the written blueprint did not stay in the slot: " + inSlot);
        int blanks = 0;
        for (int i = 1; i < player.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.BLUEPRINT) && BlueprintItem.content(s).isBlank()) {
                blanks += s.getCount();
            }
        }
        helper.assertTrue(blanks == 2, "the rest of the pile is not in the inventory: " + blanks);
        ModMessageHandlers.handleBlueprintEdit(new BlueprintEditPayload(0, "stone 0..1,0,0", false, ""), player);
        helper.assertTrue(BlueprintItem.content(player.getInventory().getItem(0)).code().equals("stone 0..1,0,0")
                && player.getInventory().getItem(0).getCount() == 1, "the second autosave did not overwrite the same blueprint");
        ItemStack kept = player.getInventory().getItem(0);
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.assertTrue(BlueprintItem.content(kept).code().equals("stone 0..1,0,0"), "the saved code is gone after the disconnect");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // RUNDE 3 G: SICHTBAR WACHSENDER BAU
    // =====================================================================================

    /**
     * Der Bau waechst sichtbar: Schicht fuer Schicht von unten nach oben, in jeder Schicht von der
     * Mitte nach aussen. Mit 3 Stellen je Tick (Test-Tag) steht nach dem Klick die Mitte der unteren
     * Schicht und zwei Kantenmitten, keine Ecke; nach drei Scheiben die ganze untere Schicht und noch
     * nichts oben; am Ende alles. Die Bauzeit waechst gedaempft: 1 s fuer kleine Bauten, hoechstens 9 s.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine Reihenfolge ohne Mitte-nach-aussen oder ohne
     * Schichten, ein Bau in einem Zug, eine Bauzeit, die nicht gedaempft oder nicht begrenzt ist.
     */
    public static void buildGrowsLayerByLayerFromTheCentre(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, true);
        player.addTag(BlueprintScanner.SMALL_BUDGET_TAG);
        clear(helper);
        helper.assertTrue(BlueprintBuilder.ticksFor(20) <= 25 && BlueprintBuilder.ticksFor(BlueprintCode.MAX_EXPANDED_CELLS) == 180,
                "build time is not 1 s for small and 9 s for the largest builds: " + BlueprintBuilder.ticksFor(20) + " / "
                        + BlueprintBuilder.ticksFor(BlueprintCode.MAX_EXPANDED_CELLS));
        helper.assertTrue(BlueprintBuilder.ticksFor(1000) > 20 && BlueprintBuilder.ticksFor(1000) < BlueprintBuilder.ticksFor(100_000),
                "build time does not grow with the size");
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack blueprint = blueprint("stone 0..2,0..1,0..2");
        hold(player, wand, blueprint);
        BlueprintBuilder.Result first = click(helper, player, wand, blueprint);
        helper.assertTrue(first != null && first.placed() == 3 && !first.finished(), "the first slice is not three blocks: " + first);
        // Welt: x -1..1 um TARGET, z 0..2 vor TARGET; Mitte der Schicht = TARGET.south(1)
        helper.assertTrue(helper.getBlockState(TARGET.south(1)).is(Blocks.STONE), "the centre of the bottom layer did not come first");
        for (BlockPos corner : new BlockPos[]{TARGET.west(), TARGET.east(), TARGET.south(2).west(), TARGET.south(2).east()}) {
            helper.assertTrue(helper.getBlockState(corner).isAir(), "a corner came before the edge centres: " + corner);
        }
        helper.startSequence()
                .thenExecuteAfter(1, () -> wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND))
                .thenExecuteAfter(1, () -> {
                    wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND);
                    int bottom = 0, top = 0;
                    for (int x = -1; x <= 1; x++) {
                        for (int z = 0; z <= 2; z++) {
                            bottom += helper.getBlockState(TARGET.offset(x, 0, z)).is(Blocks.STONE) ? 1 : 0;
                            top += helper.getBlockState(TARGET.offset(x, 1, z)).is(Blocks.STONE) ? 1 : 0;
                        }
                    }
                    helper.assertTrue(bottom == 9 && top == 0, "after three slices: bottom " + bottom + ", top " + top);
                })
                .thenExecuteAfter(1, () -> wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND))
                .thenExecuteAfter(1, () -> wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND))
                .thenExecuteAfter(1, () -> {
                    wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND);
                    helper.assertTrue(helper.getBlockState(TARGET.offset(1, 1, 2)).is(Blocks.STONE) && !BlueprintBuilder.building(player),
                            "the build did not finish");
                    clear(helper);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * Jeder gesetzte Block kostet den Baustab einen Haltbarkeitspunkt; bricht er, stoppt der Bau
     * sofort - kein Block ueber die Haltbarkeit hinaus.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Bau, der mit zerbrochenem Stab weitersetzt.
     */
    public static void buildStopsWhenTheWandBreaks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);
        player.addTag(BlueprintScanner.SMALL_BUDGET_TAG);
        clear(helper);
        ItemStack wand = new ItemStack(ModItems.COPPER_BUILDING_WAND);
        wand.setDamageValue(wand.getMaxDamage() - 2);
        ItemStack stone = new ItemStack(Items.STONE, 20);
        ItemStack blueprint = blueprint("stone 0,0,0..0,0,6");
        hold(player, wand, blueprint, stone);
        BlueprintBuilder.Result result = click(helper, player, wand, blueprint);
        int placed = 0;
        for (int z = 0; z <= 6; z++) {
            placed += helper.getBlockState(TARGET.south(z)).is(Blocks.STONE) ? 1 : 0;
        }
        helper.assertTrue(wand.isEmpty(), "the wand did not break");
        helper.assertTrue(placed == 2 && result.placed() == 2 && result.wandBroke(), "placed " + placed + " blocks with 2 durability left: " + result);
        helper.assertTrue(!BlueprintBuilder.building(player), "a job is still registered for the broken wand");
        clear(helper);
        TestCleanup.succeed(helper);
    }

    /**
     * Wechselt der Spieler waehrend des Baus das Werkzeug in der Haupthand, bricht der Bau ab (wie
     * beim normalen Baustab) und nimmt ihn auch nicht wieder auf, wenn der Stab zurueckkommt.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Auftrag, der nur tickt, solange der Stab in
     * der Hand ist, und deshalb liegen bleibt und spaeter weiterbaut.
     */
    public static void buildStopsWhenTheWandLeavesTheMainHand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, true);
        player.addTag(BlueprintScanner.SMALL_BUDGET_TAG);
        clear(helper);
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack blueprint = blueprint("stone 0,0,0..0,0,6");
        hold(player, wand, blueprint);
        BlueprintBuilder.Result first = click(helper, player, wand, blueprint);
        helper.assertTrue(first != null && first.placed() == 3 && BlueprintBuilder.building(player), "no job after the first slice: " + first);
        player.getInventory().setSelectedSlot(1);
        wand.getItem().inventoryTick(wand, level, player, null);
        helper.assertTrue(!BlueprintBuilder.building(player), "switching the main hand did not stop the build");
        player.getInventory().setSelectedSlot(0);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    wand.getItem().inventoryTick(wand, level, player, EquipmentSlot.MAINHAND);
                    int placed = 0;
                    for (int z = 0; z <= 6; z++) {
                        placed += helper.getBlockState(TARGET.south(z)).is(Blocks.STONE) ? 1 : 0;
                    }
                    helper.assertTrue(placed == 3, "the cancelled build went on after the wand came back: " + placed);
                    clear(helper);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * Zwei-Klick-Regel: fehlt Material, baut der erste Klick nichts (er warnt nur, die roten Stellen
     * leuchten auf); ein zweiter Klick kurz danach baut alles Vorhandene. Fehlt nichts, baut schon
     * der erste Klick.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Bau ohne Warnung trotz Luecken, eine Warnung,
     * die jeden Klick blockiert, eine Warnung, obwohl nichts fehlt.
     */
    public static void missingBlocksNeedTheSecondClick(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);
        clear(helper);
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        ItemStack stone = new ItemStack(Items.STONE, 2);
        ItemStack blueprint = blueprint("stone 0,0,0..0,0,3");
        hold(player, wand, blueprint, stone);
        BlueprintBuilder.Result warned = build(helper, player, wand, blueprint);
        helper.assertTrue(warned != null && warned.warned() && warned.placed() == 0 && warned.missing() == 2 && stone.getCount() == 2,
                "the first click with missing blocks did not only warn: " + warned);
        BlueprintBuilder.Result second = build(helper, player, wand, blueprint);
        helper.assertTrue(second != null && !second.warned() && second.placed() == 2 && second.missing() == 2,
                "the confirming click did not build what is there: " + second);

        clear(helper);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 10));
        BlueprintBuilder.Result complete = build(helper, player, wand, blueprint);
        helper.assertTrue(complete != null && !complete.warned() && complete.placed() == 4, "nothing missing, but the first click did not build: " + complete);
        clear(helper);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HILFEN
    // =====================================================================================

    private static ServerPlayer mockPlayer(GameTestHelper helper, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 0.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = instabuild;
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Leert den Bau-Ort (x/z 0..7, y 1..6) und legt den geklickten Obsidian. */
    private static void clear(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int y = 1; y < 7; y++) {
                for (int z = 0; z < 8; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.setBlock(CLICKED, Blocks.OBSIDIAN);
    }

    private static ItemStack blueprint(String code) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT);
        stack.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent(code, "Test", "Tester", true));
        return stack;
    }

    private static ItemStack octant(GameTestHelper helper, BlockPos a, BlockPos b) {
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        BlockPos pa = helper.absolutePos(a);
        BlockPos pb = helper.absolutePos(b);
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{pa.getX(), pa.getY(), pa.getZ()});
        nbt.putIntArray("Pos2", new int[]{pb.getX(), pb.getY(), pb.getZ()});
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return octant;
    }

    /** Baustab ausgewaehlt in Slot 0, Blaupause in der Nebenhand, Material ab Slot 1. */
    private static void hold(ServerPlayer player, ItemStack wand, ItemStack blueprint, ItemStack... supplies) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.setItemInHand(InteractionHand.OFF_HAND, blueprint);
        for (int i = 0; i < supplies.length; i++) {
            player.getInventory().setItem(i + 1, supplies[i]);
        }
    }

    /** Ein Klick und, falls ein Auftrag bleibt, dessen sofortiger Abschluss: das Ergebnis des ganzen Baus. */
    private static BlueprintBuilder.Result build(GameTestHelper helper, ServerPlayer player, ItemStack wand, ItemStack blueprint) {
        BlueprintBuilder.Result result = click(helper, player, wand, blueprint);
        if (result != null && !result.warned() && !result.finished()) {
            BlueprintBuilder.Result rest = BlueprintBuilder.completeJob(player);
            return rest != null ? rest : result;
        }
        return result;
    }

    /** Nur der Klick: die erste Scheibe, der Rest bleibt als Auftrag stehen. */
    private static BlueprintBuilder.Result click(GameTestHelper helper, ServerPlayer player, ItemStack wand, ItemStack blueprint) {
        return BlueprintBuilder.build(helper.getLevel(), player, wand, blueprint, helper.absolutePos(CLICKED), Direction.UP);
    }

    private static Direction stairFacing(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.is(Blocks.OAK_STAIRS) ? state.getValue(StairBlock.FACING) : null;
    }
}
