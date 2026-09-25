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
        return new Result(done[0], done[1], System.currentTimeMillis() - start);
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
    static int[] apply(ServerLevel level, List<TcOp> ops) {
        int count = 0;
        int entities = 0;
        for (TcOp op : ops) {
            if (op instanceof TcOp.Place place) {
                level.setBlock(place.pos(), place.state(), QUIET);
                count++;
            }
        }
        for (TcOp op : ops) {
            switch (op) {
                case TcOp.Sign sign -> {
                    placeSign(level, sign.pos(), sign.facing(), sign.lines());
                    count++;
                }
                case TcOp.Fill fill -> {
                    if (level.getBlockEntity(fill.pos()) instanceof Container container) {
                        for (int i = 0; i < fill.contents().size() && i < container.getContainerSize(); i++) {
                            container.setItem(i, fill.contents().get(i).copy());
                        }
                        container.setChanged();
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
                    spawnFrame(level, frame.pos(), frame.facing(), octant(frame.cornerA(), frame.cornerB()));
                    entities++;
                    count++;
                }
                case TcOp.BlueprintFrame frame -> {
                    // Der gescannte Bereich steht schon: alle Bloecke kommen vor den Rahmen.
                    BlueprintScanner.Outcome outcome = BlueprintScanner.scanAtTable(level, frame.table(),
                            octant(frame.cornerA(), frame.cornerB()), new ItemStack(ModItems.BLUEPRINT));
                    if (outcome.error() != null) {
                        Simplebuilding.LOGGER.warn("Test centre blueprint scan refused: {}", outcome.error().getString());
                    }
                    spawnFrame(level, frame.pos(), frame.facing(),
                            outcome.error() == null ? outcome.result() : new ItemStack(ModItems.BLUEPRINT));
                    entities++;
                    count++;
                }
                case TcOp.Stand stand -> {
                    ArmorStand entity = new ArmorStand(level, stand.pos().getX() + 0.5, stand.pos().getY(), stand.pos().getZ() + 0.5);
                    entity.setYRot(stand.yaw());
                    entity.setYBodyRot(stand.yaw());
                    entity.setYHeadRot(stand.yaw());
                    entity.setShowArms(true);
                    EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
                    for (int i = 0; i < slots.length && i < stand.gear().size(); i++) {
                        entity.setItemSlot(slots[i], stand.gear().get(i).copy());
                    }
                    entity.setCustomName(stand.name());
                    entity.setCustomNameVisible(true);
                    McVersion.setInvulnerable(entity, true);
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

    private static void spawnFrame(ServerLevel level, BlockPos pos, Direction facing, ItemStack stack) {
        ItemFrame entity = new ItemFrame(level, pos, facing);
        entity.setItem(stack.copy(), false);
        McVersion.setInvulnerable(entity, true);
        level.addFreshEntity(entity);
    }

    /** Ein Oktant mit gesetzter Quader-Auswahl (dieselben Schluessel, die der Oktant selbst schreibt). */
    static ItemStack octant(BlockPos cornerA, BlockPos cornerB) {
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
        nbt.putIntArray("Pos2", new int[]{cornerB.getX(), cornerB.getY(), cornerB.getZ()});
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return octant;
    }

    private static void placeSign(ServerLevel level, BlockPos pos, Direction facing, List<net.minecraft.network.chat.Component> lines) {
        level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), QUIET);
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
        level.setBlock(pos.above(), TcCanvas.TRIM, QUIET);
        level.setBlock(pos.relative(facing), Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, facing), QUIET);
        placeSign(level, pos.above().relative(facing), facing, command.label());
    }
}
