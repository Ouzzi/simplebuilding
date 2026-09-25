package com.simplebuilding.blueprint;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.OctantShape;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Scannt die Auswahl eines Oktanten in eine Blaupause - am Kartentisch: Oktant oben, leere oder
 * unsignierte Blaupause unten, rechts kommt die gefuellte Blaupause heraus.
 *
 * <p>Regeln: gescannt wird genau die <b>Figur</b> des Oktanten (Quader, Zylinder, Kugel, Pyramide,
 * Prisma ...; {@link OctantShape}, dieselbe Quelle wie die Vorschau im Client). Ihre Bounding Box
 * ist hoechstens {@value BlueprintCode#GRID} Bloecke je Kante gross und geladen, und der Kartentisch
 * steht hoechstens {@value #MAX_TABLE_DISTANCE} Bloecke von ihr entfernt. Luft wird uebersprungen,
 * von Block-Entities wird nur der Zustand uebernommen (kein Truheninhalt). Die Koordinaten beginnen
 * bei der kleinsten belegten Stelle.
 *
 * <p>Grosse Auswahlen laufen als {@link Job} ueber mehrere Ticks ({@link #DEFAULT_BUDGET_PER_TICK} Stellen je
 * Tick); kleine (bis zu einem Tick-Budget) werden sofort fertig.
 */
public final class BlueprintScanner {
    public static final int MAX_TABLE_DISTANCE = 32;
    /** Gepruefte Stellen je Tick. Ein 256er-Wuerfel (16,7 Mio.) braucht damit 64 Ticks, gut 3 s. */
    public static final int DEFAULT_BUDGET_PER_TICK = 262_144;
    /**
     * Spieltest-Haken: traegt der Spieler dieses Tag, arbeiten Scan und Bau in winzigen Scheiben
     * (20 Stellen / 3 Bloecke je Tick), damit der Mehr-Tick-Weg an einer kleinen Auswahl pruefbar
     * ist - je Spieler, nicht global, weil Spieltests nebeneinander laufen.
     */
    public static final String SMALL_BUDGET_TAG = "simplebuilding.blueprint_small_budget";
    public static final int SMALL_BUDGET = 20;

    public static int budgetFor(net.minecraft.world.entity.player.Player player) {
        return smallBudget(player) ? SMALL_BUDGET : DEFAULT_BUDGET_PER_TICK;
    }

    public static boolean smallBudget(net.minecraft.world.entity.player.Player player) {
        return player != null && player.getTags().contains(SMALL_BUDGET_TAG);
    }

    private BlueprintScanner() {
    }

    /** Ergebnis des Scans: entweder eine gefuellte Blaupause oder ein Grund fuer die Ablehnung. */
    public record Outcome(ItemStack result, Component error) {
        static Outcome fail(String key, Object... args) {
            return new Outcome(ItemStack.EMPTY, Component.translatable("simplebuilding.blueprint.scan." + key, args));
        }
    }

    public static boolean isOctant(ItemStack stack) {
        return stack.getItem() instanceof OctantItem;
    }

    /** Leere oder unsignierte Blaupause: die kann der Kartentisch (neu) beschreiben. */
    public static boolean isWritableBlueprint(ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem
                && !stack.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY).signed();
    }

    public static Optional<BlockPos> corner(ItemStack octant, String key) {
        return Optional.ofNullable(OctantShape.corner(OctantShape.data(octant), key));
    }

    /**
     * Prueft die Voraussetzungen und liefert entweder sofort ein {@link Outcome} (Ablehnung) oder
     * einen {@link Job}, der den Scan in Scheiben erledigt.
     */
    public static Object start(Level level, BlockPos table, ItemStack octant, ItemStack blueprint) {
        if (!isOctant(octant) || !isWritableBlueprint(blueprint)) {
            return Outcome.fail("wrong_items");
        }
        AABB box = OctantShape.bounds(OctantShape.data(octant));
        if (box == null) {
            return Outcome.fail("no_selection");
        }
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX - 1, box.maxY - 1, box.maxZ - 1);
        int sx = max.getX() - min.getX() + 1, sy = max.getY() - min.getY() + 1, sz = max.getZ() - min.getZ() + 1;
        if (sx > BlueprintCode.GRID || sy > BlueprintCode.GRID || sz > BlueprintCode.GRID) {
            return Outcome.fail("too_large", sx, sy, sz, BlueprintCode.GRID);
        }
        if (!box.inflate(MAX_TABLE_DISTANCE).contains(table.getX() + 0.5, table.getY() + 0.5, table.getZ() + 0.5)) {
            return Outcome.fail("too_far", MAX_TABLE_DISTANCE);
        }
        if (!level.hasChunksAt(min, max)) {
            return Outcome.fail("not_loaded");
        }
        return new Job(min, max, OctantShape.of(octant), octant, blueprint);
    }

    /** Scan in einem Zug (Spieltests, kleine Auswahlen): {@link #start} und {@link Job#step} bis fertig. */
    public static Outcome scanAtTable(Level level, BlockPos table, ItemStack octant, ItemStack blueprint) {
        Object started = start(level, table, octant, blueprint);
        if (started instanceof Outcome outcome) {
            return outcome;
        }
        Job job = (Job) started;
        while (!job.step(level, Integer.MAX_VALUE)) {
            // step erledigt alles in einem Aufruf
        }
        return job.outcome();
    }

    /**
     * Ein laufender Scan: geht die Bounding Box Stelle fuer Stelle durch (y, dann z, dann x), nimmt
     * jede Nicht-Luft-Stelle innerhalb der Figur auf und baut am Ende die Blaupause.
     */
    public static final class Job {
        private final BlockPos min;
        private final BlockPos max;
        private final Predicate<BlockPos> shape;
        private final ItemStack octant;
        private final ItemStack blueprint;
        private final BlueprintModel model = new BlueprintModel();
        private final long total;
        private long cursor;
        private Outcome outcome;

        Job(BlockPos min, BlockPos max, Predicate<BlockPos> shape, ItemStack octant, ItemStack blueprint) {
            this.min = min;
            this.max = max;
            this.shape = shape;
            this.octant = octant.copy();
            this.blueprint = blueprint.copy();
            this.total = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }

        /** Stimmt der Tischinhalt noch mit dem ueberein, wofuer der Scan laeuft? */
        public boolean matches(ItemStack octantNow, ItemStack blueprintNow) {
            return ItemStack.isSameItemSameComponents(octant, octantNow) && ItemStack.isSameItemSameComponents(blueprint, blueprintNow);
        }

        public long total() {
            return total;
        }

        public int percent() {
            return total == 0 ? 100 : (int) (cursor * 100 / total);
        }

        public boolean done() {
            return outcome != null;
        }

        public Outcome outcome() {
            return outcome;
        }

        /** Prueft bis zu {@code budget} Stellen; {@code true}, sobald der Scan fertig (oder gescheitert) ist. */
        public boolean step(Level level, int budget) {
            if (outcome != null) {
                return true;
            }
            if (!level.hasChunksAt(min, max)) {
                outcome = Outcome.fail("not_loaded");
                return true;
            }
            int sx = max.getX() - min.getX() + 1;
            int sz = max.getZ() - min.getZ() + 1;
            long end = Math.min(total, cursor + Math.max(1, budget));
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (; cursor < end; cursor++) {
                int x = (int) (cursor % sx);
                int z = (int) ((cursor / sx) % sz);
                int y = (int) (cursor / ((long) sx * sz));
                pos.set(min.getX() + x, min.getY() + y, min.getZ() + z);
                if (!shape.test(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(Blocks.MOVING_PISTON)) {
                    continue;
                }
                model.set(x, y, z, state);
                if (model.size() > BlueprintCode.MAX_EXPANDED_CELLS) {
                    outcome = Outcome.fail("too_many_blocks", BlueprintCode.MAX_EXPANDED_CELLS);
                    return true;
                }
            }
            if (cursor < total) {
                return false;
            }
            outcome = finish();
            return true;
        }

        private Outcome finish() {
            if (model.isEmpty()) {
                return Outcome.fail("nothing");
            }
            String code = BlueprintCode.serialize(model.normalized());
            if (code.length() > BlueprintCode.MAX_CODE_LENGTH) {
                return Outcome.fail("too_complex", code.length(), BlueprintCode.MAX_CODE_LENGTH);
            }
            ItemStack result = blueprint.copyWithCount(1);
            BlueprintContent old = result.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
            result.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent(code, old.title(), "", false));
            result.remove(ModDataComponentTypes.BLUEPRINT_ROTATION);
            return new Outcome(result, null);
        }
    }
}
