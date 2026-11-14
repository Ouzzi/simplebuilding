package com.simplebuilding.dev.testcentre;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blueprint.BlueprintScanner;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Schreibt eine {@link TestCentreLayout.Plan Planung} in eine Welt: Bereich leeren (Entities,
 * Behaelterinhalte, Bloecke), Boden legen, dann die Schritte der Abschnitte ausfuehren - erst alle
 * Bloecke, danach Schilder, Behaelter und Befehlsbloecke, zuletzt Rahmen und Ruestungsstaender.
 *
 * <p>Bloecke werden "leise" gesetzt (keine Nachbar-Updates, keine Drops): eine Leiter oder ein
 * Schild, deren Halt in derselben Runde spaeter kommt, fallen so nicht ab, und ein Kolben faehrt beim
 * Bau nicht von selbst aus.
 */
public final class TestCentreBuilder {

    /** Setzen ohne Nachbar-Updates und ohne Drops. */
    static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** Leeren: zusaetzlich ohne Nebenwirkungen der Block-Entities (Behaelter verstreuen nichts). */
    static final int CLEAR = QUIET | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
    static final BlockState SECTION_FLOOR = Blocks.DEEPSLATE_TILES.defaultBlockState();
    static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
    static final int LAMP_SPACING = 6;

    /** Was ein Bau getan hat. */
    public record Result(int ops, int entities, long millis) {
    }

    private TestCentreBuilder() {
    }

