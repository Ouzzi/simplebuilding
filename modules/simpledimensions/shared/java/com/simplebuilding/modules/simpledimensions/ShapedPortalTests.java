package com.simplebuilding.modules.simpledimensions;

import dev.simpledimension.common.portal.ArchPortalScanner;
import dev.simpledimension.common.portal.DimensionPortalConfig;
import dev.simpledimension.common.portal.PortalActivationService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * N33 GameTests (Fabric catalogue; the block is registered on Fabric only so far): nether portals in arch shape -
 * valid and invalid forms, colour (own darker block vs Vanilla), breaking, travel there and back (POI), the server
 * switch, and the per-recipe shape rules of the own dimension portals.
 */
public final class ShapedPortalTests {

    private ShapedPortalTests() {
    }

    /** A drawing in one vertical plane: rows top to bottom, 'O' obsidian, '#' dirt, '.' or ' ' air; bottom-left at base. */
    record Shape(GameTestHelper h, BlockPos base, Direction.Axis axis, String... rows) {
        BlockPos at(int col, int row) {
            BlockPos rel = base.offset(axis == Direction.Axis.X ? col : 0, rows.length - 1 - row, axis == Direction.Axis.Z ? col : 0);
            return h.absolutePos(rel);
        }

        List<BlockPos> cells(char symbol) {
            List<BlockPos> out = new ArrayList<>();
            for (int row = 0; row < rows.length; row++) {
                for (int col = 0; col < rows[row].length(); col++) {
                    if (rows[row].charAt(col) == symbol) {
                        out.add(at(col, row));
                    }
                }
            }
            return out;
        }

