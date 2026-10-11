package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.crucible.CrucibleCompat;
import com.simplebuilding.datafix.IdMovesHook;
import com.simplebuilding.items.ModItems;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Migration test world (split plan stage 3b): a saved world from before the split loads after ids moved.
 *
 * <p><b>The fixture.</b> testing/fixtures/migration-before/ holds save data of this build, in the formats the game
 * writes: {@code chunk.snbt} (chunk 0,0 with a section of SimpleBuilding blocks - chest tiers, furnaces, the enderite
 * crucible, octets - and their block entities, the chests full of hammers, an enchanted enderite chestplate with mod
 * components, octet and crucible items), {@code player.snbt} (inventory with every sledgehammer, the recipe book with
 * mod recipes) and {@code advancements.json} (progress file with mod advancements). {@link #fixturesAreBuildable}
 * rewrites them with {@code SIMPLEBUILDING_WRITE_MIGRATION_FIXTURES=1}; without it the builder only runs in memory
 * (so it cannot rot), and the files stay the "before" world. Once real ids move (stage 4 on), this same world is
 * the old world the moves must load.
 *
 * <p><b>The move.</b> No real id moves yet. {@link #worldLoadsUnderMovedIds} therefore spells every SimpleBuilding
 * id of the fixture as {@code simpletest:} - palette names, block entity ids, item ids, component types,
 * enchantments, recipe-book keys, advancement ids - the shape of a world saved before a namespace move, and the
 * test move {@code simpletest -> simplebuilding} ({@code IdMovesHook}) must load it exactly as the unchanged
 * fixture loads (the oracle), and write it back under the new ids. The unchanged fixture itself must load with
 * every block, block entity and item in place (null probe).
 */
public final class MigrationWorldTests {

    static final String FIXTURE_DIR = "testing/fixtures/migration-before";
    static final String WRITE_ENV = "SIMPLEBUILDING_WRITE_MIGRATION_FIXTURES";
    static final String CHUNK = "chunk.snbt";
    static final String PLAYER = "player.snbt";
    static final String ADVANCEMENTS = "advancements.json";
    static final String OLD = IdMovesHook.TEST_NAMESPACE + ":";
    static final String NOW = Simplebuilding.MOD_ID + ":";
    static final int SECTION_Y = 4;
    static final List<String> PLAYER_KEYS = List.of("Inventory", "recipeBook");
    /** Compound keys whose children are registry ids (component types, enchantments). */
    static final Set<String> ID_KEYED = Set.of("components", "minecraft:enchantments", "minecraft:stored_enchantments");
    /** String fields that hold a registry id (item/block entity/entity id, palette block name). */
    static final Set<String> ID_FIELDS = Set.of("id", "Name");
    /** String lists of recipe keys (recipe book). */
    static final Set<String> ID_LISTS = Set.of("recipes", "toBeDisplayed");

    private MigrationWorldTests() {
    }

    // ===================================================================================== the samples

    /** The placed blocks, x = index in section (z = 1, y = 64). */
    static List<Block> sampleBlocks() {
        List<Block> blocks = new ArrayList<>(List.of(ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST,
                ModBlocks.REINFORCED_FURNACE, ModBlocks.ENDERITE_FURNACE, ModBlocks.CHECKER_OCTET));
        if (!ModBlocks.MATERIAL_OCTETS.isEmpty()) blocks.add(ModBlocks.MATERIAL_OCTETS.get(0));
        if (!ModBlocks.WOOD_OCTETS.isEmpty()) blocks.add(ModBlocks.WOOD_OCTETS.get(0));
        if (CrucibleCompat.enderiteCrucible() != null) blocks.add(CrucibleCompat.enderiteCrucible());
        return blocks;
    }

    static BlockPos samplePos(int index) {
        return new BlockPos(index, SECTION_Y * 16, 1);
    }

    /** Mod items with mod components and enchantments, the contents of every sample container. */
    static List<ItemStack> sampleStacks(GameTestHelper helper) {
        List<ItemStack> items = new ArrayList<>();
        items.add(parseStack(helper, "{id:\"simplebuilding:diamond_sledgehammer\",count:1,components:{"
                + "\"minecraft:enchantments\":{\"simplebuilding:range\":2},\"minecraft:damage\":17}}"));
        items.add(parseStack(helper, "{id:\"simplebuilding:enderite_chestplate\",count:1,components:{"
                + "\"minecraft:enchantments\":{\"simplebuilding:kinetic_protection\":2,\"minecraft:unbreaking\":3},"
                + "\"simplebuilding:glow_level\":1,\"simplebuilding:visual_glow\":1b,\"minecraft:damage\":11}}"));
        items.add(new ItemStack(ModItems.ENDERITE_SLEDGEHAMMER));
        for (Block block : sampleBlocks()) {
            if (block.asItem() != net.minecraft.world.item.Items.AIR) items.add(new ItemStack(block.asItem(), 3));
        }
        return items;
    }

    static List<ItemStack> sledgehammers() {
        return List.of(ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER, ModItems.GOLD_SLEDGEHAMMER,
                ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_SLEDGEHAMMER).stream()
                .map(ItemStack::new).toList();
    }

    // ===================================================================================== the builder

    /**
     * Builds the fixture in memory; with {@code SIMPLEBUILDING_WRITE_MIGRATION_FIXTURES=1} writes it to
     * testing/fixtures/migration-before. Without the variable, the committed fixture must exist and parse.
     */
    public static void fixturesAreBuildable(GameTestHelper helper) {
        CompoundTag chunk = writeChunk(helper);
        CompoundTag player = writePlayer(helper);
        JsonObject advancements = writeAdvancements(helper);
        helper.assertTrue(chunk.toString().contains(NOW) && player.toString().contains(NOW) && advancements.toString().contains(NOW),
                "the built fixture holds no SimpleBuilding ids");
        Path dir = fixtureDir(helper);
        if ("1".equals(System.getenv(WRITE_ENV))) {
            try {
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(CHUNK), new SnbtPrinterTagVisitor().visit(chunk) + "\n", StandardCharsets.UTF_8);
                Files.writeString(dir.resolve(PLAYER), new SnbtPrinterTagVisitor().visit(player) + "\n", StandardCharsets.UTF_8);
                Files.writeString(dir.resolve(ADVANCEMENTS), new com.google.gson.GsonBuilder().setPrettyPrinting().create()
                        .toJson(advancements) + "\n", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw helper.assertionException("could not write the migration fixture: " + e);
            }
        } else {
            readFixture(helper, CHUNK);
            readFixture(helper, PLAYER);
            readJson(helper, ADVANCEMENTS);
        }
        helper.succeed();
    }

    private static CompoundTag writeChunk(GameTestHelper helper) {
        PalettedContainerFactory factory = helper.getLevel().palettedContainerFactory();
        LevelChunkSection section = new LevelChunkSection(factory);
        ListTag blockEntities = new ListTag();
        List<Block> blocks = sampleBlocks();
        List<ItemStack> contents = sampleStacks(helper);
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = samplePos(i);
            BlockState state = blocks.get(i).defaultBlockState();
            section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state, false);
            if (!(blocks.get(i) instanceof EntityBlock entityBlock)) continue;
            BlockEntity be = entityBlock.newBlockEntity(pos, state);
            if (be == null) continue;
            if (be instanceof Container container) {
                for (int slot = 0; slot < Math.min(container.getContainerSize(), contents.size()); slot++) {
                    if (container.canPlaceItem(slot, contents.get(slot))) container.setItem(slot, contents.get(slot).copy());
                }
            }
            blockEntities.add(be.saveWithFullMetadata(helper.getLevel().registryAccess()));
        }
        CompoundTag sectionTag = new CompoundTag();
        sectionTag.putByte("Y", (byte) SECTION_Y);
        sectionTag.put("block_states", factory.blockStatesContainerCodec().encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow());
        ListTag sections = new ListTag();
        sections.add(sectionTag);
        CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", currentDataVersion());
        chunk.putInt("xPos", 0);
        chunk.putInt("zPos", 0);
        chunk.putInt("yPos", helper.getLevel().getMinSectionY());
        chunk.putString("Status", "minecraft:full");
        chunk.put("sections", sections);
        chunk.put("block_entities", blockEntities);
        return chunk;
    }

    private static CompoundTag writePlayer(GameTestHelper helper) {
        ServerPlayer player = detachedPlayer(helper);
        List<ItemStack> hammers = sledgehammers();
        for (int i = 0; i < hammers.size(); i++) player.getInventory().setItem(i, hammers.get(i).copy());
        List<ItemStack> stacks = sampleStacks(helper);
        for (int i = 0; i < stacks.size() && 9 + i < 36; i++) player.getInventory().setItem(9 + i, stacks.get(i).copy());
        List<RecipeHolder<?>> recipes = modRecipes(helper.getLevel().getServer());
        for (RecipeHolder<?> recipe : recipes) player.getRecipeBook().add(recipe.id());
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        CompoundTag full = output.buildResult();
        CompoundTag subset = new CompoundTag();
        subset.putInt("DataVersion", currentDataVersion());
        for (String key : PLAYER_KEYS) {
            Tag value = full.get(key);
            helper.assertTrue(value != null, "a saved player has no " + key);
            subset.put(key, value);
        }
        return subset;
    }

    /**
     * A progress file in the game's format ({@code PlayerAdvancements#save}: id -> criteria with dates, done) with every
     * criterion of a few mod advancements done. Written by hand rather than by awarding, which would grant rewards.
     */
    private static JsonObject writeAdvancements(GameTestHelper helper) {
        JsonObject progress = new JsonObject();
        for (AdvancementHolder holder : modAdvancements(helper.getLevel().getServer())) {
            JsonObject criteria = new JsonObject();
            for (String criterion : holder.value().criteria().keySet()) criteria.addProperty(criterion, "2026-10-11 12:00:00 +0000");
            JsonObject entry = new JsonObject();
            entry.add("criteria", criteria);
            entry.addProperty("done", true);
            progress.add(holder.id().toString(), entry);
        }
        progress.addProperty("DataVersion", currentDataVersion());
        return progress;
    }

    static List<RecipeHolder<?>> modRecipes(MinecraftServer server) {
        return server.getRecipeManager().getRecipes().stream()
                .filter(r -> r.id().identifier().getNamespace().equals(Simplebuilding.MOD_ID))
                .sorted(Comparator.comparing(r -> r.id().identifier().toString())).limit(6).toList();
    }

    static List<AdvancementHolder> modAdvancements(MinecraftServer server) {
        return server.getAdvancements().getAllAdvancements().stream()
                .filter(a -> a.id().getNamespace().equals(Simplebuilding.MOD_ID) && !a.value().criteria().isEmpty())
                .sorted(Comparator.comparing(a -> a.id().toString())).limit(4).toList();
    }

    // ===================================================================================== the test

    /**
     * The saved world, with its SimpleBuilding ids spelled {@code simpletest:}, loads exactly like the unchanged
     * world: same block in every position of the section, same block entities with the same saved data (items,
     * components, enchantments), same player inventory and recipe book, same advancement progress - and everything
     * is written back under {@code simplebuilding:}. The unchanged world loads every sample (null probe).
     */
    public static void worldLoadsUnderMovedIds(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        // --- chunk
        CompoundTag before = readFixture(helper, CHUNK);
        CompoundTag moved = before.copy();
        int rewritten = rewrite(moved, "");
        helper.assertTrue(rewritten >= 10, "the moved fixture rewrote only " + rewritten + " ids");

        SerializableChunkData oracle = SerializableChunkData.parse(helper.getLevel(), helper.getLevel().palettedContainerFactory(), before);
        SerializableChunkData loaded = SerializableChunkData.parse(helper.getLevel(), helper.getLevel().palettedContainerFactory(), moved);
        helper.assertTrue(oracle != null && loaded != null, "the fixture chunk does not parse");
        LevelChunkSection oracleSection = section(helper, oracle);
        LevelChunkSection loadedSection = section(helper, loaded);

        List<Block> blocks = sampleBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = samplePos(i);
            BlockState state = oracleSection.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            if (!state.is(blocks.get(i))) problems.add("null probe: " + pos + " holds " + state + " instead of " + BuiltInRegistries.BLOCK.getKey(blocks.get(i)));
        }
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
            BlockState want = oracleSection.getBlockState(x, y, z);
            BlockState got = loadedSection.getBlockState(x, y, z);
            if (want != got) problems.add("section " + x + "," + y + "," + z + ": moved world holds " + got + ", old world " + want);
        }
        Tag resavedStates = helper.getLevel().palettedContainerFactory().blockStatesContainerCodec()
                .encodeStart(NbtOps.INSTANCE, loadedSection.getStates()).getOrThrow();
        if (resavedStates.toString().contains(OLD)) problems.add("palette saved back as " + resavedStates);

        helper.assertValueEqual(loaded.blockEntities().size(), oracle.blockEntities().size(), "block entities in the moved chunk");
        for (int i = 0; i < oracle.blockEntities().size(); i++) {
            CompoundTag oracleTag = oracle.blockEntities().get(i);
            BlockPos pos = BlockEntity.getPosFromTag(new net.minecraft.world.level.ChunkPos(0, 0), oracleTag);
            BlockState state = oracleSection.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            BlockEntity want = BlockEntity.loadStatic(pos, state, oracleTag, helper.getLevel().registryAccess());
            BlockEntity got = BlockEntity.loadStatic(pos, state, loaded.blockEntities().get(i), helper.getLevel().registryAccess());
            if (want == null) {
                problems.add("null probe: block entity at " + pos + " (" + oracleTag.getStringOr("id", "?") + ") does not load");
                continue;
            }
            if (!oracleTag.getListOrEmpty("Items").isEmpty() && want instanceof Container container && container.isEmpty()) {
                problems.add("null probe: " + pos + " lost its items");
            }
            if (got == null || got.getType() != want.getType()) {
                problems.add("block entity at " + pos + " loads as " + got + " instead of " + want.getType());
                continue;
            }
            CompoundTag wantSaved = want.saveWithFullMetadata(helper.getLevel().registryAccess());
            CompoundTag gotSaved = got.saveWithFullMetadata(helper.getLevel().registryAccess());
            if (!wantSaved.equals(gotSaved)) problems.add("block entity at " + pos + " saves back as " + gotSaved + ", old world " + wantSaved);
        }

        // --- player: inventory and recipe book
        CompoundTag playerBefore = readFixture(helper, PLAYER);
        CompoundTag playerMoved = playerBefore.copy();
        rewrite(playerMoved, "");
        ServerPlayer want = detachedPlayer(helper);
        ServerPlayer got = detachedPlayer(helper);
        want.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), playerBefore));
        got.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), playerMoved));
        List<ItemStack> hammers = sledgehammers();
        for (int i = 0; i < hammers.size(); i++) {
            if (!want.getInventory().getItem(i).is(hammers.get(i).getItem())) problems.add("null probe: inventory slot " + i + " holds " + want.getInventory().getItem(i));
        }
        for (int slot = 0; slot < want.getInventory().getContainerSize(); slot++) {
            if (!ItemStack.matches(want.getInventory().getItem(slot), got.getInventory().getItem(slot))) {
                problems.add("inventory slot " + slot + ": " + got.getInventory().getItem(slot) + " instead of " + want.getInventory().getItem(slot));
            }
        }
        List<ResourceKey<Recipe<?>>> known = want.getRecipeBook().pack().known();
        if (known.isEmpty()) problems.add("null probe: the recipe book is empty");
        for (ResourceKey<Recipe<?>> key : known) {
            if (!got.getRecipeBook().contains(key)) problems.add("the recipe book lost " + key.identifier());
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        got.saveWithoutId(output);
        CompoundTag playerResaved = output.buildResult();
        for (String key : PLAYER_KEYS) {
            if (String.valueOf(playerResaved.get(key)).contains(OLD)) problems.add("player " + key + " saved back with " + OLD + " ids");
        }

        // --- advancements
        MinecraftServer server = helper.getLevel().getServer();
        JsonObject progressBefore = readJson(helper, ADVANCEMENTS);
        JsonObject progressMoved = new JsonObject();
        List<Identifier> doneBefore = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : progressBefore.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(NOW)) doneBefore.add(Identifier.parse(key));
            progressMoved.add(key.startsWith(NOW) ? OLD + key.substring(NOW.length()) : key, entry.getValue());
        }
        if (doneBefore.isEmpty()) problems.add("null probe: the advancement fixture holds no mod advancement");
        Path file = server.getServerDirectory().resolve("simplebuilding-migration-test").resolve("moved-advancements.json");
        ServerPlayer advancementPlayer = detachedPlayer(helper);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, progressMoved.toString(), StandardCharsets.UTF_8);
            PlayerAdvancements advancements = new PlayerAdvancements(server.getFixerUpper(), server.getPlayerList(), server.getAdvancements(), file, advancementPlayer);
            for (Identifier id : doneBefore) {
                AdvancementHolder holder = server.getAdvancements().get(id);
                if (holder == null) problems.add("null probe: advancement " + id + " no longer exists");
                else if (!advancements.getOrStartProgress(holder).isDone()) problems.add("advancement progress saved as " + OLD + id.getPath() + " was lost");
            }
            advancements.save();
            advancements.clearTriggers();
            String resaved = Files.readString(file, StandardCharsets.UTF_8);
            if (resaved.contains(OLD)) problems.add("advancement progress saved back with " + OLD + " ids");
        } catch (IOException e) {
            throw helper.assertionException("advancement fixture: " + e);
        }

        // --- control: the move is a namespace move, unknown paths stay unknown
        Identifier unknown = Identifier.fromNamespaceAndPath(IdMovesHook.TEST_NAMESPACE, "not_a_simplebuilding_block");
        if (BuiltInRegistries.BLOCK.get(unknown).isPresent()) problems.add(unknown + " resolves to a block");

        helper.assertTrue(problems.isEmpty(), "migration world: " + problems.size() + " problems: "
                + problems.subList(0, Math.min(12, problems.size())));
        helper.succeed();
    }

    // ===================================================================================== helpers

    /**
     * Spells SimpleBuilding registry ids of saved data as {@code simpletest:} in place: id/Name string fields, keys of
     * component and enchantment maps, recipe-book key lists. Other strings (model ids, custom data) stay. Returns the
     * number of ids changed.
     */
    static int rewrite(Tag tag, String parentKey) {
        int count = 0;
        if (tag instanceof CompoundTag compound) {
            for (String key : List.copyOf(compound.keySet())) {
                Tag value = compound.get(key);
                if (value instanceof StringTag(String text) && ID_FIELDS.contains(key) && text.startsWith(NOW)) {
                    compound.putString(key, OLD + text.substring(NOW.length()));
                    count++;
                    continue;
                }
                count += rewrite(value, key);
                if (ID_KEYED.contains(parentKey) && (key.startsWith(NOW) || key.startsWith("!" + NOW))) {
                    String bang = key.startsWith("!") ? "!" : "";
                    compound.remove(key);
                    compound.put(bang + OLD + key.substring(bang.length() + NOW.length()), value);
                    count++;
                }
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                Tag element = list.get(i);
                if (element instanceof StringTag(String text) && ID_LISTS.contains(parentKey) && text.startsWith(NOW)) {
                    list.set(i, StringTag.valueOf(OLD + text.substring(NOW.length())));
                    count++;
                } else {
                    count += rewrite(element, parentKey);
                }
            }
        }
        return count;
    }

    private static LevelChunkSection section(GameTestHelper helper, SerializableChunkData data) {
        for (SerializableChunkData.SectionData section : data.sectionData()) {
            if (section.y() == SECTION_Y && section.chunkSection() != null) return section.chunkSection();
        }
        throw helper.assertionException("the fixture chunk has no section " + SECTION_Y);
    }

    static int currentDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    private static RegistryOps<Tag> ops(GameTestHelper helper) {
        return helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    private static ItemStack parseStack(GameTestHelper helper, String snbt) {
        try {
            return ItemStack.CODEC.parse(ops(helper), TagParser.parseCompoundFully(snbt))
                    .getOrThrow(error -> helper.assertionException("stack " + snbt + " does not decode: " + error));
        } catch (CommandSyntaxException e) {
            throw helper.assertionException("bad SNBT " + snbt + ": " + e.getMessage());
        }
    }

    private static ServerPlayer detachedPlayer(GameTestHelper helper) {
        Player raw = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) throw helper.assertionException("makeMockServerPlayer returned " + raw.getClass().getName());
        return player;
    }

    static Path fixtureDir(GameTestHelper helper) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("testing"))) return dir.resolve(FIXTURE_DIR);
            dir = dir.getParent();
        }
        throw helper.assertionException("no testing/ directory above " + Path.of("").toAbsolutePath());
    }

    private static CompoundTag readFixture(GameTestHelper helper, String name) {
        Path file = fixtureDir(helper).resolve(name);
        try {
            return TagParser.parseCompoundFully(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | CommandSyntaxException e) {
            throw helper.assertionException("cannot read migration fixture " + file + ": " + e.getMessage());
        }
    }

    private static JsonObject readJson(GameTestHelper helper, String name) {
        Path file = fixtureDir(helper).resolve(name);
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            throw helper.assertionException("cannot read migration fixture " + file + ": " + e.getMessage());
        }
    }
}
