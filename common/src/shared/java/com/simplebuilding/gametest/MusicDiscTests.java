package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.LootInjection;
import com.simplebuilding.util.DiscFlips;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.MusicDiscs;
import com.simplebuilding.util.SpeakerBoost;
import com.simplebuilding.util.TransformTargets;
import com.simplebuilding.version.McVersion;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Schallplatten der Dimensionen, ihre B-Seiten und die Lautsprecher (2026-10-03, {@code McVersion.MUSIC_DISCS};
 * Plan {@code docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md}). Auf Linien ohne das Feature bestehen alle Tests sofort.
 *
 * <p>Nicht pruefbar im Server-Test: wie laut der Client ein Stueck hoert ({@code LevelEventHandlerSpeakerMixin}) -
 * geprueft wird die Rechnung, die er benutzt ({@link SpeakerBoost}), und alles, was der Server selbst tut.
 */
public final class MusicDiscTests {
    private MusicDiscTests() {
    }

    private static boolean enabled(GameTestHelper helper) {
        if (McVersion.MUSIC_DISCS) {
            return true;
        }
        helper.succeed();
        return false;
    }

    /** Jede Platte: Stapel 1, selten, ablegbar, mit einem geladenen Song, dessen Sound-Event registriert ist. */
    public static void discsCarryLoadedSongsWithRegisteredSounds(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        var songs = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
        List<String> problems = new ArrayList<>();
        helper.assertValueEqual(MusicDiscs.items().size(), 8, "disc count");
        for (MusicDiscs.Disc disc : MusicDiscs.discs()) {
            for (Item item : List.of(disc.aSide(), disc.bSide())) {
                String song = item == disc.aSide() ? disc.song().name() : disc.song().bSide();
                ItemStack stack = new ItemStack(item);
                JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
                if (playable == null) {
                    problems.add(item + " has no jukebox_playable");
                    continue;
                }
                Optional<Holder<JukeboxSong>> holder = JukeboxSong.fromStack(stack);
                if (holder.isEmpty()) {
                    problems.add(item + " names no loaded song");
                    continue;
                }
                ResourceKey<JukeboxSong> key = MusicDiscs.songKey(song);
                if (!holder.get().is(key)) {
                    problems.add(item + " plays " + holder.get() + ", expected " + key.identifier());
                }
                JukeboxSong value = holder.get().value();
                if (!value.soundEvent().is(MusicDiscs.soundId(song)) || !BuiltInRegistries.SOUND_EVENT.containsKey(MusicDiscs.soundId(song))) {
                    problems.add(song + " sound " + value.soundEvent() + " is not the registered " + MusicDiscs.soundId(song));
                }
                if (value.comparatorOutput() != disc.song().comparator() || value.lengthInSeconds() <= 0.0F) {
                    problems.add(song + " comparator " + value.comparatorOutput() + " length " + value.lengthInSeconds());
                }
                if (stack.getMaxStackSize() != 1 || stack.get(DataComponents.RARITY) != net.minecraft.world.item.Rarity.RARE) {
                    problems.add(item + " stacks to " + stack.getMaxStackSize() + ", rarity " + stack.get(DataComponents.RARITY));
                }
                if (!stack.is(ModTags.Items.PLACEABLE_SMALL)) {
                    problems.add(item + " cannot be placed as a small part");
                }
                if (!songs.containsKey(key)) {
                    problems.add(key.identifier() + " missing from the jukebox_song registry");
                }
            }
        }
        // Creeper-von-Skelett: nur die beiden Oberwelt-A-Seiten, wie Vanillas Oberwelt-Platten.
        for (Item item : MusicDiscs.items()) {
            boolean creeper = new ItemStack(item).is(ItemTags.CREEPER_DROP_MUSIC_DISCS);
            boolean expected = item == ModItems.MUSIC_DISC_DRIFTWOOD || item == ModItems.MUSIC_DISC_DAYBREAK;
            if (creeper != expected) {
                problems.add(item + (creeper ? " drops from creepers" : " does not drop from creepers"));
            }
        }
        // Die 26.3-sounds.json verdeckt die gemeinsame: sie muss den Kolbenklang weiter fuehren.
        try (InputStream in = MusicDiscTests.class.getResourceAsStream("/assets/simplebuilding/sounds.json")) {
            String sounds = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!sounds.contains("\"block.piston.bore\"")) {
                problems.add("sounds.json lost the piston bore sound");
            }
            for (String song : MusicDiscs.songNames()) {
                if (!sounds.contains("\"music_disc." + song + "\"") || !sounds.contains("records/" + song)) {
                    problems.add("sounds.json has no streamed track for " + song);
                }
            }
        } catch (java.io.IOException e) {
            problems.add("sounds.json unreadable: " + e);
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " disc problems: " + problems);
        helper.succeed();
    }

    /** Die A-Seiten liegen in ihren Struktur-Truhen (Inject-Tabellen), die B-Seiten nirgends. */
    public static void discsLieInTheirStructureChests(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var server = helper.getLevel().getServer();
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Object[][] cases = {
                {BuiltInLootTables.END_CITY_TREASURE, ModItems.MUSIC_DISC_VOIDLINE},
                {BuiltInLootTables.BASTION_OTHER, ModItems.MUSIC_DISC_BRIMSTONE},
                {BuiltInLootTables.WOODLAND_MANSION, ModItems.MUSIC_DISC_DRIFTWOOD},
                {BuiltInLootTables.ANCIENT_CITY, ModItems.MUSIC_DISC_DAYBREAK}};
        List<String> problems = new ArrayList<>();
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            ResourceKey<LootTable> key = (ResourceKey<LootTable>) c[0];
            LootTable table = server.reloadableRegistries().getLootTable(LootInjection.injectKey(key));
            String json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().toString();
            String disc = BuiltInRegistries.ITEM.getKey((Item) c[1]).toString();
            if (!json.contains("\"" + disc + "\"")) {
                problems.add(key.identifier() + " does not hold " + disc);
            }
            for (Item item : MusicDiscs.items()) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (item != c[1] && json.contains("\"" + id + "\"")) {
                    problems.add(key.identifier() + " also holds " + id);
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "disc loot: " + problems);
        helper.succeed();
    }

    /** Hammer wendet die abgelegte Platte hin und zurueck; der Plattenspieler spielt jeweils den passenden Song. */
    public static void hammerFlipsPlacedDiscBackAndForth(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
        pile.setParts(List.of(new ItemStack(Items.FLINT), new ItemStack(ModItems.MUSIC_DISC_VOIDLINE)));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));

        // Kein Hammer: eine Spitzhacke wendet nichts und neigt sich nicht.
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.assertTrue(!TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "pickaxe hints a flip");
        click(helper, player, rel, pickaxe);
        helper.assertTrue(pile.parts().get(1).is(ModItems.MUSIC_DISC_VOIDLINE), "a pickaxe flipped the disc");

        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        helper.assertTrue(TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "no hint for the hammer");
        click(helper, player, rel, hammer);
        helper.assertTrue(pile.parts().get(1).is(ModItems.MUSIC_DISC_VOIDLINE_B_SIDE), "A -> B: " + pile.parts());
        helper.assertTrue(pile.parts().get(0).is(Items.FLINT), "the flint changed: " + pile.parts());
        helper.assertValueEqual(hammer.getDamageValue(), 1, "hammer damage after one flip");
        click(helper, player, rel, hammer);
        helper.assertTrue(pile.parts().get(1).is(ModItems.MUSIC_DISC_VOIDLINE), "B -> A: " + pile.parts());
        helper.assertValueEqual(hammer.getDamageValue(), 2, "hammer damage after two flips");

        // Ohne Platte im Haeufchen tut der Hammer hier nichts.
        pile.setParts(List.of(new ItemStack(Items.FLINT)));
        helper.assertTrue(!DiscFlips.canFlip(level, helper.absolutePos(rel), hammer), "flip without a disc");

        // Der Plattenspieler spielt die A- bzw. B-Seite.
        var songs = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
        BlockPos box = new BlockPos(4, 1, 2);
        for (Item item : List.of(ModItems.MUSIC_DISC_BRIMSTONE, ModItems.MUSIC_DISC_BRIMSTONE_B_SIDE)) {
            helper.setBlock(box, Blocks.AIR);
            helper.setBlock(box, Blocks.JUKEBOX);
            JukeboxBlockEntity jukebox = (JukeboxBlockEntity) level.getBlockEntity(helper.absolutePos(box));
            jukebox.setTheItem(new ItemStack(item));
            String expected = item == ModItems.MUSIC_DISC_BRIMSTONE ? "brimstone" : "brimstone_b_side";
            helper.assertTrue(jukebox.getSongPlayer().isPlaying()
                    && jukebox.getSongPlayer().getSong() == songs.getValue(MusicDiscs.songKey(expected)), item + " does not play " + expected);
        }
        helper.succeed();
    }

    /** Astralit nur am Plattenspieler, Nihilit nur am Notenblock; Standard: hoechstens zwei zaehlen, +50 % je Stueck. */
    public static void speakersBoostOnlyTheirOwnSource(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos jukebox = new BlockPos(2, 2, 2);
        BlockPos note = new BlockPos(6, 2, 2);
        helper.setBlock(jukebox, Blocks.JUKEBOX);
        helper.setBlock(note, Blocks.NOTE_BLOCK);
        BlockPos j = helper.absolutePos(jukebox);
        BlockPos n = helper.absolutePos(note);
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.0F, "jukebox without speakers");
        helper.assertValueEqual(SpeakerBoost.noteBlockVolume(level, n), 3.0F, "vanilla note block volume");

        // Falsche Lautsprecher: Nihilit am Plattenspieler, Astralit am Notenblock wirken nicht.
        helper.setBlock(jukebox.east(), ModBlocks.NIHILITH_SPEAKER);
        helper.setBlock(note.east(), ModBlocks.ASTRALIT_SPEAKER);
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.0F, "nihilit speaker boosted a jukebox");
        helper.assertValueEqual(SpeakerBoost.noteBlockVolume(level, n), 3.0F, "astralit speaker boosted a note block");

        // Richtige Lautsprecher: einer gibt +50 %.
        helper.setBlock(jukebox.west(), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(note.west(), ModBlocks.NIHILITH_SPEAKER);
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.5F, "one astralit speaker");
        helper.assertValueEqual(SpeakerBoost.noteBlockVolume(level, n), 4.5F, "one nihilit speaker");

        // Vier ringsum: es zaehlen nur zwei (Standard), also hoechstens doppelt.
        helper.setBlock(jukebox.east(), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(jukebox.north(), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(jukebox.above(), ModBlocks.ASTRALIT_SPEAKER);
        helper.assertValueEqual(SpeakerBoost.adjacentSpeakers(level, j, SpeakerBoost.Source.JUKEBOX), 4, "adjacent astralit speakers");
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 2.0F, "capped at two speakers");
        helper.assertValueEqual(SpeakerBoost.jukeboxEventRange(2.0F), 128.0, "jukebox reach with two speakers");
        helper.assertValueEqual(SpeakerBoost.jukeboxEventRange(1.0F), 64.0, "vanilla jukebox reach");
        helper.succeed();
    }

    /** Start/Stopp jenseits von 64 Bloecken nur an die Spieler bis zur verstaerkten Hoerweite; Config-Grenzen. */
    public static void speakerRangeAndConfigCaps(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerPlayer near = helper.makeMockServerPlayerInLevel();
        ServerPlayer middle = helper.makeMockServerPlayerInLevel();
        ServerPlayer far = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> {
            level.getServer().getPlayerList().remove(near);
            level.getServer().getPlayerList().remove(middle);
            level.getServer().getPlayerList().remove(far);
        });
        near.snapTo(origin.getX() + 30.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        middle.snapTo(origin.getX() + 100.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        far.snapTo(origin.getX() + 140.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        // Andere Spieler der Testwelt koennen auch in der Zone stehen: gezaehlt wird nur, dass "middle" dabei ist.
        int beyond = SpeakerBoost.sendBeyondVanilla(level, origin,
                new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, origin, 0, false), SpeakerBoost.jukeboxEventRange(2.0F));
        helper.assertTrue(beyond >= 1, "the player 100 blocks away got no packet");
        helper.assertValueEqual(SpeakerBoost.sendBeyondVanilla(level, origin,
                new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, origin, 0, false), 64.0), 0, "vanilla range sends extra packets");

        ServerTuningConfig config = new ServerTuningConfig();
        config.speakers.maxSpeakers = 9;
        config.speakers.boostPercent = 500;
        config.validate();
        helper.assertValueEqual(config.speakers.maxSpeakers, ServerTuning.MAX_SPEAKERS, "max speakers cap");
        helper.assertValueEqual(config.speakers.boostPercent, ServerTuning.MAX_SPEAKER_BOOST_PERCENT, "boost cap");
        config.speakers.maxSpeakers = -4;
        config.validate();
        helper.assertValueEqual(config.speakers.maxSpeakers, 0, "max speakers floor");

        ServerTuningConfig.Speakers live = ServerTuning.local().speakers;
        int oldMax = live.maxSpeakers;
        int oldBoost = live.boostPercent;
        try {
            live.maxSpeakers = 0;
            helper.assertValueEqual(SpeakerBoost.multiplier(3), 1.0F, "maxSpeakers 0 switches speakers off");
            live.maxSpeakers = 99;
            live.boostPercent = 99;
            // Handeditierte Werte ueber der Grenze: hoechstens 3 x 50 % = 2,5-fach, Plattenspieler 160 Bloecke.
            helper.assertValueEqual(SpeakerBoost.multiplier(6), 2.5F, "hard cap of the multiplier");
            helper.assertValueEqual(SpeakerBoost.jukeboxEventRange(SpeakerBoost.maxMultiplier()), 160.0, "hard cap of the jukebox reach");
        } finally {
            live.maxSpeakers = oldMax;
            live.boostPercent = oldBoost;
        }
        helper.succeed();
    }

    /** Acht Bretter um Astralitstaub bzw. Nihilitsplitter ergeben den Lautsprecher (jede Holzart). */
    public static void speakerRecipesUseAnyPlanks(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        Object[][] cases = {{ModItems.ASTRALIT_DUST, ModItems.ASTRALIT_SPEAKER, Items.OAK_PLANKS},
                {ModItems.NIHILITH_SHARD, ModItems.NIHILITH_SPEAKER, Items.CRIMSON_PLANKS}};
        for (Object[] c : cases) {
            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                grid.add(new ItemStack(i == 4 ? (Item) c[0] : (Item) c[2]));
            }
            CraftingInput input = CraftingInput.of(3, 3, grid);
            Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            helper.assertTrue(match.isPresent() && match.get().value().assemble(input).is((Item) c[1]), "no speaker recipe for " + c[0]);
        }
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos rel) {
        BlockPos absolute = helper.absolutePos(rel);
        return new BlockHitResult(Vec3.atBottomCenterOf(absolute).add(0.0, 0.05, 0.0), Direction.UP, absolute, false);
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 eye = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY() + 1.0, rel.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(helper, rel));
    }
}
