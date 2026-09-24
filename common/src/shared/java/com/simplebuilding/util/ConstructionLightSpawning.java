package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Das Baulicht leuchtet mit Stufe 15, verhindert aber keine Monster-Spawns: sein Licht zaehlt fuer
 * {@code Monster#isDarkEnoughToSpawn} nicht mit.
 *
 * <p>Die Lichtengine kennt nur einen Wert pro Block und nicht, woher er kommt. Deshalb wird das
 * Blocklicht fuer die Spawnpruefung neu berechnet, nur aus den <em>anderen</em> Lichtquellen in
 * Reichweite, mit derselben Daempfung wie in der Lichtengine (siehe {@link #otherBlockLight}). Die
 * Rechnung nimmt eher zu viel Fremdlicht an als zu wenig, eine Fackel neben dem Baulicht schuetzt
 * also weiterhin mindestens so weit wie in Vanilla, und das Ergebnis ist nie heller als das echte
 * Blocklicht. Himmelslicht bleibt unveraendert: ein Baulicht im Freien macht den Tag nicht zur Nacht.
 *
 * <p>Ohne Baulicht in Reichweite (die Paletten der umliegenden Chunk-Sektionen verraten das billig)
 * rechnet Vanilla unveraendert.
 */
public final class ConstructionLightSpawning {

    /** Weiter als 14 Bloecke reicht kein Licht der Stufe 15. */
    private static final int REACH = 14;

    private static final Predicate<BlockState> IS_CONSTRUCTION_LIGHT = state -> state.is(ModBlocks.CONSTRUCTION_LIGHT);
    private static final Predicate<BlockState> IS_OTHER_EMITTER =
            state -> state.getLightEmission() > 0 && !state.is(ModBlocks.CONSTRUCTION_LIGHT);

    private ConstructionLightSpawning() {
    }

    /**
     * Die Vanilla-Pruefung mit Blocklicht ohne Baulicht, oder {@code null}, wenn kein Baulicht in
     * Reichweite ist oder es an dieser Stelle nichts aendern wuerde (dann rechnet Vanilla selbst).
     * Verbraucht Zufallszahlen in derselben Reihenfolge wie Vanilla.
     */
    public static @Nullable Boolean isDarkEnoughIgnoringConstructionLight(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        if (blockLight == 0 || !anySectionMaybeHas(level, pos, IS_CONSTRUCTION_LIGHT)) {
            return null;
        }
        int otherLight = otherBlockLight(level, pos, blockLight);
        if (otherLight >= blockLight) {
            return null;
        }

        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        if (skyLight > random.nextInt(32)) {
            return false;
        }
        DimensionType dimensionType = level.dimensionType();
        int blockLightLimit = dimensionType.monsterSpawnBlockLightLimit();
        if (blockLightLimit < 15 && otherLight > blockLightLimit) {
            return false;
        }
        int skyDarkening = level.getLevel().isThundering() ? 10 : level.getSkyDarken();
        int brightness = Math.max(otherLight, skyLight - skyDarkening);
        return brightness <= dimensionType.monsterSpawnLightTest().sample(random);
    }

    /**
     * Hellstes Licht, das eine andere Quelle als das Baulicht an {@code pos} haben kann, gedeckelt auf
     * {@code cap} (ab da steht fest, dass Vanilla-Verhalten herauskommt).
     *
     * <p>Rueckwaerts-Ausbreitung ab {@code pos} wie in der Lichtengine: jeder Schritt kostet die
     * Lichtdaempfung des Blocks, in den das Licht dabei eintritt (mindestens 1), volle Bloecke
     * sperren. Eine Quelle mit Leuchtstaerke {@code e}, die mit Kosten {@code d} erreicht wird,
     * bringt {@code e - d}. Die Form einzelner Flaechen (Stufen, Platten) wird nicht beachtet;
     * dadurch wird hoechstens zu viel Fremdlicht angenommen, nie zu wenig.
     */
    static int otherBlockLight(ServerLevelAccessor level, BlockPos pos, int cap) {
        int best = 0;
        Long2IntOpenHashMap cost = new Long2IntOpenHashMap();
        cost.defaultReturnValue(Integer.MAX_VALUE);
        LongArrayList[] buckets = new LongArrayList[REACH + 1];
        for (int i = 0; i <= REACH; i++) buckets[i] = new LongArrayList();
        cost.put(pos.asLong(), 0);
        buckets[0].add(pos.asLong());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (int d = 0; d <= REACH; d++) {
            // Keine Quelle ist heller als 15: ab hier kann niemand mehr "best" uebertreffen.
            if (15 - d <= best) break;
            LongArrayList bucket = buckets[d];
            for (int i = 0; i < bucket.size(); i++) {
                long packed = bucket.getLong(i);
                if (cost.get(packed) != d) continue;
                cursor.set(packed);
                if (!level.hasChunkAt(cursor)) continue;
                BlockState state = level.getBlockState(cursor);
                if (IS_OTHER_EMITTER.test(state)) {
                    best = Math.max(best, state.getLightEmission() - d);
                    if (best >= cap) return cap;
                }
                // Licht, das von einem Nachbarn her hier ankommt, tritt in diesen Block ein.
                int step = Math.max(1, state.getLightDampening());
                int nd = d + step;
                if (nd > REACH) continue;
                for (Direction direction : Direction.values()) {
                    next.setWithOffset(cursor, direction);
                    long key = next.asLong();
                    if (nd < cost.get(key)) {
                        cost.put(key, nd);
                        buckets[nd].add(key);
                    }
                }
            }
        }
        return best;
    }

    private static boolean anySectionMaybeHas(ServerLevelAccessor level, BlockPos pos, Predicate<BlockState> predicate) {
        for (int sx = SectionPos.blockToSectionCoord(pos.getX() - REACH); sx <= SectionPos.blockToSectionCoord(pos.getX() + REACH); sx++) {
            for (int sz = SectionPos.blockToSectionCoord(pos.getZ() - REACH); sz <= SectionPos.blockToSectionCoord(pos.getZ() + REACH); sz++) {
                ChunkAccess chunk = level.getChunk(sx, sz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (int sy = SectionPos.blockToSectionCoord(pos.getY() - REACH); sy <= SectionPos.blockToSectionCoord(pos.getY() + REACH); sy++) {
                    LevelChunkSection section = section(level, chunk, sy);
                    if (section != null && section.maybeHas(predicate)) return true;
                }
            }
        }
        return false;
    }

    private static @Nullable LevelChunkSection section(ServerLevelAccessor level, ChunkAccess chunk, int sectionY) {
        int index = level.getSectionIndexFromSectionY(sectionY);
        if (index < 0 || index >= chunk.getSections().length) return null;
        LevelChunkSection section = chunk.getSection(index);
        return section.hasOnlyAir() ? null : section;
    }
}
