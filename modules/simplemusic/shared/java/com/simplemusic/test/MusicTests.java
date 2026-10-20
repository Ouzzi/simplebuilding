package com.simplemusic.test;

import com.google.gson.JsonObject;
import com.simplemusic.SimpleMusic;
import com.simplemusic.config.MusicConfig;
import com.simplemusic.disc.DiscPlayback;
import com.simplemusic.disc.JukeboxDiscs;
import com.simplemusic.disc.TrackRef;
import com.simplemusic.net.SheetEditPayload;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import com.simplemusic.net.UploadBeginPayload;
import com.simplemusic.net.UploadChunkPayload;
import com.simplemusic.net.UploadResultPayload;
import com.simplemusic.orchestrion.OrchestrionBlockEntity;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetContent;
import com.simplemusic.sheet.SheetSounds;
import com.simplemusic.upload.MusicServer;
import com.simplemusic.upload.OggCheck;
import com.simplemusic.upload.OggFiles;
import com.simplemusic.upload.TrackStore;
import com.simplemusic.upload.UploadSession;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/**
 * Loader independent module tests (plan docs/ai/PLAN-B-MUSIC-2026-10-20.md); the Fabric/NeoForge/Forge adapters register
 * them as {@code module_game_test_<name>} on {@code simplemusic:empty} (8 x 8 x 8). Every test runs standalone.
 */