    /** Die ganze Zentrale: Bereich leeren, Boden, alle Abschnitte. */
    public static Result build(ServerLevel level, TestCentreLayout.Plan plan) {
        long start = System.currentTimeMillis();
        BoundingBox bounds = plan.bounds();
        clear(level, bounds);
        BlockPos origin = plan.origin();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                level.setBlock(new BlockPos(x, origin.getY() - 1, z), lamp(origin, x, z) ? LAMP : FLOOR, QUIET);
            }
        }
        int ops = 0;
        int entities = 0;
        for (TestCentreLayout.Section section : plan.sections()) {
            sectionFloor(level, origin, section);
            int[] done = apply(level, section.ops());
            ops += done[0];
            entities += done[1];
            if (OrderAreas.SECTIONS.contains(section.id())) {
                settle(level, section.ops(), section.box(origin));
            }
        }
        return new Result(ops, entities, System.currentTimeMillis() - start);
    }

    /** Nur ein Abschnitt: sein Kasten wird geleert und neu gebaut, der Rest bleibt stehen. */
    public static Result buildSection(ServerLevel level, TestCentreLayout.Plan plan, String id) {
        long start = System.currentTimeMillis();
        TestCentreLayout.Section section = plan.section(id);
        clear(level, section.box(plan.origin()));
        sectionFloor(level, plan.origin(), section);
        int[] done = apply(level, section.ops());
        if (OrderAreas.SECTIONS.contains(id)) {
            settle(level, section.ops(), section.box(plan.origin()));
        }
        return new Result(done[0], done[1], System.currentTimeMillis() - start);
    }

    /**
     * DW04D-1 ({@code /testcenter mods|mod <id> build}): eine Zeichenflaeche fuer sich an {@code origin} (vordere linke Ecke,
     * Standhoehe). Ihr Kasten wird vorher geleert (Entities ausser Spielern, Behaelter, Bloecke; ein Feld Rand), dann Boden
     * (glatter Stein, Seelaternen im 6er-Raster) und die Schritte.
     */
    public static Result buildCanvas(ServerLevel level, BlockPos origin, TcCanvas canvas) {
        long start = System.currentTimeMillis();
        BoundingBox box = canvasBox(origin, canvas);
        clear(level, box);
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                level.setBlock(new BlockPos(x, origin.getY() - 1, z), lamp(origin, x, z) ? LAMP : FLOOR, QUIET);
            }
        }
        List<TcOp> ops = new ArrayList<>();
        canvas.ops().forEach(op -> ops.add(op.moved(origin)));
        int[] done = apply(level, ops);
        settle(level, ops, box);
        return new Result(done[0], done[1], System.currentTimeMillis() - start);
    }

    /**
     * TZ38E-11 (Besitzer 13.11.: "Block-Updates nach dem Bau: Licht neu berechnen, Formen/Verbindungen aktualisieren - heute
     * verbinden Zaeune erst nach Neu-Setzen, Licht verbuggt"): nach dem stillen Setzen einer Ausstellungsflaeche (Ordnungs-
     * Bereiche, {@code /testcenter order|mods build}) bekommt jeder gesetzte Block die Form zu seinen Nachbarn (Zaeune, Mauern,
     * Scheiben, Gitter, Redstone, Treppen-Ecken); was dabei zerfiele, bleibt wie gesetzt (kein Abbau beim Bau), Beobachter bleiben
     * still. Danach rechnet die Licht-Engine die Chunks des Kastens neu (Lichtquellen und Himmelslicht).
     */
    static int settle(ServerLevel level, List<TcOp> ops, BoundingBox box) {
        int changed = 0;
        for (TcOp op : ops) {
            if (!(op instanceof TcOp.Place place)) {
                continue;
            }
            BlockState placed = level.getBlockState(place.pos());
            if (placed.isAir() || placed.getBlock() instanceof net.minecraft.world.level.block.ObserverBlock) {
                continue;
            }
            BlockState shaped;
            try {
                shaped = Block.updateFromNeighbourShapes(placed, level, place.pos());
            } catch (RuntimeException e) {
                continue;
            }
            if (!shaped.isAir() && shaped.getBlock() == placed.getBlock() && shaped != placed) {
                level.setBlock(place.pos(), shaped, QUIET);
                changed++;
            }
        }
        var light = level.getChunkSource().getLightEngine();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                light.lightChunk(level.getChunk(cx, cz), false);
            }
        }
        return changed;
    }

    /** Der Kasten, den {@link #buildCanvas} leert: die Flaeche mit einem Feld Rand, vom Boden bis zwei ueber die Hoehe. */
    public static BoundingBox canvasBox(BlockPos origin, TcCanvas canvas) {
        return new BoundingBox(origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1,
                origin.getX() + canvas.width(), origin.getY() + canvas.height() + 2, origin.getZ() + canvas.depth());
    }

    private static boolean lamp(BlockPos origin, int x, int z) {
        return Math.floorMod(x - origin.getX(), LAMP_SPACING) == 3 && Math.floorMod(z - origin.getZ(), LAMP_SPACING) == 3;
    }

    private static void sectionFloor(ServerLevel level, BlockPos origin, TestCentreLayout.Section section) {
        BlockPos min = origin.offset(section.offset());
        for (int x = 0; x < section.width(); x++) {
            for (int z = 0; z < section.depth(); z++) {
                int ax = min.getX() + x;
                int az = min.getZ() + z;
                level.setBlock(new BlockPos(ax, origin.getY() - 1, az), lamp(origin, ax, az) ? LAMP : SECTION_FLOOR, QUIET);
            }
        }
    }

    /** Entities (ausser Spielern) weg, Behaelter leeren, dann alles zu Luft; Drops danach noch einmal weg. */
    public static void clear(ServerLevel level, BoundingBox box) {
        AABB area = AABB.of(box).inflate(1.0);
        removeEntities(level, area);
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (box.isInside(blockEntity.getBlockPos()) && blockEntity instanceof Container container) {
                        container.clearContent();
                    }
                }
            }
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = box.maxY(); y >= box.minY(); y--) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, CLEAR);
                    }
                }
            }
        }
        removeEntities(level, area);
    }

    private static int removeEntities(ServerLevel level, AABB area) {
        List<Entity> found = level.getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof Player));
        found.forEach(Entity::discard);
        return found.size();
    }

    /** Fuehrt Schritte aus; liefert {Schritte, Entities}. */
    public static int[] apply(ServerLevel level, List<TcOp> ops) {
        int count = 0;
        int entities = 0;
        for (TcOp op : ops) {
            if (op instanceof TcOp.Place place) {
                level.setBlock(place.pos(), place.state(), QUIET);
                count++;
            }
        }
        // Redstone dust of every line is placed without neighbour updates: give it its connections afterwards.
        for (TcOp op : ops) {
            if (op instanceof TcOp.Place place && com.simplebuilding.util.OresLine.lineOf(place.state()) != null) {
                BlockState placed = level.getBlockState(place.pos());
                level.setBlock(place.pos(), Block.updateFromNeighbourShapes(placed, level, place.pos()), QUIET);
            }
        }
        // Strukturvorlagen (TZ22-3) nach den Bloecken, vor Schildern und Rahmen; ihre Rahmen-Entities zaehlen mit.
        for (TcOp op : ops) {
            if (op instanceof TcOp.Structure structure) {
                placeStructure(level, structure);
                entities += structure.frames();
                count++;
            }
        }
        for (TcOp op : ops) {
            switch (op) {
                case TcOp.Sign sign -> {
                    placeSign(level, sign.pos(), sign.facing(), sign.lines(), sign.standing());
                    count++;
                }
                case TcOp.Fill fill -> {
                    if (level.getBlockEntity(fill.pos()) instanceof Container container) {
                        for (int i = 0; i < fill.contents().size() && i < container.getContainerSize(); i++) {
                            container.setItem(i, fill.contents().get(i).copy());
                        }
                        container.setChanged();
                    } else if (level.getBlockEntity(fill.pos()) instanceof PlacedTemplateBlockEntity placed && !fill.contents().isEmpty()) {
                        placed.setTemplate(fill.contents().getFirst());
                    } else if (level.getBlockEntity(fill.pos()) instanceof net.minecraft.world.level.block.entity.BlockEntity other
                            && TestCentreSections.SIMPLE_TRIMS_PLACED.equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(other.getType()))
                            && !fill.contents().isEmpty()) {
                        // Simple Trims (26.3, N18): no class import - its block entity reads the template from the key "Template".
                        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                        tag.put("Template", net.minecraft.world.item.ItemStack.CODEC.encodeStart(
                                level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), fill.contents().getFirst()).getOrThrow());
                        other.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
                        other.setChanged();
                        level.sendBlockUpdated(fill.pos(), other.getBlockState(), other.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    } else if (level.getBlockEntity(fill.pos()) instanceof com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity pile) {
                        pile.setParts(fill.contents());
                    } else if (level.getBlockEntity(fill.pos()) instanceof com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity holder
                            && !fill.contents().isEmpty()) {
                        // Ziegenhorn-Halter (Queue N24): erstes Item das Horn, ein zweites steckt darin.
                        holder.setHorn(fill.contents().getFirst());
                        holder.setHeld(fill.contents().size() > 1 ? fill.contents().get(1) : ItemStack.EMPTY);
                    } else if (level.getBlockEntity(fill.pos()) instanceof com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity bundle
                            && !fill.contents().isEmpty()) {
                        bundle.setBundle(fill.contents().getFirst());
                    } else if (level.getBlockEntity(fill.pos()) instanceof net.minecraft.world.level.block.entity.BlockEntity other
                            && com.simplebuilding.util.StorageRefs.id("placed_bundle").equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(other.getType()))
                            && !fill.contents().isEmpty()) {
                        // Split stage 8c-1 (26.3): Simple Storage's placed bundle, no class import - it reads the bundle from the key "Bundle".
                        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                        tag.put("Bundle", net.minecraft.world.item.ItemStack.CODEC.encodeStart(
                                level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), fill.contents().getFirst()).getOrThrow());
                        other.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
                        other.setChanged();
                        level.sendBlockUpdated(fill.pos(), other.getBlockState(), other.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    }
                    count++;
                }
                case TcOp.Command command -> {
                    placeCommand(level, command);
                    count++;
                }
                default -> {
                }
            }
        }
        for (TcOp op : ops) {
            switch (op) {
                case TcOp.Frame frame -> {
                    spawnFrame(level, frame.pos(), frame.facing(), frame.stack());
                    entities++;
                    count++;
                }
                case TcOp.OctantFrame frame -> {
                    spawnFrame(level, frame.pos(), frame.facing(), octant(frame.cornerA(), frame.cornerB(), frame.shape()));
                    entities++;
                    count++;
                }
                case TcOp.BlueprintFrame frame -> {
                    // Der gescannte Bereich steht schon: alle Bloecke kommen vor den Rahmen.
                    // b-btools07: 26.3 scans with Simple Building Tools' own code; SimpleBuilding's scanner knows only its 26.2 items.
                    BlueprintScanner.Outcome outcome = !com.simplebuilding.version.McVersion.BTOOLS_IN_SB ? null
                            : BlueprintScanner.scanAtTable(level, frame.table(),
                            octant(frame.cornerA(), frame.cornerB()), new ItemStack(com.simplebuilding.util.BuildingToolRefs.blueprint()));
                    if (outcome == null) {
                        spawnFrame(level, frame.pos(), frame.facing(), new ItemStack(com.simplebuilding.util.BuildingToolRefs.blueprint()));
                        entities++;
                        count++;
                        continue;
                    }
                    if (outcome.error() != null) {
                        Simplebuilding.LOGGER.warn("Test centre blueprint scan refused: {}", outcome.error().getString());
                    }
                    spawnFrame(level, frame.pos(), frame.facing(),
                            outcome.error() == null ? outcome.result() : new ItemStack(com.simplebuilding.util.BuildingToolRefs.blueprint()));
                    entities++;
                    count++;
                }
                case TcOp.Mob mob -> {
                    if (spawnMob(level, mob)) {
                        entities++;
                    }
                    count++;
                }
                case TcOp.Hammock hammock -> {
                    com.simplebuilding.blocks.custom.HammockLayout.link(level,
                            new com.simplebuilding.blocks.custom.HammockLayout.Spot(hammock.pos(), hammock.dx(), hammock.dz()));
                    count++;
                }
                case TcOp.Stand stand -> {
                    ArmorStand entity = stand.dummy()
                            ? com.simplebuilding.entity.ModEntities.TRAINING_DUMMY.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                            : new ArmorStand(level, 0, 0, 0);
                    entity.setPos(stand.pos().getX() + 0.5, stand.pos().getY(), stand.pos().getZ() + 0.5);
                    entity.setYRot(stand.yaw());
                    entity.setYBodyRot(stand.yaw());
                    entity.setYHeadRot(stand.yaw());
                    entity.setShowArms(!stand.dummy());
                    EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
                    for (int i = 0; i < slots.length && i < stand.gear().size(); i++) {
                        entity.setItemSlot(slots[i], stand.gear().get(i).copy());
                    }
                    entity.setCustomName(stand.name());
                    // Die Puppe zeigt ueber dem Kopf ihre Schadenszahlen; ihr Name steht auf dem Schild davor.
                    entity.setCustomNameVisible(!stand.dummy());
                    if (!stand.dummy()) {
                        McVersion.setInvulnerable(entity, true);
                    }
                    level.addFreshEntity(entity);
                    entities++;
                    count++;
                }
                default -> {
                }
            }
        }
        return new int[]{count, entities};
    }

    /**
     * TZ38: spawnt einen Mob ohne KI, unverwundbar, still und dauerhaft, mit dem Tag {@link OrderAreas#MOB_TAG}; false, wenn
     * der Typ fehlt oder sich nicht erzeugen laesst.
     */
    static boolean spawnMob(ServerLevel level, TcOp.Mob mob) {
        net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(mob.entity()).orElse(null);
        Entity entity = type == null ? null : type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (entity == null || entity instanceof ArmorStand) {
            if (entity != null) {
                entity.discard();
            }
            return false;
        }
        entity.setPos(mob.pos().getX() + 0.5, mob.pos().getY(), mob.pos().getZ() + 0.5);
        entity.setYRot(mob.yaw());
        entity.setYHeadRot(mob.yaw());
        if (entity instanceof net.minecraft.world.entity.Mob living) {
            living.setNoAi(true);
            living.setPersistenceRequired();
            living.setYBodyRot(mob.yaw());
        }
        entity.setSilent(true);
        McVersion.setInvulnerable(entity, true);
        entity.addTag(OrderAreas.MOB_TAG);
        return level.addFreshEntity(entity);
    }

    /**
     * a-fix13 (TZ38D, Besitzer 13.11.: ein Befehlsblock, der alle Mobs ohne KI beschwoert): spawnt jeden {@link TcOp.Mob}-Schritt
     * von {@code ops} an seinem Platz, wie der Bau ({@link #spawnMob}). Was dort schon steht (gleicher Typ mit dem Tag
     * {@link OrderAreas#MOB_TAG} im Block des Schritts), bleibt - ein zweiter Druck verdoppelt nichts. Die Chunks werden geladen,
     * der Knopf steht ja weit weg von den Mobs. Liefert die Zahl der neu beschworenen Mobs.
     */
    public static int spawnMobs(ServerLevel level, Iterable<? extends TcOp> ops) {
        int spawned = 0;
        for (TcOp op : ops) {
            if (!(op instanceof TcOp.Mob mob)) {
                continue;
            }
            level.getChunkAt(mob.pos());
            if (!stillMobsAt(level, mob).isEmpty()) {
                continue;
            }
            if (spawnMob(level, mob)) {
                spawned++;
            }
        }
        return spawned;
    }

    /** Wie {@link #spawnMobs(ServerLevel, Iterable)}, fuer alle Abschnitte einer Planung. */
    public static int spawnMobs(ServerLevel level, TestCentreLayout.Plan plan) {
        return spawnMobs(level, mobOps(plan));
    }

    /**
     * a-fix13: entfernt alle Entities mit dem Tag {@link OrderAreas#MOB_TAG} in den Chunks der {@link TcOp.Mob}-Schritte von
     * {@code ops} (ohne Drops und Erfahrung, {@code discard}); alles ohne den Tag bleibt. Liefert die Zahl der entfernten.
     */
    public static int removeMobs(ServerLevel level, Iterable<? extends TcOp> ops) {
        java.util.Set<net.minecraft.world.level.ChunkPos> chunks = new java.util.LinkedHashSet<>();
        for (TcOp op : ops) {
            if (op instanceof TcOp.Mob mob) {
                chunks.add(net.minecraft.world.level.ChunkPos.containing(mob.pos()));
            }
        }
        java.util.Set<Entity> found = new java.util.LinkedHashSet<>();
        for (net.minecraft.world.level.ChunkPos chunk : chunks) {
            level.getChunk(chunk.x(), chunk.z());
            AABB column = new AABB(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX() + 1, level.getMaxY() + 1, chunk.getMaxBlockZ() + 1);
            found.addAll(level.getEntitiesOfClass(Entity.class, column, entity -> entity.entityTags().contains(OrderAreas.MOB_TAG)));
        }
        found.forEach(Entity::discard);
        return found.size();
    }

    /** Wie {@link #removeMobs(ServerLevel, Iterable)}, fuer alle Abschnitte einer Planung. */
    public static int removeMobs(ServerLevel level, TestCentreLayout.Plan plan) {
        return removeMobs(level, mobOps(plan));
    }

    static List<TcOp> mobOps(TestCentreLayout.Plan plan) {
        List<TcOp> out = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Mob) {
                    out.add(op);
                }
            }
        }
        return out;
    }

    /** Die schon stehenden Mobs eines Schritts: gleicher Typ, Tag {@link OrderAreas#MOB_TAG}, im Block des Schritts. */
    private static List<Entity> stillMobsAt(ServerLevel level, TcOp.Mob mob) {
        net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getOptional(mob.entity()).orElse(null);
        return type == null ? List.of() : level.getEntitiesOfClass(Entity.class, new AABB(mob.pos()),
                entity -> entity.getType() == type && entity.entityTags().contains(OrderAreas.MOB_TAG));
    }

    // ------------------------------------------------------------------ a-fix13: Mob-Knoepfe als kurzer Auftrag ueber mehrere Ticks

    /**
     * Der Knopf steht weit von den Mobs: deren Chunks sind oft nicht geladen, und die Entities eines Chunks kommen erst einige Ticks
     * nach dem Chunk (Entity-Speicher laedt asynchron; vorher wuerde "beschwoeren" die schon gespeicherten Mobs doppeln und "entfernen"
     * sie nicht finden). Darum erzwingt der Auftrag die Chunks, wartet auf {@code areEntitiesLoaded}, arbeitet dann in einem Tick und
     * gibt die Chunks wieder frei; nach {@link #MOB_JOB_TIMEOUT_TICKS} Ticks mit dem, was geladen ist.
     */
    static final int MOB_JOB_TIMEOUT_TICKS = 400;

    private record MobJob(ServerLevel level, List<TcOp> mobs, boolean spawn, List<net.minecraft.world.level.ChunkPos> chunks,
                          List<net.minecraft.world.level.ChunkPos> forced, int deadline, java.util.function.IntConsumer done) {
    }

    private static MobJob pendingMobs;

    /** Ob ein Mob-Auftrag noch laeuft (es laeuft hoechstens einer). */
    public static boolean mobJobPending() {
        return pendingMobs != null;
    }

    /**
     * Stellt einen Auftrag ein: {@code spawn} beschwoert alle {@link TcOp.Mob}-Schritte von {@code ops}, sonst entfernt es die
     * stillen Mobs dort ({@link #spawnMobs}/{@link #removeMobs}); {@code done} bekommt die Zahl, im selben Tick vor der Freigabe der
     * Chunks. False, wenn schon ein Auftrag laeuft.
     */
    public static boolean queueMobs(ServerLevel level, Iterable<? extends TcOp> ops, boolean spawn, java.util.function.IntConsumer done) {
        if (pendingMobs != null) {
            return false;
        }
        List<TcOp> mobs = new ArrayList<>();
        java.util.Set<net.minecraft.world.level.ChunkPos> chunks = new java.util.LinkedHashSet<>();
        for (TcOp op : ops) {
            if (op instanceof TcOp.Mob mob) {
                mobs.add(mob);
                chunks.add(net.minecraft.world.level.ChunkPos.containing(mob.pos()));
            }
        }
        List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
        for (net.minecraft.world.level.ChunkPos chunk : chunks) {
            if (level.setChunkForced(chunk.x(), chunk.z(), true)) {
                forced.add(chunk); // only what this job forced is released again
            }
        }
        pendingMobs = new MobJob(level, mobs, spawn, List.copyOf(chunks), forced,
                level.getServer().getTickCount() + MOB_JOB_TIMEOUT_TICKS, done);
        return true;
    }

    /** Jeden Server-Tick ({@link TestCentreCommand#serverTick}): ein wartender Mob-Auftrag laeuft, sobald alle Chunks ihre Entities haben. */
    static void tickMobJobs(net.minecraft.server.MinecraftServer server) {
        MobJob job = pendingMobs;
        if (job == null) {
            return;
        }
        boolean sameServer = job.level().getServer() == server;
        boolean ready = sameServer && job.chunks().stream().allMatch(chunk -> job.level().areEntitiesLoaded(chunk.pack()));
        if (sameServer && !ready && server.getTickCount() < job.deadline()) {
            return;
        }
        pendingMobs = null;
        try {
            if (sameServer) {
                int count = job.spawn() ? spawnMobs(job.level(), job.mobs()) : removeMobs(job.level(), job.mobs());
                job.done().accept(count);
            }
        } finally {
            for (net.minecraft.world.level.ChunkPos chunk : job.forced()) {
                job.level().setChunkForced(chunk.x(), chunk.z(), false);
            }
        }
    }

    /**
     * Setzt eine Vorlage wie ein Pool-Teil, unrotiert: Strukturbloecke fallen weg, Jigsaw-Bloecke werden durch ihren
     * Endzustand ersetzt, Entities (etwa der Rahmen im Wrack) und Beutetabellen kommen mit; leise wie der Rest des Baus.
     */
    static boolean placeStructure(ServerLevel level, TcOp.Structure op) {
        java.util.Optional<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate> template =
                McVersion.structureTemplates(level).get(op.template());
        if (template.isEmpty()) {
            Simplebuilding.LOGGER.warn("Test centre: structure template {} is missing", op.template());
            return false;
        }
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings =
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                        .setIgnoreEntities(false)
                        .addProcessor(net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor.STRUCTURE_BLOCK)
                        .addProcessor(net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor.INSTANCE);
        return template.get().placeInWorld(level, op.pos(), op.pos(), settings,
                net.minecraft.util.RandomSource.create(op.pos().asLong()), QUIET);
    }

    private static void spawnFrame(ServerLevel level, BlockPos pos, Direction facing, ItemStack stack) {
        ItemFrame entity = new ItemFrame(level, pos, facing);
        entity.setItem(stack.copy(), false);
        McVersion.setInvulnerable(entity, true);
        level.addFreshEntity(entity);
    }

    /** Ein Oktant mit gesetzter Quader-Auswahl (dieselben Schluessel, die der Oktant selbst schreibt). */
    static ItemStack octant(BlockPos cornerA, BlockPos cornerB) {
        return octant(cornerA, cornerB, "CUBOID");
    }

    /** Wie oben, mit Figur; Ausrichtung +Y (so zeigt ein Prisma seine Spitze nach oben: Dachmodus). */
    static ItemStack octant(BlockPos cornerA, BlockPos cornerB, String shape) {
        ItemStack octant = new ItemStack(com.simplebuilding.util.BuildingToolRefs.octant());
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
        nbt.putIntArray("Pos2", new int[]{cornerB.getX(), cornerB.getY(), cornerB.getZ()});
        nbt.putString("Shape", shape);
        nbt.putInt("Orientation", 1);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return octant;
    }

    private static void placeSign(ServerLevel level, BlockPos pos, Direction facing, List<net.minecraft.network.chat.Component> lines) {
        placeSign(level, pos, facing, lines, false);
    }

    /** Wandschild (haengt am Block hinter sich) oder Steh-Schild auf dem Boden (TZ38E-10), Schrift nach {@code facing}. */
    private static void placeSign(ServerLevel level, BlockPos pos, Direction facing, List<net.minecraft.network.chat.Component> lines,
                                  boolean standing) {
        level.setBlock(pos, standing
                ? Blocks.OAK_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION,
                        net.minecraft.world.level.block.state.properties.RotationSegment.convertToSegment(facing))
                : Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), QUIET);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            McVersion.setSignFrontText(sign, lines);
            sign.setWaxed(true);
        }
    }

    private static void placeCommand(ServerLevel level, TcOp.Command command) {
        BlockPos pos = command.pos();
        Direction facing = command.facing();
        level.setBlock(pos, Blocks.COMMAND_BLOCK.defaultBlockState().setValue(CommandBlock.FACING, facing), QUIET);
        if (level.getBlockEntity(pos) instanceof CommandBlockEntity entity) {
            entity.getCommandBlock().setCommand(command.command());
            entity.setChanged();
        }
        // Der Kantenstein darueber ist ein eigener Place-Schritt (TcCanvas#command) und steht schon.
        level.setBlock(pos.relative(facing), Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, facing), QUIET);
        placeSign(level, pos.above().relative(facing), facing, command.label());
    }
}