        /** Clear a box around the drawing (one block in front and behind) and place it. */
        Shape build() {
            ServerLevel l = h.getLevel();
            int width = 0;
            for (String row : rows) {
                width = Math.max(width, row.length());
            }
            for (int col = -1; col <= width; col++) {
                for (int row = -1; row <= rows.length; row++) {
                    for (int depth = -1; depth <= 1; depth++) {
                        BlockPos p = at(col, row).offset(axis == Direction.Axis.Z ? depth : 0, 0, axis == Direction.Axis.X ? depth : 0);
                        l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            for (int row = 0; row < rows.length; row++) {
                for (int col = 0; col < rows[row].length(); col++) {
                    char c = rows[row].charAt(col);
                    if (c == 'O' || c == '#') {
                        l.setBlock(at(col, row), (c == 'O' ? Blocks.OBSIDIAN : Blocks.DIRT).defaultBlockState(), 3);
                    }
                }
            }
            return this;
        }
    }

    private static void yes(GameTestHelper h, boolean condition, String message) {
        h.assertTrue(condition, message);
    }

    private static void requireBlock(GameTestHelper h) {
        yes(h, DimensionRegistry.SHAPED != null && BuiltInRegistries.BLOCK.containsKey(DimensionRegistry.id("shaped_nether_portal")),
                "simpledimensions:shaped_nether_portal registered");
        yes(h, DimensionRuntime.get(h.getLevel().getServer()).settings.shapedNetherPortals, "Arches are on by default (F3=A)");
    }

    /** Fire at the cell, as flint and steel, a fire charge or spreading fire would place it. */
    private static void light(GameTestHelper h, BlockPos cell) {
        h.getLevel().setBlock(cell, Blocks.FIRE.defaultBlockState(), 3);
    }

    private static boolean all(GameTestHelper h, List<BlockPos> cells, Block block, Direction.Axis axis) {
        for (BlockPos p : cells) {
            BlockState s = h.getLevel().getBlockState(p);
            if (!s.is(block) || s.getValue(NetherPortalBlock.AXIS) != axis) {
                return false;
            }
        }
        return true;
    }

    private static boolean none(GameTestHelper h, List<BlockPos> cells) {
        for (BlockPos p : cells) {
            BlockState s = h.getLevel().getBlockState(p);
            if (s.is(DimensionRegistry.SHAPED) || s.is(Blocks.NETHER_PORTAL)) {
                return false;
            }
        }
        return true;
    }

    private static String key(Optional<Component> text) {
        return text.map(c -> c.getContents() instanceof TranslatableContents t ? t.getKey() : "").orElse("none");
    }

    private static Shape round(GameTestHelper h, BlockPos base, Direction.Axis axis) {
        return new Shape(h, base, axis, "  OOO  ", " O...O ", "O.....O", "O.....O", "O.....O", "OOOOOOO").build();
    }

    /** Valid arches become the darker shaped portal on both axes; the 1 × 2 minimum too; a Vanilla rectangle stays Vanilla. */
    public static void archShapes(GameTestHelper h) {
        requireBlock(h);
        Shape round = round(h, new BlockPos(1, 1, 2), Direction.Axis.X);
        light(h, round.at(3, 4));
        yes(h, all(h, round.cells('.'), DimensionRegistry.SHAPED, Direction.Axis.X), "Round arch lit as shaped portal (X)");
        Shape stairs = new Shape(h, new BlockPos(2, 1, 8), Direction.Axis.Z, "OO   ", "O.O  ", "O..O ", "O...O", "OOOOO").build();
        light(h, stairs.at(1, 3));
        yes(h, all(h, stairs.cells('.'), DimensionRegistry.SHAPED, Direction.Axis.Z), "Stepped arch lit as shaped portal (Z)");
        Shape twin = new Shape(h, new BlockPos(10, 1, 2), Direction.Axis.X, " OO OO ", "O..O..O", "O.....O", "O.....O", "OOOOOOO").build();
        light(h, twin.at(5, 3));
        yes(h, all(h, twin.cells('.'), DimensionRegistry.SHAPED, Direction.Axis.X), "Double arch lit");
        Shape smallest = new Shape(h, new BlockPos(20, 1, 2), Direction.Axis.X, " O ", "O.O", "O.O", " O ").build();
        light(h, smallest.at(1, 2));
        yes(h, all(h, smallest.cells('.'), DimensionRegistry.SHAPED, Direction.Axis.X), "Smallest portal 1 x 2 (F1=B)");
        Shape vanilla = new Shape(h, new BlockPos(20, 1, 10), Direction.Axis.X, "OOOO", "O..O", "O..O", "O..O", "OOOO").build();
        light(h, vanilla.at(1, 3));
        yes(h, all(h, vanilla.cells('.'), Blocks.NETHER_PORTAL, Direction.Axis.X), "Vanilla rectangle stays the Vanilla portal (normal colour)");
        h.succeed();
    }

    /** Forms against the rules light nothing; the action bar names the reason. */
    public static void archInvalid(GameTestHelper h) {
        requireBlock(h);
        Shape bump = new Shape(h, new BlockPos(1, 1, 2), Direction.Axis.X, "OOOOO", "O...O", "O.O.O", "OOOOO").build();
        Shape shelf = new Shape(h, new BlockPos(9, 1, 2), Direction.Axis.X, "OOOOO", "O...O", "OO..O", "O...O", "OOOOO").build();
        Shape gap = new Shape(h, new BlockPos(17, 1, 2), Direction.Axis.X, "OOOO", "O..#", "O..O", "OOOO").build();
        Shape open = new Shape(h, new BlockPos(1, 1, 10), Direction.Axis.X, "O..O", "O..O", "OOOO").build();
        Shape single = new Shape(h, new BlockPos(9, 1, 10), Direction.Axis.X, " O ", "O.O", " O ").build();
        ServerLevel l = h.getLevel();
        yes(h, key(ShapedNetherPortals.reason(l, bump.at(1, 2))).equals("simpledimensions.portal_shape.bottom"), "Bump in the floor explained");
        yes(h, key(ShapedNetherPortals.reason(l, shelf.at(2, 3))).equals("simpledimensions.portal_shape.sides"), "Shelf explained");
        yes(h, key(ShapedNetherPortals.reason(l, gap.at(1, 2))).equals("simpledimensions.portal_shape.gap"), "Foreign block explained");
        yes(h, key(ShapedNetherPortals.reason(l, open.at(1, 1))).equals("simpledimensions.portal_shape.open"), "Open frame explained");
        yes(h, key(ShapedNetherPortals.reason(l, single.at(1, 1))).equals("simpledimensions.portal_shape.too_small"), "1 x 1 explained");
        BlockPos field = h.absolutePos(new BlockPos(20, 3, 20));
        l.setBlock(field.below(), Blocks.OBSIDIAN.defaultBlockState(), 3);
        yes(h, ShapedNetherPortals.reason(l, field).isEmpty(), "Fire on a lone obsidian block stays silent");
        for (Shape s : List.of(bump, shelf, gap, open, single)) {
            light(h, s.cells('.').getLast());
            yes(h, none(h, s.cells('.')), "Invalid form lights no portal: " + String.join("/", s.rows()));
        }
        Component open21 = ShapedNetherPortals.text(ArchPortalScanner.Verdict.OPEN, ShapedNetherPortals.SCANNER);
        yes(h, open21.getContents() instanceof TranslatableContents t && t.getArgs().length == 2 && t.getArgs()[0].equals(21), "Open text names 21 x 21");
        h.succeed();
    }

    /** Removing one frame block dissolves the whole arch (like Vanilla); a block in front of it does not. */
    public static void archBreak(GameTestHelper h) {
        requireBlock(h);
        Shape round = round(h, new BlockPos(2, 1, 4), Direction.Axis.X);
        light(h, round.at(3, 4));
        List<BlockPos> cells = round.cells('.');
        yes(h, all(h, cells, DimensionRegistry.SHAPED, Direction.Axis.X), "Arch lit");
        ServerLevel l = h.getLevel();
        BlockPos front = round.at(3, 4).north();
        l.setBlock(front, Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(front, Blocks.AIR.defaultBlockState(), 3);
        yes(h, all(h, cells, DimensionRegistry.SHAPED, Direction.Axis.X), "A block in front leaves the portal");
        l.setBlock(round.at(1, 1), Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos p : cells) {
            yes(h, l.getBlockState(p).isAir(), "Broken frame dissolves every cell, " + p);
        }
        h.succeed();
    }

    /** There (nether, Vanilla exit) and back: the overworld search finds the arch as a nether portal (POI). */
    public static void archTravel(GameTestHelper h) {
        requireBlock(h);
        Shape round = round(h, new BlockPos(4, 1, 6), Direction.Axis.X);
        light(h, round.at(3, 4));
        BlockPos cell = round.at(3, 4);
        yes(h, h.getLevel().getBlockState(cell).is(DimensionRegistry.SHAPED), "Arch lit");
        // POI records are added by a server task after the block change: check a few ticks later.
        h.runAfterDelay(5, () -> {
            ServerLevel overworld = h.getLevel();
            Optional<BlockPos> found = overworld.getPortalForcer().findClosestPortalPosition(cell, false, overworld.getWorldBorder());
            yes(h, found.isPresent() && overworld.getBlockState(found.get()).is(DimensionRegistry.SHAPED), "Exit search finds the arch as nether portal");
            var traveller = EntityTypes.PIG.create(overworld, EntitySpawnReason.COMMAND);
            traveller.setPos(cell.getX() + .5, cell.getY(), cell.getZ() + .5);
            var there = DimensionRegistry.SHAPED.getPortalDestination(overworld, traveller, cell);
            yes(h, there != null && there.newLevel().dimension() == Level.NETHER, "Arch leads to the nether");
            ServerLevel nether = there.newLevel();
            var back = EntityTypes.PIG.create(nether, EntitySpawnReason.COMMAND);
            back.setPos(cell.getX() / 8.0, cell.getY(), cell.getZ() / 8.0);
            var home = ((NetherPortalBlock) Blocks.NETHER_PORTAL).getPortalDestination(nether, back, BlockPos.containing(back.position()));
            yes(h, home != null && home.newLevel().dimension() == Level.OVERWORLD, "Vanilla portal leads back to the overworld");
            yes(h, home.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(cell)) < 6, "Return lands in the arch, not in a new portal: " + home.position());
            h.succeed();
        });
    }

    /** Server switch off: fire in an arch stays fire (no portal); on again lights it. */
    public static void archSwitch(GameTestHelper h) {
        requireBlock(h);
        DimensionSettings settings = DimensionRuntime.get(h.getLevel().getServer()).settings;
        Shape round = round(h, new BlockPos(2, 1, 4), Direction.Axis.X);
        try {
            settings.shapedNetherPortals = false;
            light(h, round.at(3, 4));
            yes(h, none(h, round.cells('.')), "Switch off: no arch portal");
            yes(h, ShapedNetherPortals.reason(h.getLevel(), round.at(2, 4)).isEmpty(), "Switch off: no reason either");
        } finally {
            settings.shapedNetherPortals = true;
        }
        h.getLevel().setBlock(round.at(3, 4), Blocks.AIR.defaultBlockState(), 3);
        light(h, round.at(3, 4));
        yes(h, all(h, round.cells('.'), DimensionRegistry.SHAPED, Direction.Axis.X), "Switch on: arch lit");
        h.succeed();
    }

    /** Own dimension portals: the shape rules of a definition (fallback outline and recipe shape) on real blocks. */
    public static void shapeRules(GameTestHelper h) {
        Shape arch = new Shape(h, new BlockPos(2, 1, 4), Direction.Axis.X, "  OOO  ", " O...O ", "O.....O", "O.....O", "OOOOOOO").build();
        Shape box = new Shape(h, new BlockPos(12, 1, 4), Direction.Axis.Z, "OOOO", "O..O", "O..O", "O..O", "OOOO").build();
        ServerLevel l = h.getLevel();
        for (Shape s : List.of(arch, box)) {
            for (BlockPos p : s.cells('O')) {
                l.setBlock(p, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            }
        }
        DimensionPortalConfig c = new DimensionPortalConfig();
        c.id = "shape_test";
        c.targetDimensionId = "simpledimensions:shape_test";
        c.portalRecipes = new ArrayList<>();
        ConfigLimits.validate(c);
        BlockPos inArch = arch.at(3, 3), inBox = box.at(1, 3);
        yes(h, PortalActivationService.match(new PortalWorld(l, c), c, inArch.getX(), inArch.getY(), inArch.getZ()).isPresent(), "Default outline any: arch (F4=A)");
        yes(h, PortalActivationService.match(new PortalWorld(l, c), c, inBox.getX(), inBox.getY(), inBox.getZ()).isPresent(), "Default outline any: rectangle");
        c.portalShape = "rectangle";
        yes(h, PortalActivationService.match(new PortalWorld(l, c), c, inArch.getX(), inArch.getY(), inArch.getZ()).isEmpty(), "rectangle refuses the arch");
        yes(h, PortalActivationService.explain(new PortalWorld(l, c), c, inArch.getX(), inArch.getY(), inArch.getZ()).map(r -> r.verdict()).orElse(null) == ArchPortalScanner.Verdict.RECTANGLE_ONLY, "Reason: rectangle only");
        c.portalShape = "arch";
        yes(h, PortalActivationService.match(new PortalWorld(l, c), c, inBox.getX(), inBox.getY(), inBox.getZ()).isEmpty(), "arch refuses the rectangle");
        yes(h, PortalActivationService.match(new PortalWorld(l, c), c, inArch.getX(), inArch.getY(), inArch.getZ()).isPresent(), "arch accepts the arch");
        // A recipe with a shape: free form of its own frame block within its sizes, next to a pattern.
        DimensionPortalConfig r = DimensionPortalConfig.defaultSkyblock();
        var recipe = new DimensionPortalConfig.PortalRecipeConfig();
        recipe.shape = "any";
        recipe.frame = "minecraft:crying_obsidian";
        recipe.maxWidth = 4;
        r.portalRecipes.add(recipe);
        ConfigLimits.validate(r);
        yes(h, PortalActivationService.match(new PortalWorld(l, r), r, inBox.getX(), inBox.getY(), inBox.getZ()).isPresent(), "Recipe shape any: 2-wide rectangle fits max 4");
        yes(h, PortalActivationService.match(new PortalWorld(l, r), r, inArch.getX(), inArch.getY(), inArch.getZ()).isEmpty(), "Recipe max width 4 refuses the 5-wide arch");
        var bad = DimensionPortalConfig.defaultSkyblock();
        var obsidian = new DimensionPortalConfig.PortalRecipeConfig();
        obsidian.shape = "arch";
        obsidian.frame = "minecraft:obsidian";
        bad.portalRecipes.add(obsidian);
        boolean refused;
        try {
            ConfigLimits.validate(bad);
            refused = false;
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        yes(h, refused, "Obsidian stays reserved for Vanilla (also in shape recipes)");
        var unknown = new DimensionPortalConfig();
        unknown.id = "shape_test";
        unknown.targetDimensionId = "simpledimensions:shape_test";
        unknown.portalShape = "circle";
        try {
            ConfigLimits.validate(unknown);
            refused = false;
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        yes(h, refused, "Unknown outline refused");
        h.succeed();
    }
}
