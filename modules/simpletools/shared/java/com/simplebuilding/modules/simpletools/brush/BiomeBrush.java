package com.simplebuilding.modules.simpletools.brush;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.Protection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Biome Brush logic (concept N26 B; moved from Simple Mobs to Simple Tools on 21.10.): which material paints which biome, and painting a stroke - server side, claim-safe.
 *
 * <p>Materials come from data files {@code data/<namespace>/biome_brush/<name>.json}:
 * {@code {"biome": "minecraft:desert", "items": ["minecraft:sand", "#minecraft:sand"], "color": "#DBCB8B"}}
 * (items or item tags; color tints the stroke particles). Data packs add more; files are read again after /reload.
 *
 * <p>A stroke sets the 4x4x4 biome cells around the clicked block (Vanilla stores biomes in such cells) - radius 1 cell
 * by default, so 3x3x3 cells = 12 blocks wide - and resends those chunks so colors, weather and spawns follow at once.
 * Every cell is checked like a block edit: claims of any mod using the framework {@link Protection} (Simple Tweaks),
 * spawn protection and adventure mode; refused cells keep their biome, a refused clicked cell refuses the stroke.
 * Biomes stay in their dimension (Overworld biomes only in the Overworld ...), cave biomes ({@link #LOCKED}) are neither
 * painted over nor painted.
 */
public final class BiomeBrush {
    private static final Logger LOG = LoggerFactory.getLogger("simpletools");
    public static final String DIR = "biome_brush";
    /** Biomes that are never painted over (Deep Dark, caves). */
    public static final TagKey<Biome> LOCKED = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(com.simplebuilding.modules.simpletools.SimpleTools.MOD_ID, "biome_brush_locked"));

    /** One material: items and item tags, the biome it paints, the stroke color. */
    public record Entry(Identifier biome, List<Identifier> items, List<TagKey<Item>> tags, int color) {
        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
            for (TagKey<Item> t : tags) if (stack.is(t)) return true;
            return false;
        }
    }

    public enum Result { PAINTED, UNCHANGED, NO_MATERIAL, WRONG_DIMENSION, DENIED, DISABLED }

    private static @Nullable ResourceManager loadedFrom;
    private static List<Entry> entries = List.of();

    private BiomeBrush() {}

    /** All materials (read from the server's data, cached until the next /reload). */
    public static synchronized List<Entry> entries(MinecraftServer server) {
        ResourceManager rm = server.getResourceManager();
        if (rm != loadedFrom) {
            entries = load(rm);
            loadedFrom = rm;
        }
        return entries;
    }

    static List<Entry> load(ResourceManager rm) {
        Map<Identifier, Entry> out = new LinkedHashMap<>();
        rm.listResources(DIR, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                List<Identifier> items = new ArrayList<>();
                List<TagKey<Item>> tags = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("items")) {
                    String s = e.getAsString();
                    if (s.startsWith("#")) tags.add(TagKey.create(Registries.ITEM, Identifier.parse(s.substring(1))));
                    else items.add(Identifier.parse(s));
                }
                int color = o.has("color") ? Integer.parseInt(o.get("color").getAsString().replace("#", ""), 16) : 0x7FB238;
                out.put(file, new Entry(Identifier.parse(o.get("biome").getAsString()), items, tags, color));
            } catch (Exception ex) {
                LOG.warn("Biome Brush: skipping {} ({})", file, ex.toString());
            }
        });
        return List.copyOf(out.values());
    }

    /** The material entry for an off-hand stack, or empty. */
    public static Optional<Entry> material(MinecraftServer server, ItemStack stack) {
        for (Entry e : entries(server)) if (e.matches(stack)) return Optional.of(e);
        return Optional.empty();
    }

    /** Whether a biome may be painted in this level: Overworld, Nether and End biomes only in their own dimension. */
    public static boolean fitsDimension(ServerLevel level, Holder<Biome> biome) {
        if (biome.is(LOCKED)) return false;
        if (level.dimension() == Level.OVERWORLD) return biome.is(BiomeTags.IS_OVERWORLD);
        if (level.dimension() == Level.NETHER) return biome.is(BiomeTags.IS_NETHER);
        if (level.dimension() == Level.END) return biome.is(BiomeTags.IS_END);
        return level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(biome);
    }

    public static @Nullable Holder<Biome> biome(ServerLevel level, Identifier id) {
        return level.registryAccess().lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, id)).orElse(null);
    }

    /** Whether this player may edit the block at {@code pos}: claims, spawn protection, adventure mode. */
    public static boolean mayEdit(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!player.mayBuild() || !level.mayInteract(player, pos) || !level.isInWorldBounds(pos)) return false;
        Object server = level.getServer();
        if (!Protection.active(server)) return true;
        boolean admin = net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_OWNERS).test(player.createCommandSourceStack());
        return Protection.allows(server, new Protection.Target(player.getUUID(), level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), admin));
    }

    /** Paints one stroke around {@code target}; returns how many cells changed (0 = nothing to do or refused). */
    public static int paint(ServerLevel level, ServerPlayer player, BlockPos target, Holder<Biome> biome, int radius) {
        int cx = QuartPos.fromBlock(target.getX()), cy = QuartPos.fromBlock(target.getY()), cz = QuartPos.fromBlock(target.getZ());
        LongOpenHashSet cells = new LongOpenHashSet();
        Map<Long, ChunkAccess> chunks = new LinkedHashMap<>();
        for (int qx = cx - radius; qx <= cx + radius; qx++)
            for (int qy = cy - radius; qy <= cy + radius; qy++)
                for (int qz = cz - radius; qz <= cz + radius; qz++) {
                    BlockPos centre = new BlockPos(QuartPos.toBlock(qx) + 2, QuartPos.toBlock(qy) + 2, QuartPos.toBlock(qz) + 2);
                    if (!level.isInWorldBounds(centre) || !mayEdit(level, player, centre)) continue;
                    if (level.getNoiseBiome(qx, qy, qz).is(LOCKED)) continue;
                    // Loaded chunks only, checked before the structure lookup (that one reads chunks by itself).
                    int chx = SectionPos.blockToSectionCoord(centre.getX()), chz = SectionPos.blockToSectionCoord(centre.getZ());
                    ChunkAccess chunk = chunks.computeIfAbsent(ChunkPos.pack(chx, chz), k -> level.getChunk(chx, chz, ChunkStatus.FULL, false));
                    if (chunk == null) continue;
                    if (level.structureManager().getStructureWithPieceAt(centre, s -> true).isValid()) continue;
                    cells.add(BlockPos.asLong(qx, qy, qz));
                }
        int changed = 0;
        List<ChunkAccess> touched = new ArrayList<>();
        for (ChunkAccess chunk : chunks.values()) {
            if (chunk == null) continue;
            int[] count = {0};
            chunk.fillBiomesFromNoise((qx, qy, qz) -> {
                Holder<Biome> now = chunk.getNoiseBiome(qx, qy, qz);
                if (!cells.contains(BlockPos.asLong(qx, qy, qz)) || now.is(biome)) return now;
                count[0]++;
                return biome;
            });
            if (count[0] > 0) {
                chunk.markUnsaved();
                touched.add(chunk);
                changed += count[0];
            }
        }
        if (!touched.isEmpty()) level.getChunkSource().chunkMap.resendBiomesForChunks(touched);
        return changed;
    }

    /** Whether the clicked cell itself may be painted (claims etc.); a refused centre refuses the whole stroke. */
    public static boolean centreAllowed(ServerLevel level, ServerPlayer player, BlockPos target) {
        BlockPos centre = new BlockPos(QuartPos.toBlock(QuartPos.fromBlock(target.getX())) + 2,
                QuartPos.toBlock(QuartPos.fromBlock(target.getY())) + 2, QuartPos.toBlock(QuartPos.fromBlock(target.getZ())) + 2);
        return mayEdit(level, player, target) && mayEdit(level, player, centre);
    }
}
