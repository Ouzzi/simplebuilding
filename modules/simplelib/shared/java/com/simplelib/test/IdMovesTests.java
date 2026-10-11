package com.simplelib.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplelib.SimpleLib;
import com.simplelib.api.IdMoves;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibComponents;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GameTests of {@link IdMoves} (split plan stage 3a), with SimpleLib's own content and the reserved namespace
 * {@code simplelibtest}: an id that never existed there stands for an id a mod gave up, moved to a SimpleLib id. The
 * moves are registered at start ({@link #registerTestMoves}, from {@code SimpleLib.init}) because Forge accepts its
 * registry aliases only while the registries are open; the namespace never exists, so they cost nothing.
 */
public final class IdMovesTests {
    static final String OLD = "simplelibtest";
    static final Identifier OLD_CRUCIBLE = Identifier.fromNamespaceAndPath(OLD, "old_iron_crucible");
    static final Identifier OLD_CRUCIBLE_BE = Identifier.fromNamespaceAndPath(OLD, "old_crucible");
    static final Identifier OLD_WARM = Identifier.fromNamespaceAndPath(OLD, "old_warm");
    static final Identifier OLD_RECIPE = Identifier.fromNamespaceAndPath(OLD, "old_copper_barrel");
    static final Identifier OLD_ADVANCEMENT = Identifier.fromNamespaceAndPath(OLD, "old_story_root");
    static final Identifier NEW_RECIPE = SimpleLib.id("copper_barrel");
    static final Identifier NEW_ADVANCEMENT = Identifier.withDefaultNamespace("story/root");

    private IdMovesTests() {}

    /** The test moves; idempotent. */
    public static void registerTestMoves() {
        IdMoves.move(Registries.ITEM, OLD_CRUCIBLE, SimpleLib.id("iron_crucible"));
        IdMoves.move(Registries.BLOCK, OLD_CRUCIBLE, SimpleLib.id("iron_crucible"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, OLD_CRUCIBLE_BE, SimpleLib.id("crucible"));
        IdMoves.move(Registries.DATA_COMPONENT_TYPE, OLD_WARM, SimpleLib.id("warm"));
        IdMoves.move(Registries.RECIPE, OLD_RECIPE, NEW_RECIPE);
        IdMoves.move(Registries.ADVANCEMENT, OLD_ADVANCEMENT, NEW_ADVANCEMENT);
    }

    /**
     * Item, block, block entity and component ids under the old name resolve to the moved entries - in the registry
     * itself and along the save paths (stack, palette state, block entity) - and are written back under the new id.
     * Control: an unknown id of the same namespace stays unknown.
     */
    public static void registries(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        var item = LibBlocks.IRON_CRUCIBLE.asItem();
        if (BuiltInRegistries.ITEM.getValue(OLD_CRUCIBLE) != item) problems.add("ITEM.getValue(old) = " + BuiltInRegistries.ITEM.getValue(OLD_CRUCIBLE));
        if (BuiltInRegistries.ITEM.get(OLD_CRUCIBLE).map(Holder::value).orElse(null) != item) problems.add("ITEM.get(old) has no holder");
        if (!BuiltInRegistries.ITEM.containsKey(OLD_CRUCIBLE)) problems.add("ITEM.containsKey(old) is false");
        if (BuiltInRegistries.BLOCK.getValue(OLD_CRUCIBLE) != LibBlocks.IRON_CRUCIBLE) problems.add("BLOCK.getValue(old) = " + BuiltInRegistries.BLOCK.getValue(OLD_CRUCIBLE));
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(OLD_CRUCIBLE_BE) != LibBlockEntities.CRUCIBLE) problems.add("BLOCK_ENTITY_TYPE.getValue(old) wrong");
        if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(OLD_WARM) != LibComponents.WARM) problems.add("DATA_COMPONENT_TYPE.getValue(old) wrong");

        RegistryOps<Tag> ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack warm = new ItemStack(item);
        warm.set(LibComponents.WARM, new com.simplelib.warm.Warm(h.getLevel().getGameTime() + 1200L));
        CompoundTag saved = (CompoundTag) ItemStack.CODEC.encodeStart(ops, warm).getOrThrow();
        CompoundTag old = saved.copy();
        old.putString("id", OLD_CRUCIBLE.toString());
        CompoundTag components = old.getCompoundOrEmpty("components");
        Tag warmTag = components.get(SimpleLib.id("warm").toString());
        if (warmTag == null) {
            problems.add("a warm stack saves no simplelib:warm component: " + saved);
        } else {
            components.remove(SimpleLib.id("warm").toString());
            components.put(OLD_WARM.toString(), warmTag);
            old.put("components", components);
        }
        ItemStack loaded = ItemStack.CODEC.parse(ops, old).result().orElse(ItemStack.EMPTY);
        if (!ItemStack.isSameItemSameComponents(loaded, warm)) problems.add("stack saved as " + old + " loads as " + loaded + " " + loaded.getComponentsPatch());
        Tag resaved = ItemStack.CODEC.encodeStart(ops, loaded).result().orElse(null);
        if (resaved == null || resaved.toString().contains(OLD + ":")) problems.add("stack is saved back as " + resaved);

        CompoundTag stateTag = NbtUtils.writeBlockState(LibBlocks.IRON_CRUCIBLE.defaultBlockState());
        stateTag.putString("Name", OLD_CRUCIBLE.toString());
        BlockState state = NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), stateTag);
        if (!state.is(LibBlocks.IRON_CRUCIBLE)) problems.add("palette state " + stateTag + " loads as " + state);

        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        BlockState crucibleState = LibBlocks.IRON_CRUCIBLE.defaultBlockState();
        CompoundTag beTag = new CrucibleBlockEntity(pos, crucibleState).saveWithFullMetadata(h.getLevel().registryAccess());
        beTag.putString("id", OLD_CRUCIBLE_BE.toString());
        BlockEntity be = BlockEntity.loadStatic(pos, crucibleState, beTag, h.getLevel().registryAccess());
        if (!(be instanceof CrucibleBlockEntity)) {
            problems.add("block entity saved as " + OLD_CRUCIBLE_BE + " loads as " + be);
        } else if (!be.saveWithFullMetadata(h.getLevel().registryAccess()).getStringOr("id", "").equals(SimpleLib.id("crucible").toString())) {
            problems.add("block entity is saved back as " + be.saveWithFullMetadata(h.getLevel().registryAccess()).getStringOr("id", ""));
        }

        Identifier unknown = Identifier.fromNamespaceAndPath(OLD, "never_moved");
        if (BuiltInRegistries.ITEM.get(unknown).isPresent() || BuiltInRegistries.BLOCK.containsKey(unknown)) problems.add(unknown + " resolves although it never moved");
        h.assertTrue(problems.isEmpty(), "id moves: " + problems);
        h.succeed();
    }

    /**
     * The recipe book keeps a moved recipe (old key replaced, unknown keys still dropped by vanilla), and advancement
     * progress saved under a moved id lands on the moved advancement when a player's progress file is read.
     */
    public static void recipeBookAndAdvancements(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ResourceKey<Recipe<?>> oldKey = ResourceKey.create(Registries.RECIPE, OLD_RECIPE);
        ResourceKey<Recipe<?>> newKey = ResourceKey.create(Registries.RECIPE, NEW_RECIPE);
        ResourceKey<Recipe<?>> unknownKey = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(OLD, "never_moved"));
        h.assertTrue(server.getRecipeManager().byKey(newKey).isPresent(), "test needs recipe " + NEW_RECIPE);
        ServerRecipeBook book = new ServerRecipeBook((id, output) -> {});
        book.loadUntrusted(new ServerRecipeBook.Packed(new RecipeBookSettings(), List.of(oldKey, unknownKey), List.of(oldKey)),
                key -> server.getRecipeManager().byKey(key).isPresent());
        h.assertTrue(book.contains(newKey), "the recipe book lost the moved recipe " + OLD_RECIPE);
        h.assertTrue(!book.contains(unknownKey) && !book.contains(oldKey), "the recipe book kept an unknown key");
        h.assertTrue(book.pack().highlight().contains(newKey), "the moved recipe lost its highlight");

        AdvancementHolder target = server.getAdvancements().get(NEW_ADVANCEMENT);
        h.assertTrue(target != null, "test needs advancement " + NEW_ADVANCEMENT);
        h.assertTrue(server.getAdvancements().get(OLD_ADVANCEMENT) == target, "ServerAdvancementManager.get(old) is not the moved advancement");
        h.assertTrue(server.getAdvancements().get(Identifier.fromNamespaceAndPath(OLD, "never_moved")) == null, "an unknown advancement resolves");

        Path file = server.getServerDirectory().resolve("simplelib-idmoves-test").resolve("advancements.json");
        JsonObject progress = new JsonObject();
        JsonObject entry = new JsonObject();
        JsonObject criteria = new JsonObject();
        for (String criterion : target.value().criteria().keySet()) criteria.addProperty(criterion, "2026-10-11 12:00:00 +0000");
        entry.add("criteria", criteria);
        entry.addProperty("done", true);
        progress.add(OLD_ADVANCEMENT.toString(), entry);
        progress.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        ServerPlayer player = detachedPlayer(h);
        PlayerAdvancements advancements;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, progress.toString(), StandardCharsets.UTF_8);
            advancements = new PlayerAdvancements(server.getFixerUpper(), server.getPlayerList(), server.getAdvancements(), file, player);
        } catch (java.io.IOException e) {
            throw h.assertionException("cannot write " + file + ": " + e);
        }
        try {
            h.assertTrue(advancements.getOrStartProgress(target).isDone(), "progress saved under " + OLD_ADVANCEMENT + " did not reach " + NEW_ADVANCEMENT);
            advancements.save();
            JsonObject resaved = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            h.assertTrue(resaved.has(NEW_ADVANCEMENT.toString()) && !resaved.has(OLD_ADVANCEMENT.toString()),
                    "progress is saved back as " + resaved.keySet());
        } catch (java.io.IOException e) {
            throw h.assertionException("cannot read " + file + ": " + e);
        } finally {
            advancements.stopListening();
        }
        h.succeed();
    }

    /** Moves refuse loops and conflicting targets; namespace moves too. Uses a registry key no registry has. */
    public static void refuseLoops(GameTestHelper h) {
        ResourceKey<net.minecraft.core.Registry<Object>> scratch = ResourceKey.createRegistryKey(SimpleLib.id("id_moves_test_scratch"));
        Identifier a = Identifier.fromNamespaceAndPath(OLD, "loop_a");
        Identifier b = Identifier.fromNamespaceAndPath(OLD, "loop_b");
        Identifier c = Identifier.fromNamespaceAndPath(OLD, "loop_c");
        IdMoves.move(scratch, a, b);
        IdMoves.move(scratch, b, c);
        IdMoves.move(scratch, a, b); // same move again: fine
        h.assertTrue(throwsOn(() -> IdMoves.move(scratch, c, a)), "a -> b -> c -> a was accepted");
        h.assertTrue(throwsOn(() -> IdMoves.move(scratch, a, c)), "a second target for a was accepted");
        h.assertTrue(throwsOn(() -> IdMoves.move(scratch, a, a)), "a move onto itself was accepted");
        h.assertValueEqual(IdMoves.target(scratch, a), b, "target of a");
        IdMoves.moveNamespace("simplelibtest_ns_a", "simplelibtest_ns_b");
        h.assertTrue(throwsOn(() -> IdMoves.moveNamespace("simplelibtest_ns_b", "simplelibtest_ns_a")), "a namespace loop was accepted");
        h.assertTrue(throwsOn(() -> IdMoves.moveNamespace("simplelibtest_ns_a", "simplelibtest_ns_c")), "a second namespace target was accepted");
        h.assertValueEqual(IdMoves.target(scratch, Identifier.fromNamespaceAndPath("simplelibtest_ns_a", "x")),
                Identifier.fromNamespaceAndPath("simplelibtest_ns_b", "x"), "namespace move");
        h.succeed();
    }

    /** Config takeover: listed keys of the old file go to a missing new file; the old file stays; an existing new file wins. */
    public static void adoptConfig(GameTestHelper h) {
        Path dir = h.getLevel().getServer().getServerDirectory().resolve("simplelib-idmoves-test").resolve("config");
        try {
            Files.createDirectories(dir);
            Files.deleteIfExists(dir.resolve("new.json"));
            String oldText = "{\"a\": 1, \"b\": {\"x\": true}, \"c\": \"three\"}";
            Files.writeString(dir.resolve("old.json"), oldText);
            h.assertTrue(IdMoves.adoptConfig(dir, "new.json", "old.json", Set.of("a", "c", "missing")), "nothing was taken over");
            JsonObject taken = JsonParser.parseString(Files.readString(dir.resolve("new.json"))).getAsJsonObject();
            h.assertTrue(taken.keySet().equals(Set.of("a", "c")) && taken.get("a").getAsInt() == 1 && taken.get("c").getAsString().equals("three"),
                    "taken over: " + taken);
            h.assertValueEqual(Files.readString(dir.resolve("old.json")), oldText, "old config");
            Files.writeString(dir.resolve("new.json"), "{\"a\": 5}");
            h.assertTrue(!IdMoves.adoptConfig(dir, "new.json", "old.json", Set.of("a")), "an existing new config was replaced");
            h.assertValueEqual(Files.readString(dir.resolve("new.json")), "{\"a\": 5}", "existing new config");
            h.assertTrue(!IdMoves.adoptConfig(dir, "other.json", "absent.json", Set.of("a")), "a missing old config was taken over");
            h.assertTrue(!IdMoves.adoptConfig(dir, "other.json", "old.json", Set.of("missing")), "a file without the keys was written");
            h.assertTrue(!Files.exists(dir.resolve("other.json")), "other.json was written");
        } catch (java.io.IOException e) {
            throw h.assertionException("config takeover test: " + e);
        }
        h.succeed();
    }

    private static boolean throwsOn(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static ServerPlayer detachedPlayer(GameTestHelper h) {
        Player raw = h.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) throw h.assertionException("makeMockServerPlayer returned " + raw.getClass().getName());
        return player;
    }
}
