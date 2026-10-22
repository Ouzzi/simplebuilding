package com.simplemusic.test;

import com.google.gson.JsonObject;
import com.simplemusic.SimpleMusic;
import com.simplemusic.config.MusicConfig;
import com.simplemusic.disc.CustomDiscItem;
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
import com.simplemusic.pocket.PocketJukeboxItem;
import com.simplemusic.pocket.PocketPlayback;
import com.simplemusic.pocket.PocketJukeboxBlockEntity;
import com.simplemusic.pocket.PocketState;
import com.simplemusic.press.DiscPress;
import com.simplemusic.press.DiscPressBlockEntity;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetContent;
import com.simplemusic.sheet.SheetMusicItem;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
            for (String id : List.of("sheet_music", "orchestrion", "disc_press", "blank_disc", "custom_disc", "guide_book", "pocket_jukebox", "black_pocket_jukebox")) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleMusic.id(id)), "item " + id);
            }
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleMusic.id("orchestrion")) && BuiltInRegistries.BLOCK.containsKey(SimpleMusic.id("disc_press"))
                    && BuiltInRegistries.BLOCK.containsKey(SimpleMusic.id("black_pocket_jukebox"))
                    && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleMusic.id("pocket_jukebox")), "blocks");
            h.assertTrue(BuiltInRegistries.MENU.containsKey(SimpleMusic.id("disc_press")), "press menu");
            h.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleMusic.id("sheet"))
                    && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleMusic.id("track"))
                    && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleMusic.id("pocket")), "components");
            h.assertTrue(MusicRegistry.POCKET_JUKEBOXES.size() == 17, "17 pocket jukeboxes");
            h.succeed();
        });
        ALL.put("sounds_policy", h -> {
            NoteCode.SoundPolicy policy = SheetSounds.policy();
            h.assertTrue(policy.check("minecraft:entity.cat.ambient") == null, "any registered sound");
            h.assertTrue("blocked_sound".equals(policy.check("minecraft:music.game")), "music blocked");
            h.assertTrue("unknown_sound".equals(policy.check("minecraft:no.such.sound")), "unknown");
            h.assertTrue(SheetSounds.headSounds().contains(net.minecraft.resources.Identifier.withDefaultNamespace("block.note_block.imitate.zombie")), "zombie head sound");
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
                alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.SHEET_MUSIC, 2));
                byte[] stereo = uniqueOgg(2, 5);
                upload(alice, stereo, "Stereo");
                h.assertTrue(last(sent).equals("stereo"), "stereo refused: " + last(sent));
                h.assertTrue(alice.getMainHandItem().getCount() == 2 && SheetMusicItem.track(alice.getMainHandItem()) == null, "sheets kept blank");
                byte[] mono = uniqueOgg(1, 5);
                upload(alice, mono, "Mono");
                h.assertTrue(last(sent).equals("pending"), "stored, waits for an OP: " + last(sent));
                ItemStack uploaded = alice.getMainHandItem();
                TrackRef ref = SheetMusicItem.track(uploaded);
                h.assertTrue(uploaded.getCount() == 1 && ref != null && ref.seconds() == 5 && ref.title().equals("Mono")
                        && SheetMusicItem.content(uploaded).signed(), "upload sheet (track, signed): " + uploaded);
                h.assertTrue(alice.getInventory().countItem(MusicRegistry.SHEET_MUSIC) == 2, "the other sheet stays blank");
                // owner 21.10.: the press makes the disc, the sheet stays
                ItemStack disc = DiscPress.result(uploaded, new ItemStack(MusicRegistry.BLANK_DISC));
                h.assertTrue(disc.is(MusicRegistry.CUSTOM_DISC) && ref.equals(disc.get(MusicComponents.TRACK)), "pressed track disc");
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
                    alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.SHEET_MUSIC));
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
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.SHEET_MUSIC));
            MusicServer.clock = () -> 5_000_000L;
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "Gap", 100_000), player);
            MusicServer.handleUploadChunk(new UploadChunkPayload(1, new byte[100]), player);
            h.assertTrue(last(sent).equals("missing_chunk") && !MusicServer.uploading(player.getUUID()), "gap aborts");
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "", 100), player);
            h.assertTrue(last(sent).equals("bad_title"), "title needed");
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "Big", 17 << 20), player);
            h.assertTrue(last(sent).equals("too_big"), "announced size checked");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MusicRegistry.BLANK_DISC));
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "X", 100), player);
            h.assertTrue(last(sent).equals("no_sheet"), "a blank disc no longer takes uploads (owner 21.10.)");
            player.setItemInHand(InteractionHand.MAIN_HAND, sheet(TUNE));
            MusicServer.handleUploadBegin(new UploadBeginPayload(0, "X", 100), player);
            h.assertTrue(last(sent).equals("no_sheet"), "a signed sheet is read-only");
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
            // a pressed composition plays as server tones, once
            BlockPos pos2 = new BlockPos(5, 1, 1);
            h.setBlock(pos2, Blocks.JUKEBOX.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, DiscPress.result(sheet(TUNE), new ItemStack(MusicRegistry.BLANK_DISC)));
            h.useBlock(pos2, player);
            JukeboxBlockEntity jukebox2 = h.getBlockEntity(pos2, JukeboxBlockEntity.class);
            h.assertTrue(JukeboxDiscs.isDisc(jukebox2.getTheItem()), "composition disc in the Vanilla jukebox");
            BlockPos abs2 = h.absolutePos(pos2);
            h.runAfterDelay(2, () -> h.assertTrue(com.simplemusic.disc.SheetPlayback.playing(h.getLevel(), abs2), "composition plays"));
            h.runAfterDelay(30, () -> {
                h.assertTrue(!DiscPlayback.playing(h.getLevel(), abs), "a jukebox plays it once (1 s)");
                h.assertTrue(JukeboxDiscs.isDisc(jukebox.getTheItem()), "disc stays in");
                h.assertTrue(!com.simplemusic.disc.SheetPlayback.playing(h.getLevel(), abs2) && JukeboxDiscs.isDisc(jukebox2.getTheItem()),
                        "the composition ends after one run (6 ticks), the disc stays in");
                jukebox2.popOutTheItem();
                jukebox.popOutTheItem();
                restore();
                h.succeed();
            });
        });
        MAX_TICKS.put("jukebox_custom_disc", 60);
        ALL.put("pocket_recipe", h -> {
            var level = h.getLevel();
            for (int i = 0; i < MusicRegistry.POCKET_JUKEBOXES.size(); i++) {
                net.minecraft.world.item.DyeColor color = i == 0 ? null : net.minecraft.world.item.DyeColor.values()[i - 1];
                ItemStack bundle = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(
                        color == null ? "bundle" : color.getSerializedName() + "_bundle")));
                ItemStack n = new ItemStack(Items.COPPER_NUGGET);
                net.minecraft.world.item.crafting.CraftingInput input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                        List.of(n, new ItemStack(Items.DIAMOND), n, n, bundle, n, n, n, n));
                ItemStack out = craft(h, input);
                h.assertTrue(out.is(MusicRegistry.POCKET_JUKEBOXES.get(i)), "8 copper nuggets + diamond + " + bundle + " -> " + out);
            }
            ItemStack jukebox = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(0));
            PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_CAT));
            PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_13));
            ItemStack blue = craft(h, net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(jukebox, new ItemStack(Items.DYE.blue()))));
            h.assertTrue(blue.is(MusicRegistry.POCKET_JUKEBOXES.get(1 + net.minecraft.world.item.DyeColor.BLUE.ordinal())), "dyed blue: " + blue);
            h.assertTrue(PocketJukeboxItem.contents(blue).size() == 2, "dyeing keeps the discs");
            ItemStack red = craft(h, net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(blue, new ItemStack(Items.DYE.red()))));
            h.assertTrue(red.getItem() instanceof PocketJukeboxItem p && p.color == net.minecraft.world.item.DyeColor.RED, "dyed again: " + red);
            h.assertTrue(!jukebox.is(net.minecraft.tags.ItemTags.BUNDLES), "not a Vanilla bundle (its dye recipes would eat it)");
            h.succeed();
        });
        ALL.put("pocket_insert_remove", h -> {
            ItemStack jukebox = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(0));
            h.assertTrue(!PocketJukeboxItem.insert(jukebox, new ItemStack(Items.DIRT)), "no dirt");
            h.assertTrue(!PocketJukeboxItem.insert(jukebox, new ItemStack(MusicRegistry.CUSTOM_DISC)), "no disc without a track");
            h.assertTrue(!PocketJukeboxItem.insert(jukebox, new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(3))), "no jukebox in a jukebox");
            ItemStack cat = new ItemStack(Items.MUSIC_DISC_CAT);
            h.assertTrue(PocketJukeboxItem.insert(jukebox, cat) && cat.isEmpty(), "a Vanilla disc");
            ItemStack custom = new ItemStack(MusicRegistry.CUSTOM_DISC);
            custom.set(MusicComponents.TRACK, new TrackRef("b".repeat(64), "T", 3, "x"));
            h.assertTrue(PocketJukeboxItem.insert(jukebox, custom), "a custom disc");
            for (int i = 2; i < PocketJukeboxItem.CAPACITY; i++) PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_13));
            h.assertTrue(PocketJukeboxItem.contents(jukebox).size() == PocketJukeboxItem.CAPACITY, "20 discs");
            h.assertTrue(!PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_13)), "full at 20");
            var contents = PocketJukeboxItem.contents(jukebox);
            h.assertTrue(PocketJukeboxItem.disc(contents, 0).is(Items.MUSIC_DISC_CAT) && PocketJukeboxItem.disc(contents, 1).is(MusicRegistry.CUSTOM_DISC),
                    "order: first in = 0");
            h.assertTrue(jukebox.getItem().getTooltipImage(jukebox).orElse(null) instanceof com.simplemusic.pocket.PocketContents, "bundle tooltip data");
            h.assertTrue(PocketJukeboxItem.removeOne(jukebox).is(Items.MUSIC_DISC_13) && PocketJukeboxItem.contents(jukebox).size() == 19, "takes the newest out");
            // inventory clicks like a bundle
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            ItemStack small = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(5));
            net.minecraft.world.SimpleContainer box = new net.minecraft.world.SimpleContainer(1);
            box.setItem(0, small);
            net.minecraft.world.inventory.Slot slot = new net.minecraft.world.inventory.Slot(box, 0, 0, 0);
            ItemStack[] cursor = {new ItemStack(Items.MUSIC_DISC_WAIT)};
            net.minecraft.world.entity.SlotAccess carried = net.minecraft.world.entity.SlotAccess.of(() -> cursor[0], v -> cursor[0] = v);
            h.assertTrue(small.getItem().overrideOtherStackedOnMe(small, cursor[0], slot, net.minecraft.world.inventory.ClickAction.PRIMARY, player, carried)
                    && cursor[0].isEmpty() && PocketJukeboxItem.contents(small).size() == 1, "left click with a disc puts it in");
            ItemStack dirt = new ItemStack(Items.DIRT);
            h.assertTrue(!small.getItem().overrideOtherStackedOnMe(small, dirt, slot, net.minecraft.world.inventory.ClickAction.PRIMARY, player, carried),
                    "other items swap as usual");
            h.assertTrue(small.getItem().overrideOtherStackedOnMe(small, ItemStack.EMPTY, slot, net.minecraft.world.inventory.ClickAction.SECONDARY, player, carried)
                    && cursor[0].is(Items.MUSIC_DISC_WAIT) && PocketJukeboxItem.contents(small).isEmpty(), "right click with an empty hand takes it out");
            box.setItem(0, new ItemStack(Items.MUSIC_DISC_STAL));
            h.assertTrue(small.getItem().overrideStackedOnOther(small, slot, net.minecraft.world.inventory.ClickAction.PRIMARY, player)
                    && box.getItem(0).isEmpty() && PocketJukeboxItem.contents(small).size() == 1, "jukebox on the cursor picks a disc up");
            h.succeed();
        });
        ALL.put("pocket_play_pause", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            List<CustomPacketPayload> sent = capture(player);
            ItemStack jukebox = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(0));
            PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_13));
            PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_CAT));
            player.setItemInHand(InteractionHand.OFF_HAND, jukebox.copy());
            h.assertTrue(!player.getOffhandItem().use(h.getLevel(), player, InteractionHand.OFF_HAND).consumesAction()
                    && PocketPlayback.playing(player) == -1, "the off hand only holds it");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, jukebox);
            h.assertTrue(jukebox.use(h.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "use");
            h.assertTrue(PocketPlayback.playing(player) == 0, "first disc in plays first");
            h.assertTrue(sent.stream().anyMatch(p -> p instanceof TrackPlayPayload t && t.carried() && t.isSong()
                    && t.sha256().equals("minecraft:music_disc.13")), "carried play to the carrier: " + sent);
            h.assertTrue(PocketJukeboxItem.contents(jukebox).getSelectedItemIndex() == 1, "open: the playing disc is selected");
            long[] paused = {0};
            h.runAfterDelay(10, () -> {
                ItemStack held = player.getMainHandItem();
                held.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                paused[0] = PocketPlayback.state(held).ticks();
                h.assertTrue(PocketPlayback.playing(player) == -1 && paused[0] >= 8, "paused at " + paused[0]);
                h.assertTrue(PocketJukeboxItem.contents(held).getSelectedItemIndex() == -1, "closed while paused");
                h.assertTrue(sent.get(sent.size() - 1) instanceof TrackPlayPayload t && t.carried() && t.isStop(), "stop sent");
                held.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                h.assertTrue(PocketPlayback.elapsed(player) >= paused[0], "goes on where it paused");
                h.assertTrue(sent.get(sent.size() - 1) instanceof TrackPlayPayload t && t.offsetTicks() == paused[0], "with the offset");
                // a second jukebox pauses the first
                ItemStack other = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(2));
                PocketJukeboxItem.insert(other, new ItemStack(Items.MUSIC_DISC_WAIT));
                player.getInventory().setItem(5, other);
                PocketPlayback.toggle(player, player.getInventory().getItem(5));
                h.assertTrue(PocketPlayback.active(player, player.getInventory().getItem(5)) && !PocketPlayback.active(player, held)
                        && PocketPlayback.state(held).ticks() >= paused[0], "second plays, first paused");
                PocketPlayback.toggle(player, player.getInventory().getItem(5));
                // next disc at the end of one, after the last the first again
                int length = net.minecraft.world.item.JukeboxSong.fromStack(new ItemStack(Items.MUSIC_DISC_13)).orElseThrow().value().lengthInTicks();
                held.set(MusicComponents.POCKET, new com.simplemusic.pocket.PocketState(PocketPlayback.state(held).id(), 0, length - 2));
                held.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            });
            h.runAfterDelay(16, () -> {
                ItemStack held = player.getMainHandItem();
                h.assertTrue(PocketPlayback.playing(player) == 1, "next disc: " + PocketPlayback.playing(player));
                h.assertTrue(PocketJukeboxItem.contents(held).getSelectedItemIndex() == 0, "selection follows");
                held.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                int length = net.minecraft.world.item.JukeboxSong.fromStack(new ItemStack(Items.MUSIC_DISC_CAT)).orElseThrow().value().lengthInTicks();
                held.set(MusicComponents.POCKET, new com.simplemusic.pocket.PocketState(PocketPlayback.state(held).id(), 1, length - 2));
                held.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            });
            h.runAfterDelay(22, () -> {
                h.assertTrue(PocketPlayback.playing(player) == 0, "loops to the first: " + PocketPlayback.playing(player));
                player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                h.succeed();
            });
        });
        MAX_TICKS.put("pocket_play_pause", 60);
        ALL.put("pocket_stops", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            List<CustomPacketPayload> sent = capture(player);
            ItemStack jukebox = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(7));
            PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_CHIRP));
            player.setItemInHand(InteractionHand.MAIN_HAND, jukebox);
            h.assertTrue(jukebox.use(h.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction() && PocketPlayback.playing(player) == 0, "plays");
            h.runAfterDelay(4, () -> {
                PocketJukeboxItem.removeOne(player.getMainHandItem());
            });
            h.runAfterDelay(6, () -> {
                ItemStack held = player.getMainHandItem();
                h.assertTrue(PocketPlayback.playing(player) == -1 && PocketPlayback.state(held).ticks() == 0, "taking the playing disc out stops");
                h.assertTrue(sent.get(sent.size() - 1) instanceof TrackPlayPayload t && t.carried() && t.isStop(), "stop sent");
                PocketJukeboxItem.insert(held, new ItemStack(Items.MUSIC_DISC_CHIRP));
                held.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                h.assertTrue(PocketPlayback.playing(player) == 0, "plays again");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
            h.runAfterDelay(9, () -> {
                h.assertTrue(PocketPlayback.playing(player) == -1, "gone from the inventory: stops");
                h.assertTrue(sent.get(sent.size() - 1) instanceof TrackPlayPayload t && t.carried() && t.isStop(), "stop sent");
                h.succeed();
            });
        });
        MAX_TICKS.put("pocket_stops", 40);
        ALL.put("pocket_place_break", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            player.getAbilities().instabuild = false;
            int red = net.minecraft.world.item.DyeColor.RED.getId() + 1;
            net.minecraft.world.item.Item item = MusicRegistry.POCKET_JUKEBOXES.get(red);
            net.minecraft.world.level.block.Block block = MusicRegistry.POCKET_JUKEBOX_BLOCKS.get(red);
            h.assertTrue(MusicRegistry.POCKET_JUKEBOX_BLOCKS.size() == 17 && block.asItem() == item
                    && item instanceof net.minecraft.world.item.BlockItem b && b.getBlock() == block, "every colour places its own block");
            ItemStack stack = new ItemStack(item);
            PocketJukeboxItem.insert(stack, new ItemStack(Items.MUSIC_DISC_13));
            PocketJukeboxItem.insert(stack, new ItemStack(Items.MUSIC_DISC_CAT));
            stack.set(MusicComponents.POCKET, new PocketState(7L, 1, 40));
            BlockPos floor = new BlockPos(2, 1, 2);
            h.setBlock(floor, Blocks.STONE);
            BlockPos abs = h.absolutePos(floor);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
            h.assertTrue(!stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                    && h.getBlockState(floor.above()).isAir(), "without sneaking it does not place (use plays)");
            player.setShiftKeyDown(true);
            h.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "sneak + use places");
            player.setShiftKeyDown(false);
            h.assertTrue(h.getBlockState(floor.above()).is(block) && player.getMainHandItem().isEmpty(), "placed: " + h.getBlockState(floor.above()));
            PocketJukeboxBlockEntity be = h.getBlockEntity(floor.above(), PocketJukeboxBlockEntity.class);
            h.assertTrue(be.contents().size() == 2 && be.state().fromEnd() == 1 && be.state().ticks() == 40 && !be.playing(),
                    "discs and position went into the block: " + be.state());
            BlockPos placed = h.absolutePos(floor.above());
            h.getLevel().destroyBlock(placed, true, player);
            List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(placed).inflate(1.5));
            ItemStack back = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(item)).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue(PocketJukeboxItem.contents(back).size() == 2 && PocketPlayback.state(back).fromEnd() == 1
                    && PocketPlayback.state(back).ticks() == 40, "broken: the item comes back with discs and position: " + back);
            h.assertTrue(drops.size() == 1, "the discs stay inside: " + drops);
            drops.forEach(ItemEntity::discard);
            // creative breaking drops a jukebox with discs like a shulker box
            BlockPos second = new BlockPos(5, 1, 5);
            h.setBlock(second, block.defaultBlockState());
            h.getBlockEntity(second, PocketJukeboxBlockEntity.class).applyComponentsFromItemStack(back);
            player.getAbilities().instabuild = true;
            block.playerWillDestroy(h.getLevel(), h.absolutePos(second), h.getBlockState(second), player);
            player.getAbilities().instabuild = false;
            h.setBlock(second, Blocks.AIR);
            List<ItemEntity> creative = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(second)).inflate(1.5));
            h.assertTrue(creative.size() == 1 && PocketJukeboxItem.contents(creative.get(0).getItem()).size() == 2, "creative: drops with its discs");
            creative.forEach(ItemEntity::discard);
            h.succeed();
        });
        ALL.put("pocket_place_while_playing", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            player.getAbilities().instabuild = false;
            List<CustomPacketPayload> sent = capture(player);
            BlockPos floor = new BlockPos(3, 1, 3);
            h.setBlock(floor, Blocks.STONE);
            BlockPos abs = h.absolutePos(floor);
            player.setPos(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 2.5);
            ItemStack jukebox = new ItemStack(MusicRegistry.POCKET_JUKEBOXES.get(0));
            PocketJukeboxItem.insert(jukebox, new ItemStack(Items.MUSIC_DISC_13));
            player.setItemInHand(InteractionHand.MAIN_HAND, jukebox);
            h.assertTrue(jukebox.use(h.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction() && PocketPlayback.playing(player) == 0, "plays in the hand");
            h.runAfterDelay(6, () -> {
                ItemStack held = player.getMainHandItem();
                player.setShiftKeyDown(true);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
                h.assertTrue(held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "placed");
                player.setShiftKeyDown(false);
                PocketJukeboxBlockEntity be = h.getBlockEntity(floor.above(), PocketJukeboxBlockEntity.class);
                BlockPos at = h.absolutePos(floor.above());
                h.assertTrue(PocketPlayback.playing(player) == -1 && be.playing() && DiscPlayback.playing(h.getLevel(), at), "goes on playing on the block");
                h.assertTrue(DiscPlayback.elapsed(h.getLevel(), at) >= 5, "at the same position: " + DiscPlayback.elapsed(h.getLevel(), at));
                h.assertTrue(sent.stream().anyMatch(p -> p instanceof TrackPlayPayload t && t.carried() && t.isStop()), "the hand sound stops");
                h.assertTrue(sent.stream().anyMatch(p -> p instanceof TrackPlayPayload t && !t.carried() && t.isSong() && t.pos().equals(at)
                        && t.offsetTicks() >= 5), "positional play mid-song to the player nearby: " + sent);
                h.setBlock(floor.above(), Blocks.AIR);
                h.assertTrue(!DiscPlayback.playing(h.getLevel(), at), "gone: stops");
                h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(1.5)).forEach(ItemEntity::discard);
                h.succeed();
            });
        });
        MAX_TICKS.put("pocket_place_while_playing", 40);
        ALL.put("pocket_block_play", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            player.getAbilities().instabuild = false;
            List<CustomPacketPayload> sent = capture(player);
            BlockPos pos = new BlockPos(3, 1, 3);
            h.setBlock(pos, MusicRegistry.POCKET_JUKEBOX_BLOCKS.get(0).defaultBlockState());
            BlockPos abs = h.absolutePos(pos);
            player.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5);
            PocketJukeboxBlockEntity be = h.getBlockEntity(pos, PocketJukeboxBlockEntity.class);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
            var level = h.getLevel();
            h.assertTrue(!h.getBlockState(pos).useWithoutItem(level, player, hit).consumesAction() && !be.playing(), "empty: nothing to play");
            ItemStack thirteen = new ItemStack(Items.MUSIC_DISC_13);
            player.setItemInHand(InteractionHand.MAIN_HAND, thirteen);
            h.assertTrue(h.getBlockState(pos).useItemOn(thirteen, level, player, InteractionHand.MAIN_HAND, hit).consumesAction() && thirteen.isEmpty(), "disc goes in");
            h.assertTrue(be.playing() && DiscPlayback.playing(level, abs), "putting a disc in starts it");
            h.assertTrue(sent.stream().anyMatch(p -> p instanceof TrackPlayPayload t && !t.carried() && t.isSong() && t.pos().equals(abs)
                    && t.sha256().equals("minecraft:music_disc.13")), "positional play: " + sent);
            int value13 = net.minecraft.world.item.JukeboxSong.fromStack(new ItemStack(Items.MUSIC_DISC_13)).orElseThrow().value().comparatorOutput();
            h.assertTrue(h.getBlockState(pos).getSignal(level, abs, Direction.NORTH) == 15
                    && h.getBlockState(pos).getAnalogOutputSignal(level, abs, Direction.NORTH) == value13, "redstone 15, comparator = the disc's value");
            ItemStack cat = new ItemStack(Items.MUSIC_DISC_CAT);
            h.getBlockState(pos).useItemOn(cat, level, player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(be.contents().size() == 2 && be.state().fromEnd() == 0 && be.playing(), "a second disc waits its turn");
            h.assertTrue(!h.getBlockState(pos).useItemOn(new ItemStack(Items.DIRT), level, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                    && be.contents().size() == 2, "no dirt");
            int[] paused = {0};
            h.runAfterDelay(10, () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                h.getBlockState(pos).useWithoutItem(level, player, hit);
                paused[0] = be.state().ticks();
                h.assertTrue(!be.playing() && !DiscPlayback.playing(level, abs) && paused[0] >= 8, "paused at " + paused[0]);
                h.assertTrue(h.getBlockState(pos).getSignal(level, abs, Direction.NORTH) == 0, "no signal while paused");
                h.assertTrue(sent.get(sent.size() - 1) instanceof TrackPlayPayload t && !t.carried() && t.isStop() && t.pos().equals(abs), "stop sent");
                h.getBlockState(pos).useWithoutItem(level, player, hit);
                h.assertTrue(be.playing() && sent.get(sent.size() - 1) instanceof TrackPlayPayload t && t.offsetTicks() == paused[0], "goes on where it paused");
                int length = net.minecraft.world.item.JukeboxSong.fromStack(new ItemStack(Items.MUSIC_DISC_13)).orElseThrow().value().lengthInTicks();
                be.restartAt(0, length - 2);
            });
            h.runAfterDelay(16, () -> {
                h.assertTrue(be.playing() && be.state().fromEnd() == 1, "next disc: " + be.state());
                int length = net.minecraft.world.item.JukeboxSong.fromStack(new ItemStack(Items.MUSIC_DISC_CAT)).orElseThrow().value().lengthInTicks();
                be.restartAt(1, length - 2);
            });
            h.runAfterDelay(22, () -> {
                h.assertTrue(be.playing() && be.state().fromEnd() == 0, "after the last the first again: " + be.state());
                player.setShiftKeyDown(true);
                h.assertTrue(h.getBlockState(pos).useWithoutItem(level, player, hit).consumesAction()
                        && be.contents().size() == 1 && be.playing(), "sneak: the newest disc (not playing) comes out, music goes on");
                h.getBlockState(pos).useWithoutItem(level, player, hit);
                player.setShiftKeyDown(false);
                h.assertTrue(be.isEmpty() && !be.playing() && !DiscPlayback.playing(level, abs), "the playing disc out: stops");
                List<ItemEntity> out = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.5));
                h.assertTrue(out.size() == 2 && out.stream().anyMatch(e -> e.getItem().is(Items.MUSIC_DISC_CAT))
                        && out.stream().anyMatch(e -> e.getItem().is(Items.MUSIC_DISC_13)), "both thrown out: " + out);
                out.forEach(ItemEntity::discard);
                h.setBlock(pos, Blocks.AIR);
                h.succeed();
            });
        });
        MAX_TICKS.put("pocket_block_play", 60);
        ALL.put("press_sheet_stays", h -> {
            BlockPos pos = new BlockPos(2, 1, 2);
            h.setBlock(pos, MusicRegistry.DISC_PRESS.defaultBlockState());
            DiscPressBlockEntity press = h.getBlockEntity(pos, DiscPressBlockEntity.class);
            press.setItem(DiscPressBlockEntity.SHEET, sheet(TUNE));
            press.setItem(DiscPressBlockEntity.BLANK, new ItemStack(MusicRegistry.BLANK_DISC, 2));
            h.runAfterDelay(DiscPress.PRESS_TICKS + 5, () -> {
                ItemStack out = press.getItem(DiscPressBlockEntity.OUTPUT);
                var music = CustomDiscItem.composition(out);
                h.assertTrue(out.is(MusicRegistry.CUSTOM_DISC) && music != null && music.code().equals(TUNE) && out.get(MusicComponents.TRACK) == null,
                        "composition pressed: " + out);
                h.assertTrue(SheetMusicItem.content(press.getItem(DiscPressBlockEntity.SHEET)).code().equals(TUNE), "the sheet stays");
                h.assertTrue(press.getItem(DiscPressBlockEntity.BLANK).getCount() == 1 && press.pressed() == 1, "one blank used");
                h.assertTrue(OrchestrionBlockEntity.playable(out) && JukeboxDiscs.isDisc(out), "plays in the Orchestrion and the jukebox");
                // the name is "Disc: <title>"; Forge's test server has no lang file, so read the argument, not the text
                h.assertTrue(out.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                        && Arrays.asList(t.getArgs()).contains("Test"), "named after the sheet: " + out.getHoverName());
            });
            h.runAfterDelay(2 * DiscPress.PRESS_TICKS + 10, () -> {
                h.assertTrue(press.pressed() == 1 && press.progress() == 0, "a full output waits");
                press.removeItemNoUpdate(DiscPressBlockEntity.OUTPUT);
            });
            h.runAfterDelay(3 * DiscPress.PRESS_TICKS + 15, () -> {
                h.assertTrue(press.pressed() == 2 && press.getItem(DiscPressBlockEntity.BLANK).isEmpty()
                        && !press.getItem(DiscPressBlockEntity.SHEET).isEmpty(), "second disc, sheet still there");
                h.succeed();
            });
        });
        MAX_TICKS.put("press_sheet_stays", 200);
        ALL.put("press_rules", h -> {
            ItemStack blank = new ItemStack(MusicRegistry.BLANK_DISC);
            ItemStack upload = new ItemStack(MusicRegistry.SHEET_MUSIC);
            TrackRef ref = new TrackRef("b".repeat(64), "Up", 7, "Ann");
            upload.set(MusicComponents.TRACK, ref);
            upload.set(MusicComponents.SHEET, new SheetContent("", "Up", "Ann", true));
            ItemStack disc = DiscPress.result(upload, blank);
            h.assertTrue(ref.equals(disc.get(MusicComponents.TRACK)) && CustomDiscItem.composition(disc) == null, "upload sheet -> track disc");
            h.assertTrue(DiscPress.result(sheet("harp: zz"), blank).isEmpty(), "a code with problems is not pressed");
            h.assertTrue(DiscPress.result(new ItemStack(MusicRegistry.SHEET_MUSIC), blank).isEmpty(), "an empty sheet is not pressed");
            h.assertTrue(DiscPress.result(sheet(TUNE), new ItemStack(Items.MUSIC_DISC_CAT)).isEmpty(), "only blank discs");
            h.assertTrue(DiscPress.result(sheet(TUNE), blank).getCount() == 1 && blank.getCount() == 1, "pure: inputs untouched");
            // an uploaded track sheet also plays straight in the Orchestrion, like a track disc
            h.assertTrue(OrchestrionBlockEntity.playable(upload) && OrchestrionBlockEntity.trackOf(upload) == ref, "upload sheet in the Orchestrion");
            // hopper faces like the furnace
            BlockPos pos = new BlockPos(2, 1, 2);
            h.setBlock(pos, MusicRegistry.DISC_PRESS.defaultBlockState());
            DiscPressBlockEntity press = h.getBlockEntity(pos, DiscPressBlockEntity.class);
            h.assertTrue(press.canPlaceItemThroughFace(DiscPressBlockEntity.SHEET, sheet(TUNE), Direction.UP)
                    && !press.canPlaceItemThroughFace(DiscPressBlockEntity.SHEET, blank, Direction.UP), "sheet from above");
            h.assertTrue(press.canPlaceItemThroughFace(DiscPressBlockEntity.BLANK, blank, Direction.NORTH)
                    && !press.canPlaceItemThroughFace(DiscPressBlockEntity.BLANK, sheet(TUNE), Direction.NORTH), "blank from the side");
            h.assertTrue(press.canTakeItemThroughFace(DiscPressBlockEntity.OUTPUT, disc, Direction.DOWN)
                    && !press.canTakeItemThroughFace(DiscPressBlockEntity.SHEET, sheet(TUNE), Direction.DOWN), "only the disc out of the bottom");
            press.setItem(DiscPressBlockEntity.SHEET, sheet(TUNE));
            h.assertTrue(!press.canPlaceItemThroughFace(DiscPressBlockEntity.SHEET, sheet(TUNE), Direction.UP), "one sheet only");
            ItemStack craftedPress = craft(h, net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(
                    ItemStack.EMPTY, new ItemStack(Items.PISTON), ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.SMOOTH_STONE), new ItemStack(Items.SMOOTH_STONE), new ItemStack(Items.SMOOTH_STONE))));
            h.assertTrue(craftedPress.is(MusicRegistry.DISC_PRESS_ITEM), "recipe: piston over two iron ingots over smooth stone: " + craftedPress);
            h.succeed();
        });
        ALL.put("cartography_copies_sheet", h -> {
            BlockPos pos = new BlockPos(4, 1, 4);
            h.setBlock(pos, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            var menu = new net.minecraft.world.inventory.CartographyTableMenu(1, player.getInventory(),
                    net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(), h.absolutePos(pos)));
            ItemStack draft = new ItemStack(MusicRegistry.SHEET_MUSIC);
            draft.set(MusicComponents.SHEET, new SheetContent(TUNE, "Draft", "", false));
            h.assertTrue(menu.getSlot(0).mayPlace(sheet(TUNE)) && !menu.getSlot(0).mayPlace(draft), "signed sheets on top");
            h.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(MusicRegistry.SHEET_MUSIC)), "sheets below");
            menu.getSlot(0).set(sheet(TUNE));
            menu.getSlot(1).set(new ItemStack(MusicRegistry.SHEET_MUSIC, 2));
            ItemStack copy = menu.getSlot(2).getItem();
            SheetContent c = SheetMusicItem.content(copy);
            h.assertTrue(copy.is(MusicRegistry.SHEET_MUSIC) && copy.getCount() == 1 && c.code().equals(TUNE) && c.title().equals("Test")
                    && !c.signed() && c.author().isEmpty(), "editable copy like the blueprint: " + c);
            menu.getSlot(2).onTake(player, copy);
            h.assertTrue(menu.getSlot(1).getItem().getCount() == 1 && SheetMusicItem.content(menu.getSlot(0).getItem()).signed(),
                    "only the lower sheet is used, the original stays");
            ItemStack upload = new ItemStack(MusicRegistry.SHEET_MUSIC);
            TrackRef ref = new TrackRef("c".repeat(64), "Up", 7, "Ann");
            upload.set(MusicComponents.TRACK, ref);
            upload.set(MusicComponents.SHEET, new SheetContent("", "Up", "Ann", true));
            menu.getSlot(0).set(upload);
            ItemStack exact = menu.getSlot(2).getItem();
            h.assertTrue(ref.equals(SheetMusicItem.track(exact)) && SheetMusicItem.content(exact).signed(), "upload sheet: exact copy");
            menu.getSlot(1).set(draft);
            h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "a written sheet below takes no copy");
            menu.getSlot(0).set(ItemStack.EMPTY);
            menu.getSlot(1).set(ItemStack.EMPTY);
            h.succeed();
        });
        ALL.put("guide_book", com.simplemusic.guide.MusicGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplemusic.discover.MusicDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplemusic.discover.MusicDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplemusic.discover.MusicDiscover::firstPickupTips);
    }

    static ItemStack craft(GameTestHelper h, net.minecraft.world.item.crafting.CraftingInput input) {
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel())
                .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private MusicTests() {}
}
