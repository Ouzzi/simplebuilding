package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.ChunkLoaderBlock;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.spawn.LaunchSafety;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests der drei Stufen von Launchpad und Chunk-Loader und der Druckplatten-Aufwertungen
 * (Besitzer 2026-09-27, docs/SIMPLETWEAKS-UEBERNAHME.md Abschnitt 2). Die Chunk-Loader-Tests stehen
 * weit weg von der Teststruktur, damit keine Erzwingung der Spieltest-Umgebung ihre Chunks haelt.
 */
public final class TweaksTierTests {

    private TweaksTierTests() {
    }

    private static final Block[] LAUNCHPADS = {TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD};
    private static final int[] CAPACITIES = {4, 8, 16};

    // =====================================================================================
    // Launchpad
    // =====================================================================================

    /** Einzeln geladen fassen die Stufen I-III 4, 8 und 16 Windkugeln; mehr nimmt keine und verbraucht keine. */
    public static void launchpadTiersHoldFourEightAndSixteenWindCharges(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        for (int i = 0; i < 3; i++) {
            BlockPos pos = new BlockPos(3 + i * 2, 1, 3);
            helper.setBlock(pos, LAUNCHPADS[i]);
            ItemStack charges = new ItemStack(Items.WIND_CHARGE, 64);
            player.setItemInHand(InteractionHand.MAIN_HAND, charges);
            for (int n = 0; n < 40; n++) {
                helper.getBlockState(pos).useItemOn(charges, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, pos));
            }
            int stored = helper.getBlockEntity(pos, LaunchpadBlockEntity.class).getCharges();
            helper.assertTrue(stored == CAPACITIES[i], "launchpad tier " + (i + 1) + " holds " + stored + " charges instead of " + CAPACITIES[i]);
            helper.assertTrue(charges.getCount() == 64 - CAPACITIES[i], "loading tier " + (i + 1) + " one by one spent " + (64 - charges.getCount())
                    + " wind charges instead of " + CAPACITIES[i]);
            helper.assertTrue(((LaunchpadBlock) LAUNCHPADS[i]).getTier() == i + 1, LAUNCHPADS[i] + " is not tier " + (i + 1));
        }
        helper.assertTrue(((LaunchpadBlock) TweaksBlocks.ENDERITE_LAUNCHPAD).isEnderite() && !((LaunchpadBlock) TweaksBlocks.NETHERITE_LAUNCHPAD).isEnderite(),
                "only the enderite launchpad counts as enderite");
        TestCleanup.succeed(helper);
    }

    /**
     * Schleichen + Rechtsklick mit Windkugeln laedt alle Windkugeln der Hand auf einmal, hoechstens bis
     * zum Fassungsvermoegen; ohne Schleichen bleibt es bei einer. Geht den ganzen Vanilla-Weg
     * (ServerPlayerGameMode#useItemOn), der beim Schleichen den Block ueberspringt.
     */
    public static void sneakingWithWindChargesLoadsTheWholeHandUpToCapacity(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ServerLevel level = helper.getLevel();

        // Enderit (16): ein voller Stapel -> 16 geladen, 48 bleiben in der Hand.
        BlockPos big = new BlockPos(3, 1, 3);
        helper.setBlock(big, TweaksBlocks.ENDERITE_LAUNCHPAD);
        ItemStack stack = new ItemStack(Items.WIND_CHARGE, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit(helper, big));
        helper.assertTrue(result.consumesAction(), "a sneaking right-click with wind charges on a launchpad did nothing: " + result);
        int stored = helper.getBlockEntity(big, LaunchpadBlockEntity.class).getCharges();
        helper.assertTrue(stored == 16, "one sneaking click loaded " + stored + " charges into the enderite launchpad instead of 16");
        helper.assertTrue(player.getMainHandItem().getCount() == 48, "one sneaking click left " + player.getMainHandItem().getCount()
                + " wind charges in the hand instead of 48");

        // Voll: der naechste schleichende Klick nimmt nichts. (Ab 26.3 setzt ein erfolgreicher Klick die
        // Vanilla-Abklingzeit der Windkugel, die sonst den naechsten Klick schon vor dem Item abfaengt.)
        expireCooldowns(player);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, big));
        helper.assertTrue(player.getMainHandItem().getCount() == 48, "a sneaking click on a full launchpad still took wind charges");

        // Netherit (8) mit nur 5 in der Hand: alle 5, die Hand wird leer.
        BlockPos mid = new BlockPos(5, 1, 3);
        helper.setBlock(mid, TweaksBlocks.NETHERITE_LAUNCHPAD);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WIND_CHARGE, 5));
        expireCooldowns(player);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, mid));
        helper.assertTrue(helper.getBlockEntity(mid, LaunchpadBlockEntity.class).getCharges() == 5, "a sneaking click with 5 wind charges did not load all 5");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the hand still holds wind charges after loading all of them");

        // Stufe I ohne Schleichen: eine Windkugel je Klick.
        player.setShiftKeyDown(false);
        BlockPos small = new BlockPos(7, 1, 3);
        helper.setBlock(small, TweaksBlocks.LAUNCHPAD);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WIND_CHARGE, 64));
        expireCooldowns(player);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, small));
        helper.assertTrue(helper.getBlockEntity(small, LaunchpadBlockEntity.class).getCharges() == 1, "a plain right-click loaded more than one wind charge");
        helper.assertTrue(player.getMainHandItem().getCount() == 63, "a plain right-click spent more than one wind charge");
        TestCleanup.succeed(helper);
    }

    /**
     * Eine Ladung schiebt doppelt so stark wie in Simple Tweaks (1,5 + 0,4 je Ladung): jede volle Stufe
     * startet so hoch wie frueher die doppelte Ladung, 16 Ladungen wie die alten 32. Der echte Start
     * des vollen Enderit-Launchpads gibt genau diesen Schub und den Fallschutz, die Netherit-Stufe
     * keinen Fallschutz.
     */
    public static void launchStrengthPerChargeIsDoubledSoFullTiersLaunchLikeTwiceTheOldCharges(GameTestHelper helper) {
        for (int i = 0; i < 3; i++) {
            double now = LaunchpadBlockEntity.strengthFor(CAPACITIES[i]);
            double old = 1.5 + (2 * CAPACITIES[i]) * 0.4;
            helper.assertTrue(Math.abs(now - old) < 1e-9, CAPACITIES[i] + " charges launch with " + now + " instead of the old "
                    + (2 * CAPACITIES[i]) + " charges' " + old);
        }
        helper.assertTrue(Math.abs(LaunchpadBlockEntity.strengthFor(16) - 14.3) < 1e-9, "16 charges do not launch like the old 32 (14.3)");

        ServerPlayer enderite = launchFrom(helper, new BlockPos(3, 1, 3), TweaksBlocks.ENDERITE_LAUNCHPAD, 16);
        helper.assertTrue(Math.abs(enderite.getDeltaMovement().y - 14.3) < 1e-6, "the full enderite launchpad launched with "
                + enderite.getDeltaMovement().y + " instead of 14.3");
        helper.assertTrue(LaunchSafety.isProtected(enderite), "the enderite launchpad no longer protects against fall damage");

        ServerPlayer netherite = launchFrom(helper, new BlockPos(6, 1, 3), TweaksBlocks.NETHERITE_LAUNCHPAD, 8);
        helper.assertTrue(Math.abs(netherite.getDeltaMovement().y - LaunchpadBlockEntity.strengthFor(8)) < 1e-6, "the full netherite launchpad launched with "
                + netherite.getDeltaMovement().y + " instead of " + LaunchpadBlockEntity.strengthFor(8));
        helper.assertTrue(!LaunchSafety.isProtected(netherite), "the netherite launchpad protects against fall damage like the enderite one");
        TestCleanup.succeed(helper);
    }

    /**
     * Welt-Upgrade: Launchpads aus der Zeit vor den Stufen tragen bis zu 32 (Enderit) bzw. 16 Ladungen.
     * Beim ersten Tick behalten sie, was ihre Stufe jetzt fasst, und werfen den Rest als Windkugeln aus.
     */
    public static void oldLaunchpadsDropTheChargesTheirTierNoLongerHolds(GameTestHelper helper) {
        Block[] pads = {TweaksBlocks.ENDERITE_LAUNCHPAD, TweaksBlocks.LAUNCHPAD};
        int[] oldCharges = {32, 16};
        int[] kept = {16, 4};
        for (int i = 0; i < 2; i++) {
            BlockPos pos = new BlockPos(2 + i * 4, 1, 3);
            helper.setBlock(pos, pads[i]);
            LaunchpadBlockEntity be = helper.getBlockEntity(pos, LaunchpadBlockEntity.class);
            CompoundTag tag = new CompoundTag();
            tag.putInt("Charges", oldCharges[i]);
            be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            helper.assertTrue(be.getCharges() == oldCharges[i], "the old save did not load " + oldCharges[i] + " charges");
            LaunchpadBlockEntity.tick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), be);
            helper.assertTrue(be.getCharges() == kept[i], pads[i] + " kept " + be.getCharges() + " of its old charges instead of " + kept[i]);
            int dropped = 0;
            for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
                if (item.getItem().is(Items.WIND_CHARGE)) {
                    dropped += item.getItem().getCount();
                    item.discard();
                }
            }
            helper.assertTrue(dropped == oldCharges[i] - kept[i], pads[i] + " dropped " + dropped + " wind charges instead of " + (oldCharges[i] - kept[i]));
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Chunk-Loader
    // =====================================================================================

    /** Stufe I haelt nur den eigenen Chunk, II das Kreuz aus 5 Chunks, III die 3x3; beim Abbau kommt alles frei. */
    public static void chunkLoaderTiersForceOneFiveAndNineChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block[] loaders = {TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER};
        int[] sizes = {1, 5, 9};
        for (int i = 0; i < 3; i++) {
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1)).offset(12288 + i * 64, 0, 12288);
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            Set<Long> expected = new HashSet<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertFalse(isForced(level, cx + dx, cz + dz), "the far test chunk is already forced by someone else");
                    if (i == 2 || (i == 1 && Math.abs(dx) + Math.abs(dz) <= 1) || (dx == 0 && dz == 0)) {
                        expected.add(ChunkLoaderBlockEntity.key(cx + dx, cz + dz));
                    }
                }
            }
            try {
                level.setBlock(pos, loaders[i].defaultBlockState(), Block.UPDATE_ALL);
                ChunkLoaderBlockEntity be = (ChunkLoaderBlockEntity) level.getBlockEntity(pos);
                be.update(level);
                helper.assertTrue(expected.size() == sizes[i], "test setup: tier " + (i + 1) + " expects " + expected.size() + " chunks");
                helper.assertTrue(be.ownForced().equals(expected), "chunk loader tier " + (i + 1) + " forced " + be.ownForced().size()
                        + " chunks instead of its " + sizes[i]);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        boolean want = expected.contains(ChunkLoaderBlockEntity.key(cx + dx, cz + dz));
                        helper.assertTrue(isForced(level, cx + dx, cz + dz) == want, "chunk loader tier " + (i + 1) + (want ? " does not force" : " forces")
                                + " the chunk at offset " + dx + "," + dz);
                        helper.assertTrue(be.covers(cx + dx, cz + dz) == want, "chunk loader tier " + (i + 1) + (want ? " does not cover" : " covers")
                                + " the chunk at offset " + dx + "," + dz);
                    }
                }
                helper.assertTrue(ChunkLoaderBlockEntity.tierOf(loaders[i].defaultBlockState()) == i + 1, loaders[i] + " is not tier " + (i + 1));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                for (long key : expected) {
                    helper.assertFalse(isForced(level, (int) key, (int) (key >> 32)), "a broken chunk loader tier " + (i + 1) + " keeps a chunk forced");
                }
            } finally {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.assertTrue(ChunkLoaderBlock.inArea(2, 0, 1) && !ChunkLoaderBlock.inArea(2, 1, 1) && !ChunkLoaderBlock.inArea(1, 1, 0)
                && ChunkLoaderBlock.inArea(3, -1, 1), "chunk loader areas per tier");
        TestCleanup.succeed(helper);
    }

    /**
     * Uebergabe mit der neuen Kreuzform: ein Enderit-Loader (3x3) diagonal neben einem Netherit-Loader
     * (Kreuz) wird abgebaut. Die Chunks, die das Kreuz abdeckt, gehen an den Netherit-Loader ueber; die
     * Mitte des Enderit-Loaders liegt diagonal zum Kreuz und kommt frei.
     */
    public static void aNetheriteChunkLoaderTakesOverOnlyTheChunksOfItsCross(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cross = helper.absolutePos(new BlockPos(1, 1, 1)).offset(16384, 0, 16384);
        BlockPos square = cross.offset(16, 0, 16);
        int cx = cross.getX() >> 4;
        int cz = cross.getZ() >> 4;
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                helper.assertFalse(isForced(level, cx + dx, cz + dz), "the far test chunk is already forced by someone else");
            }
        }
        try {
            level.setBlock(square, TweaksBlocks.ENDERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ((ChunkLoaderBlockEntity) level.getBlockEntity(square)).update(level);
            level.setBlock(cross, TweaksBlocks.NETHERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity netherite = (ChunkLoaderBlockEntity) level.getBlockEntity(cross);
            netherite.update(level);
            helper.assertTrue(netherite.ownForced().equals(Set.of(ChunkLoaderBlockEntity.key(cx - 1, cz), ChunkLoaderBlockEntity.key(cx, cz - 1))),
                    "the netherite loader claims other chunks than the two of its cross the enderite loader left free: " + netherite.ownForced().size());

            level.setBlock(square, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (List<Integer> offset : List.of(List.of(0, 0), List.of(1, 0), List.of(0, 1))) {
                helper.assertTrue(isForced(level, cx + offset.get(0), cz + offset.get(1)), "a chunk of the cross came free at " + offset);
                helper.assertTrue(netherite.ownForced().contains(ChunkLoaderBlockEntity.key(cx + offset.get(0), cz + offset.get(1))),
                        "the netherite loader did not take over its cross chunk at " + offset);
            }
            helper.assertFalse(isForced(level, cx + 1, cz + 1), "the diagonal chunk stays forced although no loader covers it any more");
            helper.assertFalse(isForced(level, cx + 2, cz + 2), "a chunk only the broken enderite loader covered stays forced");
        } finally {
            level.setBlock(square, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(cross, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Rezepte
    // =====================================================================================

    /**
     * Die Aufwertungen aller Pad-Familien kosten die Druckplatte des Zielmaterials statt des Rohstoffs;
     * die alten Wege (Barren, Block) fuehren zu nichts mehr. Das Flypad I kostet eine Elytra
     * (vorlaeufig, Besitzer-Nachricht unvollstaendig).
     */
    public static void padUpgradesPayWithThePressurePlateOfTheirTargetMaterial(GameTestHelper helper) {
        Item any = Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
        Item net = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
        Item end = ModItems.ENDERITE_UPGRADE_TEMPLATE;
        Item diamond = TweaksBlocks.DIAMOND_PRESSURE_PLATE.asItem();
        Item netherite = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        Item enderite = TweaksBlocks.ENDERITE_PRESSURE_PLATE.asItem();

        expect(helper, any, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, diamond, TweaksBlocks.LAUNCHPAD);
        expect(helper, net, TweaksBlocks.LAUNCHPAD, netherite, TweaksBlocks.NETHERITE_LAUNCHPAD);
        expect(helper, end, TweaksBlocks.NETHERITE_LAUNCHPAD, enderite, TweaksBlocks.ENDERITE_LAUNCHPAD);

        expect(helper, any, TweaksBlocks.COPPER_PRESSURE_PLATE, diamond, TweaksBlocks.CHUNK_LOADER);
        expect(helper, net, TweaksBlocks.CHUNK_LOADER, netherite, TweaksBlocks.NETHERITE_CHUNK_LOADER);
        expect(helper, end, TweaksBlocks.NETHERITE_CHUNK_LOADER, enderite, TweaksBlocks.ENDERITE_CHUNK_LOADER);

        expect(helper, any, TweaksBlocks.ELYTRA_PAD, diamond, TweaksBlocks.REINFORCED_ELYTRA_PAD);
        expect(helper, net, TweaksBlocks.REINFORCED_ELYTRA_PAD, netherite, TweaksBlocks.NETHERITE_ELYTRA_PAD);
        expect(helper, end, TweaksBlocks.NETHERITE_ELYTRA_PAD, enderite, TweaksBlocks.ENDERITE_ELYTRA_PAD);

        expect(helper, net, TweaksBlocks.FINE_ELYTRA_PAD, Items.ELYTRA, TweaksBlocks.FLYPAD);
        expect(helper, net, TweaksBlocks.FLYPAD, netherite, TweaksBlocks.REINFORCED_FLYPAD);
        expect(helper, end, TweaksBlocks.NETHERITE_FLYPAD, enderite, TweaksBlocks.ENDERITE_FLYPAD);

        expect(helper, net, TweaksBlocks.SPAWN_TELEPORTER, netherite, TweaksBlocks.SPAWN_TELEPORTER_TIER_2);
        expect(helper, net, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, netherite, TweaksBlocks.SPAWN_TELEPORTER_TIER_3);
        expect(helper, net, TweaksBlocks.SPAWN_TELEPORTER_TIER_3, netherite, TweaksBlocks.SPAWN_TELEPORTER_TIER_4);
        expect(helper, end, TweaksBlocks.SPAWN_TELEPORTER_TIER_4, enderite, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER);

        // Die alten Rohstoff-Wege
        expectNothing(helper, net, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.NETHERITE_INGOT);
        expectNothing(helper, any, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.DIAMOND_BLOCK);
        expectNothing(helper, end, TweaksBlocks.LAUNCHPAD, ModItems.ENDERITE_INGOT);
        expectNothing(helper, net, TweaksBlocks.COPPER_PRESSURE_PLATE, Items.NETHERITE_INGOT);
        expectNothing(helper, end, TweaksBlocks.CHUNK_LOADER, ModItems.ENDERITE_INGOT);
        expectNothing(helper, any, TweaksBlocks.ELYTRA_PAD, Items.DIAMOND_BLOCK);
        expectNothing(helper, net, TweaksBlocks.REINFORCED_ELYTRA_PAD, Items.NETHERITE_INGOT);
        expectNothing(helper, end, TweaksBlocks.NETHERITE_ELYTRA_PAD, ModItems.ENDERITE_INGOT);
        expectNothing(helper, net, TweaksBlocks.FINE_ELYTRA_PAD, Items.NETHERITE_INGOT);
        expectNothing(helper, net, TweaksBlocks.FLYPAD, Items.NETHERITE_BLOCK);
        expectNothing(helper, end, TweaksBlocks.NETHERITE_FLYPAD, ModItems.ENDERITE_INGOT);
        expectNothing(helper, net, TweaksBlocks.SPAWN_TELEPORTER, Items.NETHERITE_INGOT);
        expectNothing(helper, end, TweaksBlocks.SPAWN_TELEPORTER_TIER_4, ModItems.ENDERITE_INGOT);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** Stellt einen Spieler auf ein volles Launchpad und laesst es bis zum Start ticken. */
    private static ServerPlayer launchFrom(GameTestHelper helper, BlockPos pos, Block pad, int charges) {
        helper.setBlock(pos, pad);
        LaunchpadBlockEntity be = helper.getBlockEntity(pos, LaunchpadBlockEntity.class);
        be.addCharges(charges, charges);
        ServerPlayer player = mockPlayer(helper, Vec3.atBottomCenterOf(pos));
        player.setDeltaMovement(Vec3.ZERO);
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getBlockState(pos);
        for (int t = 0; t < LaunchpadBlockEntity.LAUNCH_TICKS && be.getCharges() > 0; t++) {
            LaunchpadBlockEntity.tick(helper.getLevel(), abs, state, be);
        }
        helper.assertTrue(be.getCharges() == 0, pad + " did not launch within " + LaunchpadBlockEntity.LAUNCH_TICKS + " ticks");
        return player;
    }

    /** Laesst jede Item-Abklingzeit des Spielers ablaufen (Windkugel: 10 Ticks). */
    private static void expireCooldowns(ServerPlayer player) {
        for (int t = 0; t < 40; t++) {
            player.getCooldowns().tick();
        }
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    private static boolean isForced(ServerLevel level, int chunkX, int chunkZ) {
        return level.getForceLoadedChunks().contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static Optional<RecipeHolder<SmithingRecipe>> smithing(GameTestHelper helper, SmithingRecipeInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());
    }

    private static SmithingRecipeInput input(Item template, ItemLike base, ItemLike addition) {
        return new SmithingRecipeInput(new ItemStack(template), new ItemStack(base), new ItemStack(addition));
    }

    private static ItemStack assemble(GameTestHelper helper, RecipeHolder<SmithingRecipe> holder, SmithingRecipeInput input) {
        return holder.value().assemble(input, helper.getLevel().registryAccess());
    }

    private static void expect(GameTestHelper helper, Item template, ItemLike base, ItemLike addition, ItemLike result) {
        SmithingRecipeInput in = input(template, base, addition);
        Optional<RecipeHolder<SmithingRecipe>> match = smithing(helper, in);
        helper.assertTrue(match.isPresent(), "no smithing recipe turns " + base + " with " + addition + " into " + result);
        ItemStack out = assemble(helper, match.get(), in);
        helper.assertTrue(out.is(result.asItem()), "smithing " + base + " with " + addition + " made " + out + " instead of " + result);
    }

    private static void expectNothing(GameTestHelper helper, Item template, ItemLike base, ItemLike addition) {
        Optional<RecipeHolder<SmithingRecipe>> match = smithing(helper, input(template, base, addition));
        helper.assertTrue(match.isEmpty(), "the old way still smiths " + base + " with " + addition + " (" + match.map(h -> h.id().toString()).orElse("") + ")");
    }
}