public final class MusicTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    static final BlockPos ORCH = new BlockPos(3, 1, 3);
    /** harp c4 d4 e4 at 2 ticks per step: 3 steps, 6 ticks per run. */
    static final String TUNE = "tempo 2\nharp: c4 d4 e4";

    static ItemStack sheet(String code) {
        ItemStack stack = new ItemStack(MusicRegistry.SHEET_MUSIC);
        stack.set(MusicComponents.SHEET, new SheetContent(code, "Test", "Tester", true));
        return stack;
    }

    static OrchestrionBlockEntity orchestrion(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, MusicRegistry.ORCHESTRION.defaultBlockState());
        return h.getBlockEntity(pos, OrchestrionBlockEntity.class);
    }

    /** What the server sent to each player (tests run in parallel, so one recorder for all, read per player). */
    static final Map<java.util.UUID, List<CustomPacketPayload>> SENT = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean recording;

    /** The payloads sent to {@code player} from now on. */
    static List<CustomPacketPayload> capture(ServerPlayer player) {
        if (!recording) {
            recording = true;
            BiConsumer<ServerPlayer, CustomPacketPayload> loader = SimpleMusic.toClient;
            SimpleMusic.toClient = (p, payload) -> {
                SENT.computeIfAbsent(p.getUUID(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(payload);
                loader.accept(p, payload);
            };
        }
        return SENT.computeIfAbsent(player.getUUID(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());
    }

    static void restore() {
        MusicServer.clock = System::currentTimeMillis;
        MusicConfig.reset();
    }

    static String last(List<CustomPacketPayload> sent) {
        for (int i = sent.size() - 1; i >= 0; i--) if (sent.get(i) instanceof UploadResultPayload r) return r.key();
        return "none";
    }

    /** A unique mono test file (random serial, so the store never has it from an earlier run). */
    static byte[] uniqueOgg(int channels, int seconds) {
        OggFiles f = OggFiles.mono();
        f.channels = channels;
        f.samples = 44100L * seconds;
        f.serial = (int) (System.nanoTime() ^ (long) (Math.random() * Integer.MAX_VALUE));
        f.audioBytesPerPage = 40000;
        return f.build();
    }

    /** Runs a whole upload for the player with a fake clock (chunks 1 s apart, below every rate). */
    static void upload(ServerPlayer player, byte[] data, String title) {
        long[] now = {1_000_000L};
        MusicServer.clock = () -> now[0];
        MusicServer.handleUploadBegin(new UploadBeginPayload(0, title, data.length), player);
        for (int i = 0; i * UploadSession.MAX_CHUNK < data.length && MusicServer.uploading(player.getUUID()); i++) {
            now[0] += 1000;
            int from = i * UploadSession.MAX_CHUNK;
            MusicServer.handleUploadChunk(new UploadChunkPayload(i, Arrays.copyOfRange(data, from, Math.min(data.length, from + UploadSession.MAX_CHUNK))), player);
        }
    }

    static {
        ALL.put("config_defaults", h -> {
            MusicConfig.reset();
            h.assertTrue(MusicConfig.enabled && MusicConfig.sheetSounds == MusicConfig.SheetSounds.ALL && MusicConfig.maxTonesPerTick == 8
                    && MusicConfig.uploads == MusicConfig.Uploads.APPROVE && MusicConfig.maxUploadMiB == 4 && MusicConfig.maxUploadSeconds == 300
                    && MusicConfig.quotaPerPlayer == 5 && MusicConfig.jukeboxCustomDiscs, "defaults (owner decisions N4 a, U2 b, U3)");
            h.assertTrue(MusicConfig.blocked("minecraft:music.game") && MusicConfig.blocked("minecraft:music_disc.cat")
                    && MusicConfig.blocked("minecraft:ui.button.click") && !MusicConfig.blocked("minecraft:entity.cat.ambient"), "blocklist");
            JsonObject json = new JsonObject();
            json.addProperty("maxTonesPerTick", 99);
            json.addProperty("maxUploadMiB", 100);
            json.addProperty("uploads", "op");
            json.addProperty("quotaPerPlayer", "many");
            MusicConfig.apply(json);
            h.assertTrue(MusicConfig.maxTonesPerTick == 8 && MusicConfig.maxUploadMiB == 16 && MusicConfig.uploads == MusicConfig.Uploads.OP
                    && MusicConfig.quotaPerPlayer == 5, "clamped, enum read, junk = default");
            h.assertTrue(MusicConfig.toJson().keySet().containsAll(MusicConfig.KEYS) && MusicConfig.KEYS.containsAll(MusicConfig.toJson().keySet()), "keys");
            MusicConfig.reset();
            h.succeed();
        });
        ALL.put("registered", h -> {
            for (String id : List.of("sheet_music", "orchestrion", "blank_disc", "custom_disc", "guide_book")) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleMusic.id(id)), "item " + id);
            }
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleMusic.id("orchestrion")), "block");
            h.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleMusic.id("sheet"))
                    && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleMusic.id("track")), "components");
            h.succeed();
        });
        ALL.put("sounds_policy", h -> {
            NoteCode.SoundPolicy policy = SheetSounds.policy();
            h.assertTrue(policy.check("minecraft:entity.cat.ambient") == null, "any registered sound");
            h.assertTrue("blocked_sound".equals(policy.check("minecraft:music.game")), "music blocked");
            h.assertTrue("unknown_sound".equals(policy.check("minecraft:no.such.sound")), "unknown");
            h.assertTrue(SheetSounds.headSounds().contains(net.minecraft.resources.Identifier.withDefaultNamespace("entity.zombie.ambient")), "zombie head sound");
            NoteCode.Song song = NoteCode.parse("$c = sound minecraft:entity.cat.ambient\n$c: f#4\nbass: c3\ncreeper: c4", policy).song();
            h.assertTrue(Arrays.stream(SheetSounds.sounds(song)).allMatch(java.util.Objects::nonNull), "every instrument resolves");
            MusicConfig.sheetSounds = MusicConfig.SheetSounds.INSTRUMENTS;
            h.assertTrue("sound_not_allowed".equals(SheetSounds.policy().check("minecraft:entity.cat.ambient")), "instruments only");
            MusicConfig.reset();
            h.succeed();
        });
        ALL.put("sheet_save_rules", h -> {
            SheetContent draft = new SheetContent("", "", "", false);
            SheetContent saved = MusicServer.edited(draft, new SheetEditPayload(0, "harp: c4 zz", "", false), "Ann");
            h.assertTrue(saved != null && !saved.signed(), "an unsigned draft may have problems");
            h.assertTrue(MusicServer.edited(draft, new SheetEditPayload(0, "harp: c4 zz", "Song", true), "Ann") == null, "signing needs a valid code");
            h.assertTrue(MusicServer.edited(draft, new SheetEditPayload(0, "$m = sound minecraft:music.game\n$m: f#4", "Song", true), "Ann") == null,
                    "a blocked sound is refused when signing");
            h.assertTrue(MusicServer.edited(draft, new SheetEditPayload(0, TUNE, "", true), "Ann") == null, "signing needs a title");
            SheetContent signed = MusicServer.edited(draft, new SheetEditPayload(0, TUNE, "Song", true), "Ann");
            h.assertTrue(signed != null && signed.signed() && signed.author().equals("Ann"), "signed with author");
            h.assertTrue(MusicServer.edited(signed, new SheetEditPayload(0, "harp: c4", "x", false), "Bob") == null, "signed = read-only");
            h.assertTrue(MusicServer.edited(draft, new SheetEditPayload(0, TUNE, "x".repeat(NoteCode.MAX_TITLE_LENGTH + 1), false), "Ann") == null, "title limit");
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.SHEET_MUSIC, 3));
            MusicServer.handleSheetEdit(new SheetEditPayload(0, TUNE, "Song", true), player);
            ItemStack hand = player.getMainHandItem();
            h.assertTrue(hand.getCount() == 1 && com.simplemusic.sheet.SheetMusicItem.content(hand).signed(), "one of the stack is written");
            h.assertTrue(player.getInventory().countItem(MusicRegistry.SHEET_MUSIC) == 3, "the rest stays blank");
            h.succeed();
        });
        ALL.put("orchestrion_plays_steps", h -> {
            OrchestrionBlockEntity be = orchestrion(h, ORCH);
            h.assertTrue(!OrchestrionBlockEntity.playable(sheet("harp: zz")) && !OrchestrionBlockEntity.playable(sheet("")), "bad sheets refused");
            h.assertTrue(be.insert(sheet(TUNE)), "insert");
            h.assertTrue(h.getBlockState(ORCH).getValue(com.simplemusic.orchestrion.OrchestrionBlock.HAS_RECORD), "has record");
            h.assertTrue(!be.insert(sheet(TUNE)), "only one");
            // one tone every 2 ticks (the BE may tick before or after the scheduled check within a tick)
            h.runAfterDelay(2, () -> h.assertTrue(be.tonesPlayed() >= 1 && be.tonesPlayed() <= 2, "step 1 or 2 by tick 2: " + be.tonesPlayed()));
            h.runAfterDelay(5, () -> h.assertTrue(be.tonesPlayed() >= 2 && be.tonesPlayed() <= 3, "step 2 or 3 by tick 5: " + be.tonesPlayed()));
            h.runAfterDelay(4, () -> {
                int c = be.comparatorOutput();
                h.assertTrue(c >= 1 && c <= 15, "comparator in range: " + c);
            });
            h.runAfterDelay(14, () -> {
                h.assertTrue(be.runs() >= 2, "loops: " + be.runs());
                h.assertTrue(!be.item().isEmpty(), "loops without ejecting");
                h.assertTrue(be.tonesPlayed() >= 6, "kept playing: " + be.tonesPlayed());
                h.succeed();
            });
        });
        MAX_TICKS.put("orchestrion_plays_steps", 60);
        ALL.put("orchestrion_redstone", h -> {
            OrchestrionBlockEntity be = orchestrion(h, ORCH);
            be.insert(sheet("tempo 4\nharp: c4 d4 e4 f#4 g4 a4"));
            BlockPos power = ORCH.east();
            h.runAfterDelay(2, () -> h.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState()));
            h.runAfterDelay(4, () -> h.assertTrue(be.step() == 0 && be.playing(), "rising edge starts from the top: " + be.step()));
            h.runAfterDelay(9, () -> h.setBlock(power, Blocks.AIR.defaultBlockState()));
            h.runAfterDelay(16, () -> {
                int stopped = be.tonesPlayed();
                h.assertTrue(!be.playing(), "signal off stops at the end of the step");
                h.runAfterDelay(10, () -> {
                    h.assertTrue(be.tonesPlayed() == stopped, "stays silent while off");
                    h.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
                });
            });
            h.runAfterDelay(29, () -> {
                h.assertTrue(be.playing() && be.step() <= 1, "starts again from the top: " + be.step());
                h.succeed();
            });
        });
        MAX_TICKS.put("orchestrion_redstone", 80);
        ALL.put("orchestrion_playlist", h -> {
            OrchestrionBlockEntity be = orchestrion(h, ORCH);
            h.setBlock(ORCH.above(), Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.UP));
            Container barrel = (Container) h.getBlockEntity(ORCH.above(), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
            barrel.setItem(4, new ItemStack(Items.DIRT));
            barrel.setItem(5, sheet("tempo 4\nbass: c3 c3"));
            be.insert(sheet(TUNE));
            // TUNE runs 6 ticks, the bass sheet 8: bass from about tick 6 to 14, then TUNE again
            h.runAfterDelay(10, () -> {
                h.assertTrue(com.simplemusic.sheet.SheetMusicItem.content(be.item()).code().contains("bass"), "next sheet from the container");
                h.assertTrue(com.simplemusic.sheet.SheetMusicItem.content(barrel.getItem(5)).code().equals(TUNE), "finished sheet took its slot");
                h.assertTrue(barrel.getItem(4).is(Items.DIRT), "other items stay");
            });
            h.runAfterDelay(18, () -> {
                h.assertTrue(com.simplemusic.sheet.SheetMusicItem.content(be.item()).code().equals(TUNE), "and back");
                h.succeed();
            });
        });
        MAX_TICKS.put("orchestrion_playlist", 60);
        ALL.put("orchestrion_pop_out", h -> {
            OrchestrionBlockEntity be = orchestrion(h, ORCH);
            be.insert(sheet(TUNE));
            be.popOut();
            h.assertTrue(be.item().isEmpty() && !h.getBlockState(ORCH).getValue(com.simplemusic.orchestrion.OrchestrionBlock.HAS_RECORD), "empty");
            h.succeedWhenEntityPresent(net.minecraft.world.entity.EntityTypes.ITEM, ORCH.above());
        });
        ALL.put("tone_limit", h -> {
            OrchestrionBlockEntity be = orchestrion(h, ORCH);
            be.insert(sheet("tempo 20\nharp: [c4 e4 g4 c5]"));
            // ticked by hand within this callback: other tests reset the shared config between ticks
            MusicConfig.maxTonesPerTick = 2;
            be.tick(h.getLevel());
            int played = be.tonesPlayed();
            MusicConfig.reset();
            h.assertTrue(played == 2, "at most maxTonesPerTick: " + played);
            h.succeed();
        });
        ALL.put("upload_moderation_download", h -> {
            ServerPlayer alice = h.makeMockServerPlayerInLevel();
            ServerPlayer bob = h.makeMockServerPlayerInLevel();
            List<CustomPacketPayload> sent = capture(alice);
            try {
                alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.BLANK_DISC));
                byte[] stereo = uniqueOgg(2, 5);
                upload(alice, stereo, "Stereo");
                h.assertTrue(last(sent).equals("stereo"), "stereo refused: " + last(sent));
                h.assertTrue(alice.getMainHandItem().is(MusicRegistry.BLANK_DISC), "blank disc kept");
                byte[] mono = uniqueOgg(1, 5);
                upload(alice, mono, "Mono");
                h.assertTrue(last(sent).equals("pending"), "stored, waits for an OP: " + last(sent));
                ItemStack disc = alice.getMainHandItem();
                TrackRef ref = disc.get(MusicComponents.TRACK);
                h.assertTrue(disc.is(MusicRegistry.CUSTOM_DISC) && ref != null && ref.seconds() == 5 && ref.title().equals("Mono"), "custom disc");
                TrackStore store = MusicServer.store(h.getLevel().getServer());
                TrackStore.Track track = store.get(ref.sha256());
                h.assertTrue(track != null && track.status() == TrackStore.Status.PENDING, "pending in the index");
                h.assertTrue(ref.sha256().equals(UploadSession.sha256(OggCheck.strip(mono))), "hash of the stripped file, computed by the server");
                // only the uploader hears a pending disc
                OrchestrionBlockEntity be = orchestrion(h, ORCH);
                h.assertTrue(be.insert(disc.copy()), "disc into the Orchestrion");
                BlockPos abs = h.absolutePos(ORCH);
                alice.teleportTo(abs.getX(), abs.getY() + 2, abs.getZ());
                bob.teleportTo(abs.getX() + 2, abs.getY() + 2, abs.getZ());
                h.runAfterDelay(2, () -> {
                    var listeners = DiscPlayback.listeners(h.getLevel(), abs);
                    h.assertTrue(listeners.contains(alice.getUUID()) && !listeners.contains(bob.getUUID()), "pending: uploader only " + listeners);
                    h.assertTrue(sent.stream().anyMatch(p -> p instanceof TrackPlayPayload t && !t.isStop() && t.sha256().equals(ref.sha256())), "play sent");
                    MusicServer.handleTrackRequest(new TrackRequestPayload(ref.sha256()), bob);
                    h.assertTrue(MusicServer.queuedDownloads(bob.getUUID()) == 0, "no download for others while pending");
                    MusicServer.handleTrackRequest(new TrackRequestPayload(ref.sha256()), alice);
                    h.assertTrue(MusicServer.queuedDownloads(alice.getUUID()) == 1, "download for the uploader");
                    for (int i = 0; i < 40; i++) MusicServer.tick(h.getLevel().getServer());
                    ByteArrayOutputStream got = new ByteArrayOutputStream();
                    sent.stream().filter(p -> p instanceof TrackDataPayload d && d.sha256().equals(ref.sha256()))
                            .forEach(p -> got.writeBytes(((TrackDataPayload) p).bytes()));
                    h.assertTrue(UploadSession.sha256(got.toByteArray()).equals(ref.sha256()), "downloaded file matches");
                    store.setStatus(ref.sha256(), TrackStore.Status.APPROVED);
                    DiscPlayback.update(h.getLevel(), abs);
                    h.assertTrue(DiscPlayback.listeners(h.getLevel(), abs).contains(bob.getUUID()), "approved: everyone near hears it");
                    store.setStatus(ref.sha256(), TrackStore.Status.DELETED);
                    DiscPlayback.update(h.getLevel(), abs);
                    h.assertTrue(DiscPlayback.listeners(h.getLevel(), abs).isEmpty(), "deleted: the disc is silent");
                    // quota
                    MusicConfig.quotaPerPlayer = 0;
                    alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.BLANK_DISC));
                    MusicServer.handleUploadBegin(new UploadBeginPayload(0, "More", 1000), alice);
                    h.assertTrue(last(sent).equals("quota"), "quota: " + last(sent));
                    MusicConfig.reset();
                    store.ban(alice.getUUID(), true);
                    MusicServer.handleUploadBegin(new UploadBeginPayload(0, "More", 1000), alice);
                    h.assertTrue(last(sent).equals("banned"), "banned: " + last(sent));
                    store.ban(alice.getUUID(), false);
                    MusicConfig.uploads = MusicConfig.Uploads.OFF;
                    MusicServer.handleUploadBegin(new UploadBeginPayload(0, "More", 1000), alice);
                    h.assertTrue(last(sent).equals("uploads_off"), "off: " + last(sent));
                    restore();
                    be.popOut();
                    h.succeed();
                });
            } catch (RuntimeException e) {
                restore();
                throw e;
            }
        });
        MAX_TICKS.put("upload_moderation_download", 100);
        ALL.put("upload_rejects_bad_chunks", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            List<CustomPacketPayload> sent = capture(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.BLANK_DISC));
            MusicServer.clock = () -> 5_000_000L;
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "Gap", 100_000), player);
            MusicServer.handleUploadChunk(new UploadChunkPayload(1, new byte[100]), player);
            h.assertTrue(last(sent).equals("missing_chunk") && !MusicServer.uploading(player.getUUID()), "gap aborts");
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "", 100), player);
            h.assertTrue(last(sent).equals("bad_title"), "title needed");
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "Big", 17 << 20), player);
            h.assertTrue(last(sent).equals("too_big"), "announced size checked");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "X", 100), player);
            h.assertTrue(last(sent).equals("no_blank"), "needs the blank disc");
            restore();
            h.succeed();
        });
        ALL.put("jukebox_custom_disc", h -> {
            BlockPos pos = new BlockPos(1, 1, 1);
            h.setBlock(pos, Blocks.JUKEBOX.defaultBlockState());
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            ItemStack disc = new ItemStack(MusicRegistry.CUSTOM_DISC);
            disc.set(MusicComponents.TRACK, new TrackRef("a".repeat(64), "T", 1, "x"));
            player.setItemInHand(InteractionHand.MAIN_HAND, disc);
            h.assertTrue(JukeboxDiscs.on(h.getLevel()), "jukebox path on (no Vanilla playlist on 26.3)");
            h.useBlock(pos, player);
            JukeboxBlockEntity jukebox = h.getBlockEntity(pos, JukeboxBlockEntity.class);
            h.assertTrue(JukeboxDiscs.isDisc(jukebox.getTheItem()), "custom disc in the Vanilla jukebox");
            BlockPos abs = h.absolutePos(pos);
            h.runAfterDelay(3, () -> h.assertTrue(DiscPlayback.playing(h.getLevel(), abs), "plays"));
            h.runAfterDelay(30, () -> {
                h.assertTrue(!DiscPlayback.playing(h.getLevel(), abs), "a jukebox plays it once (1 s)");
                h.assertTrue(JukeboxDiscs.isDisc(jukebox.getTheItem()), "disc stays in");
                jukebox.popOutTheItem();
                restore();
                h.succeed();
            });
        });
        MAX_TICKS.put("jukebox_custom_disc", 60);
        ALL.put("guide_book", com.simplemusic.guide.MusicGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplemusic.discover.MusicDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplemusic.discover.MusicDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplemusic.discover.MusicDiscover::firstPickupTips);
    }

    private MusicTests() {}
}
