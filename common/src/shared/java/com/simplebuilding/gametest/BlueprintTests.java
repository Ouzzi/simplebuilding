package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintBuilder;
import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintMaterials;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.blueprint.BlueprintTiers;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.networking.BlueprintEditPayload;
import com.simplebuilding.networking.BlueprintRotatePayload;
import com.simplebuilding.networking.ModMessageHandlers;
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
        BlueprintCode.ParseResult bad = BlueprintCode.parse("stne 0,0,0\nstone 0,0,128\noak_stairs[facing=up] 0,0,0\nstone");
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
        helper.succeed();
    }

    /**
     * Grenzen: der Code hat eine Hoechstlaenge (sonst passt er nicht mehr in das Item), die
     * Ausroll-Obergrenze faengt Riesen-Wiederholungen ab, und die Baustab-Stufen schaffen je einen
     * Wuerfel: 16, 32, 48, 64, 96, 128.
     *
     * <p><strong>Was diesen Test bricht:</strong> eine fehlende Laengenpruefung, eine
     * Wiederholung, die erst ausgerollt und dann gezaehlt wird, eine vertauschte Stufenzuordnung.
     */
    public static void sizeLimitsCapTheCodeAndMapWandTiers(GameTestHelper helper) {
        String tooLong = "stone 0,0,0\n" + "#".repeat(BlueprintCode.MAX_CODE_LENGTH);
        List<String> keys = BlueprintCode.parse(tooLong).problems().stream().map(BlueprintCode.Problem::key).toList();
        helper.assertTrue(keys.contains("too_long"), "an over-long code was accepted: " + keys);

        long start = System.nanoTime();
        BlueprintCode.ParseResult huge = BlueprintCode.parse("stone 0..127,0..127,0..127*3@0,0,0");
        helper.assertTrue(huge.problems().stream().anyMatch(p -> p.key().equals("too_many_cells")),
                "three full 128-cubes were not refused: " + huge.problems());
        helper.assertTrue(huge.model().isEmpty(), "the refused repetition was applied anyway");
        helper.assertTrue(System.nanoTime() - start < 2_000_000_000L, "refusing the giant took too long - it was expanded first");

        int[][] expected = {
                {edge(ModItems.COPPER_BUILDING_WAND), 16}, {edge(ModItems.IRON_BUILDING_WAND), 32},
                {edge(ModItems.GOLD_BUILDING_WAND), 48}, {edge(ModItems.DIAMOND_BUILDING_WAND), 64},
                {edge(ModItems.NETHERITE_BUILDING_WAND), 96}, {edge(ModItems.ENDERITE_BUILDING_WAND), 128}};
        for (int[] pair : expected) {
            helper.assertTrue(pair[0] == pair[1], "wand edge " + pair[0] + " instead of " + pair[1]);
        }
        helper.assertTrue(BlueprintTiers.tierIndexFor(16) == 0 && BlueprintTiers.tierIndexFor(17) == 1
                        && BlueprintTiers.tierIndexFor(128) == 5 && BlueprintTiers.tierIndexFor(129) == -1,
                "tier lookup is off");
        BlueprintModel line = BlueprintCode.parse("stone 0..16,0,0").model();
        helper.assertTrue(line.maxEdge() == 17 && line.sizeX() == 17 && line.sizeY() == 1, "bounding box of a 17 line is wrong");
        helper.succeed();
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
        helper.succeed();
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
        helper.succeed();
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

        BlueprintBuilder.Result first = build(helper, player, wand, blueprint);
        helper.assertTrue(first != null, "the build was refused");
        helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE), "the free stone position was not built");
        helper.assertTrue(helper.getBlockState(TARGET.east()).is(Blocks.DIRT), "an occupied position was overwritten");
        helper.assertTrue(helper.getBlockState(TARGET.above()).isAir(), "glass appeared without glass in the inventory");
        helper.assertTrue(first.placed().size() == 1 && first.already() == 1 && first.occupied() == 1 && first.missing() == 1,
                "unexpected tally " + first);
        helper.assertTrue(stone.getCount() == 4, "stone count " + stone.getCount() + " instead of 4");
        helper.assertTrue(wand.getDamageValue() == 1, "wand damage " + wand.getDamageValue() + " instead of 1");

        player.getInventory().setItem(2, new ItemStack(Items.GLASS, 3));
        BlueprintBuilder.Result second = build(helper, player, wand, blueprint);
        helper.assertTrue(second != null && second.placed().size() == 1, "coming back did not fill the gap: " + second);
        helper.assertTrue(helper.getBlockState(TARGET.above()).is(Blocks.GLASS), "the missing glass was not placed on the second click");
        helper.assertTrue(stone.getCount() == 4, "the second click spent stone on positions that were already right");
        helper.assertTrue(player.getInventory().getItem(2).getCount() == 2, "glass not consumed");
        helper.assertTrue(wand.getDamageValue() == 2, "wand damage " + wand.getDamageValue() + " instead of 2");
        clear(helper);
        helper.succeed();
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

        player.setYRot(-90f);
        helper.assertTrue(player.getDirection() == Direction.EAST, "mock player does not face east");
        clear(helper);
        build(helper, player, wand, blueprint);
        for (int d = 0; d <= 2; d++) {
            helper.assertTrue(helper.getBlockState(TARGET.east(d)).is(Blocks.STONE), "facing east: no stone at +x " + d);
        }
        helper.assertTrue(stairFacing(helper, TARGET.east(2).above()) == Direction.EAST, "facing east: stair not facing east");
        clear(helper);
        helper.succeed();
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
        helper.assertTrue(result != null && result.placed().size() == 2, "the iron wand did not build the 17 block blueprint: " + result);
        helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE) && helper.getBlockState(TARGET.above(16)).is(Blocks.STONE),
                "the iron wand's build is not where it should be");
        helper.setBlock(TARGET.above(16), Blocks.AIR);
        clear(helper);
        helper.succeed();
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
        helper.assertTrue(pile.getCount() == 2 && BlueprintItem.content(pile).isBlank(), "the whole pile was written: " + pile);
        boolean found = false;
        for (int i = 2; i < player.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            found |= s.is(ModItems.BLUEPRINT) && BlueprintItem.content(s).code().equals("stone 0,0,0");
        }
        helper.assertTrue(found, "the written blueprint split off the pile is not in the inventory");
        helper.succeed();
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
        ItemStack result = recipe.get().value().assemble(input);
        helper.assertTrue(result.is(ModItems.BLUEPRINT) && result.getCount() == 1 && BlueprintItem.content(result).isBlank(),
                "the recipe does not make one blank blueprint: " + result);
        CraftingInput noInk = CraftingInput.of(2, 2, List.of(
                ItemStack.EMPTY, new ItemStack(ModItems.ENDER_QUARTZ), ItemStack.EMPTY, new ItemStack(Items.PAPER)));
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, noInk, level).isEmpty(),
                "a blueprint can be crafted without ink");
        helper.succeed();
    }

    // =====================================================================================
    // HILFEN
    // =====================================================================================

    private static ServerPlayer mockPlayer(GameTestHelper helper, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 0.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = instabuild;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
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
        stack.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent(code, "", "", false));
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

    private static BlueprintBuilder.Result build(GameTestHelper helper, ServerPlayer player, ItemStack wand, ItemStack blueprint) {
        return BlueprintBuilder.build(helper.getLevel(), player, wand, blueprint, helper.absolutePos(CLICKED), Direction.UP);
    }

    private static Direction stairFacing(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.is(Blocks.OAK_STAIRS) ? state.getValue(StairBlock.FACING) : null;
    }
}
