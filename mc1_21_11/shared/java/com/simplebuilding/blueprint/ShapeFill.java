package com.simplebuilding.blueprint;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.OctantShape;
import com.simplebuilding.util.WandPlacement;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

/**
 * Baustab (Haupthand) + Oktant mit vollstaendiger Auswahl (Nebenhand): der Stab fuellt die Figur des
 * Oktanten (Entscheidung des Besitzers, 2026-09-25). Die Vorschau zeigt die Geisterbloecke, sobald
 * beide gehalten werden, fehlendes Material rot; ein Klick auf einen beliebigen Block baut, mit der
 * Zwei-Klick-Regel, Staffelung, Haltbarkeit je Block und Abbruch beim Weglegen des Blaupausen-Baus
 * ({@link BlueprintBuilder#buildLayout}).
 *
 * <p><b>Fuellung:</b> Material ist der erste Baublock, den der Stab findet (die Nebenhand haelt ja
 * den Oktanten: Hotbar, mit Baumeister Inventar und Rucksack); mit Farbpalette je Stelle der Eintrag,
 * den {@link BuildingWandItem#paletteIndex} ihr zuweist. Die Bloecke werden gesetzt, als stellte man
 * sie von oben auf (Staemme stehen, Treppen zeigen in Blickrichtung, Stufen unten).
 * Die Optionen des Oktanten wirken:
 * <ul>
 *   <li><b>Hohl</b>: nur die Huelle (Stellen mit mindestens einem der sechs Nachbarn ausserhalb).</li>
 *   <li><b>Reihenfolge</b>: Standard = Schicht fuer Schicht von der Grundflaeche zur Spitze der Figur
 *       (ihre Ausrichtung), Von unten = aufwaerts, Von oben = abwaerts; in jeder Schicht von der Mitte
 *       nach aussen.</li>
 *   <li><b>Schichtmodus</b>: ein Klick baut nur die naechste noch nicht fertige Schicht dieser
 *       Reihenfolge.</li>
 * </ul>
 *
 * <p><b>Dach:</b> Ist das Material eine Treppe und die Figur ein Prisma oder eine Pyramide mit der
 * Spitze nach oben, entsteht statt der Fuellung ein Dach: nur die Dachflaechen (Stellen, deren
 * oberer Nachbar oder Nachbar in Gefaellerichtung ausserhalb liegt), jede Treppe mit der hohen Seite
 * zum First, Ecken passen sich an; ein nur einen Block breiter First bekommt untere Stufen derselben
 * Sorte ({@code oak_stairs} &rarr; {@code oak_slab}, sonst Treppen). Giebelwaende bleiben frei.
 *
 * <p><b>Grenzen:</b> die laengste Kante der Auswahl wie bei der Blaupause je Stabstufe
 * ({@link BlueprintTiers}); hoechstens {@link BlueprintCode#MAX_EXPANDED_CELLS} Stellen in der Box.
 * Nicht gespeichert: nach Logout ist ein laufender Fuell-Auftrag vorbei.
 */
public final class ShapeFill {
    private ShapeFill() {
    }

    /** Ergebnis der Planung: die Stellenliste, oder warum nicht. */
    public record Plan(BlueprintBuilder.ListLayout layout, Component problem, boolean roof) {
        static Plan fail(Component problem) {
            return new Plan(null, problem, false);
        }
    }

    /** Ist das ein Oktant mit beiden Ecken? */
    public static boolean hasSelection(ItemStack stack) {
        return stack.getItem() instanceof OctantItem && OctantShape.bounds(OctantShape.data(stack)) != null;
    }

    public static InteractionResult useWand(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack wand = context.getItemInHand();
        ItemStack octant = player.getOffhandItem();
        if (level.isClientSide()) {
            BlueprintBuilder.clientWarnClick(player, preview(level, player, wand, octant));
            return InteractionResult.SUCCESS;
        }
        Plan plan = plan(level, player, wand, octant);
        if (plan.problem() != null) {
            player.displayClientMessage(plan.problem(), true);
            return InteractionResult.FAIL;
        }
        // Vor dem Bau: Warnungen des Bauauftrags (fehlendes Material) ueberschreiben diesen Hinweis.
        Component hint = roofHint(player, wand, octant);
        if (hint != null) {
            player.displayClientMessage(hint, true);
        }
        BlueprintBuilder.Result result = BlueprintBuilder.buildLayout(level, player, wand, octant, plan.layout());
        if (result == null) {
            player.displayClientMessage(Component.translatable("simplebuilding.wand.shape.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    // =====================================================================================
    // PLANUNG
    // =====================================================================================

    public static Plan plan(Level level, Player player, ItemStack wand, ItemStack octant) {
        if (!(wand.getItem() instanceof BuildingWandItem wandItem) || !hasSelection(octant)) {
            return Plan.fail(Component.translatable("simplebuilding.wand.shape.no_selection").withStyle(ChatFormatting.RED));
        }
        CompoundTag nbt = OctantShape.data(octant);
        AABB bounds = OctantShape.bounds(nbt);
        int minX = (int) bounds.minX, minY = (int) bounds.minY, minZ = (int) bounds.minZ;
        int sx = (int) bounds.maxX - minX, sy = (int) bounds.maxY - minY, sz = (int) bounds.maxZ - minZ;
        int edge = Math.max(sx, Math.max(sy, sz));
        int limit = BlueprintTiers.edgeFor(wandItem);
        if (edge > limit) {
            int needed = BlueprintTiers.tierIndexFor(edge);
            return Plan.fail(Component.translatable("simplebuilding.blueprint.build.too_big", edge, limit,
                    BlueprintTiers.wandName(Math.max(0, needed))).withStyle(ChatFormatting.RED));
        }
        long volume = (long) sx * sy * sz;
        if (volume > BlueprintCode.MAX_EXPANDED_CELLS) {
            return Plan.fail(Component.translatable("simplebuilding.wand.shape.too_many", volume, BlueprintCode.MAX_EXPANDED_CELLS)
                    .withStyle(ChatFormatting.RED));
        }

        boolean palette = hasEnchantment(wand, level, ModEnchantments.COLOR_PALETTE);
        List<ItemStack> materials = new ArrayList<>();
        if (palette) {
            materials.addAll(BuildingWandItem.paletteStacks(player, wand));
        } else {
            ItemStack first = BuildingWandItem.firstBuildingStack(player, wand);
            if (first != null) {
                materials.add(first);
            }
        }
        if (materials.isEmpty()) {
            return Plan.fail(Component.translatable("simplebuilding.wand.shape.no_material").withStyle(ChatFormatting.RED));
        }

        OctantItem.SelectionShape shape = OctantShape.shape(nbt);
        Direction orientation = OctantShape.orientation(nbt);
        Predicate<BlockPos> inside = OctantShape.predicate(shape, orientation, bounds);
        // Dach: die Treppe ist der erste Baublock in Suchreihenfolge (Nebenhand, Hotbar, ...) - auch mit
        // Farbpalette. Frueher schaltete die Palette das Dach ab; der Enderit-Stab aus dem Kit traegt sie,
        // und statt eines Dachs kam eine bunt gefuellte Figur (Besitzer-Befund 2026-09-25).
        Block stairBlock = roofStair(player, wand, shape, orientation);
        boolean roof = stairBlock != null;
        boolean hollow = nbt.getBooleanOr("Hollow", false);
        // Prisma: Gefaelle quer zur laengeren Grundkante (wie OctantShape#isPointInPrism).
        boolean prismAlongZ = sz > sx;

        LongArrayList cells = new LongArrayList();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = minY; y < minY + sy; y++) {
            for (int z = minZ; z < minZ + sz; z++) {
                for (int x = minX; x < minX + sx; x++) {
                    p.set(x, y, z);
                    if (!inside.test(p)) {
                        continue;
                    }
                    if (roof ? !isRoofCell(inside, p, shape, prismAlongZ) : hollow && isInterior(inside, p)) {
                        continue;
                    }
                    cells.add(p.asLong());
                }
            }
        }

        // Reihenfolge: Schicht (entlang der Baurichtung), dann Abstand zur Mitte der Schicht.
        OctantItem.FillOrder order;
        try {
            order = OctantItem.FillOrder.valueOf(nbt.getStringOr("FillOrder", OctantItem.FillOrder.DEFAULT.name()));
        } catch (IllegalArgumentException e) {
            order = OctantItem.FillOrder.DEFAULT;
        }
        Direction buildDir = roof ? Direction.UP : switch (order) {
            case BOTTOM_UP -> Direction.UP;
            case TOP_DOWN -> Direction.DOWN;
            default -> orientation;
        };
        long[] sortable = new long[cells.size()];
        int twiceCx = 2 * minX + sx, twiceCy = 2 * minY + sy, twiceCz = 2 * minZ + sz;
        for (int i = 0; i < sortable.length; i++) {
            BlockPos c = BlockPos.of(cells.getLong(i));
            int layer = layerOf(c, buildDir, minX, minY, minZ, sx, sy, sz);
            long dx = 2L * c.getX() + 1 - twiceCx, dy = 2L * c.getY() + 1 - twiceCy, dz = 2L * c.getZ() + 1 - twiceCz;
            long dist = switch (buildDir.getAxis()) {
                case X -> dy * dy + dz * dz;
                case Y -> dx * dx + dz * dz;
                case Z -> dx * dx + dy * dy;
            };
            sortable[i] = ((long) layer << 44) | (Math.min(dist, 0x3FFFFFL) << 22) | i;
        }
        java.util.Arrays.sort(sortable);

        // Schichtmodus: nur die erste Schicht, in der noch etwas frei ist.
        int onlyLayer = -1;
        if (nbt.getBooleanOr("LayerMode", false)) {
            for (long key : sortable) {
                BlockPos c = BlockPos.of(cells.getLong((int) (key & 0x3FFFFFL)));
                if (level.getBlockState(c).canBeReplaced()) {
                    onlyLayer = (int) (key >>> 44);
                    break;
                }
            }
            if (onlyLayer < 0) {
                return new Plan(new BlueprintBuilder.ListLayout(new long[0], new BlockState[0]), null, roof);
            }
        }

        // Entities in der Box: nur Stellen, die eine davon beruehren, pruefen die Kollision genau.
        List<AABB> entityBoxes = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, bounds)) {
            entityBoxes.add(entity.getBoundingBox());
        }
        Block slab = roof ? slabFor(stairBlock) : null;
        BlockState[] base = new BlockState[materials.size()];
        LongArrayList outPos = new LongArrayList();
        List<BlockState> outStates = new ArrayList<>();
        for (long key : sortable) {
            if (onlyLayer >= 0 && (int) (key >>> 44) != onlyLayer) {
                continue;
            }
            BlockPos c = BlockPos.of(cells.getLong((int) (key & 0x3FFFFFL)));
            BlockState state;
            if (roof) {
                state = roofState(inside, c, shape, prismAlongZ, bounds, stairBlock, slab);
            } else {
                int m = palette ? BuildingWandItem.paletteIndex(c, materials.size()) : 0;
                if (base[m] == null) {
                    base[m] = WandPlacement.baseState(level, player, materials.get(m), c, Direction.UP, WandPlacement.TOP_CENTER, null);
                    if (base[m] == null) {
                        base[m] = ((BlockItem) materials.get(m).getItem()).getBlock().defaultBlockState();
                    }
                }
                state = base[m];
                if (!state.canSurvive(level, c)) {
                    continue;
                }
            }
            if (touchesEntity(entityBoxes, c) && !level.isUnobstructed(state, c, CollisionContext.placementContext(player))) {
                continue;
            }
            outPos.add(c.asLong());
            outStates.add(state);
        }
        return new Plan(new BlueprintBuilder.ListLayout(outPos.toLongArray(), outStates.toArray(new BlockState[0])), null, roof);
    }

    /** Figur mit Spitze nach oben, die ein Dach werden kann (Prisma oder Pyramide)? */
    static boolean isRoofShape(OctantItem.SelectionShape shape, Direction orientation) {
        return orientation == Direction.UP
                && (shape == OctantItem.SelectionShape.TRIANGLE || shape == OctantItem.SelectionShape.PYRAMID);
    }

    /** Die Treppe fuer den Dachmodus, oder {@code null} (keine Dachfigur, oder der erste Baublock ist keine Treppe). */
    static Block roofStair(Player player, ItemStack wand, OctantItem.SelectionShape shape, Direction orientation) {
        if (!isRoofShape(shape, orientation)) {
            return null;
        }
        ItemStack first = BuildingWandItem.firstBuildingStack(player, wand);
        return first != null && first.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StairBlock ? bi.getBlock() : null;
    }

    /**
     * Aktionsleisten-Hinweis zum Dachmodus: bei einer Dachfigur entweder, dass ein Dach entsteht (Treppe
     * und First), oder warum nicht (der erste Baublock ist keine Treppe). Bei anderen Figuren {@code null}.
     */
    public static Component roofHint(Player player, ItemStack wand, ItemStack octant) {
        if (!hasSelection(octant)) {
            return null;
        }
        CompoundTag nbt = OctantShape.data(octant);
        OctantItem.SelectionShape shape = OctantShape.shape(nbt);
        Direction orientation = OctantShape.orientation(nbt);
        if (!isRoofShape(shape, orientation)) {
            return null;
        }
        Block stair = roofStair(player, wand, shape, orientation);
        if (stair == null) {
            ItemStack first = BuildingWandItem.firstBuildingStack(player, wand);
            Component found = first == null ? Component.literal("-") : first.getHoverName();
            return Component.translatable("simplebuilding.wand.roof.needs_stairs", found).withStyle(ChatFormatting.YELLOW);
        }
        Block slab = slabFor(stair);
        return Component.translatable("simplebuilding.wand.roof.active", stair.getName(),
                (slab != null ? slab : stair).getName()).withStyle(ChatFormatting.AQUA);
    }

    private static int layerOf(BlockPos c, Direction dir, int minX, int minY, int minZ, int sx, int sy, int sz) {
        return switch (dir) {
            case UP -> c.getY() - minY;
            case DOWN -> minY + sy - 1 - c.getY();
            case EAST -> c.getX() - minX;
            case WEST -> minX + sx - 1 - c.getX();
            case SOUTH -> c.getZ() - minZ;
            case NORTH -> minZ + sz - 1 - c.getZ();
        };
    }

    private static boolean touchesEntity(List<AABB> boxes, BlockPos c) {
        if (boxes.isEmpty()) {
            return false;
        }
        AABB cell = new AABB(c);
        for (AABB box : boxes) {
            if (box.intersects(cell)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInterior(Predicate<BlockPos> inside, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (!inside.test(p.relative(d))) {
                return false;
            }
        }
        return true;
    }

    private static List<Direction> slopeDirections(OctantItem.SelectionShape shape, boolean prismAlongZ) {
        if (shape == OctantItem.SelectionShape.PYRAMID) {
            return List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);
        }
        return prismAlongZ ? List.of(Direction.EAST, Direction.WEST) : List.of(Direction.SOUTH, Direction.NORTH);
    }

    /** Dachflaeche: oben frei, oder in Gefaellerichtung frei (steile Daecher). */
    private static boolean isRoofCell(Predicate<BlockPos> inside, BlockPos p, OctantItem.SelectionShape shape, boolean prismAlongZ) {
        if (!inside.test(p.above())) {
            return true;
        }
        for (Direction d : slopeDirections(shape, prismAlongZ)) {
            if (!inside.test(p.relative(d))) {
                return true;
            }
        }
        return false;
    }

    private static BlockState roofState(Predicate<BlockPos> inside, BlockPos c, OctantItem.SelectionShape shape, boolean prismAlongZ,
                                        AABB bounds, Block stair, Block slab) {
        boolean xRidge = !inside.test(c.east()) && !inside.test(c.west());
        boolean zRidge = !inside.test(c.south()) && !inside.test(c.north());
        boolean ridge = shape == OctantItem.SelectionShape.PYRAMID ? xRidge || zRidge : (prismAlongZ ? xRidge : zRidge);
        if (ridge && !inside.test(c.above())) {
            if (slab != null) {
                return slab.defaultBlockState();
            }
        }
        double cx = (bounds.minX + bounds.maxX) / 2.0, cz = (bounds.minZ + bounds.maxZ) / 2.0;
        double dx = c.getX() + 0.5 - cx, dz = c.getZ() + 0.5 - cz;
        boolean alongX;
        if (shape == OctantItem.SelectionShape.PYRAMID) {
            double rx = Math.max(0.5, (bounds.maxX - bounds.minX) / 2.0), rz = Math.max(0.5, (bounds.maxZ - bounds.minZ) / 2.0);
            alongX = Math.abs(dx) / rx >= Math.abs(dz) / rz;
        } else {
            alongX = prismAlongZ;
        }
        // Die hohe Seite der Treppe zeigt zum First.
        Direction facing = alongX ? (dx < 0 ? Direction.EAST : Direction.WEST) : (dz < 0 ? Direction.SOUTH : Direction.NORTH);
        return stair.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false);
    }

    /** Die Stufe zur Treppe: gleiche Id mit {@code _slab} statt {@code _stairs}, sonst {@code null}. */
    static Block slabFor(Block stair) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(stair);
        if (!id.getPath().endsWith("_stairs")) {
            return null;
        }
        Identifier slabId = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().replace("_stairs", "_slab"));
        Block slab = BuiltInRegistries.BLOCK.getValue(slabId);
        return slab instanceof SlabBlock ? slab : null;
    }

    // =====================================================================================
    // VORSCHAU (Client)
    // =====================================================================================

    private static Object layoutKey;
    private static Plan layoutCache;
    private static Object previewKey;
    private static BlueprintBuilder.Preview previewCache = BlueprintBuilder.Preview.EMPTY;

    /**
     * Die Geisterbloecke: was ein Klick jetzt setzen wuerde (simulierter Vorrat), fehlendes Material
     * rot. Die Stellenliste wird nur neu berechnet, wenn sich Auswahl, Material oder Blickrichtung
     * aendern (bei kleinen Figuren zusaetzlich alle zwei Sekunden, damit der Schichtmodus
     * weiterrueckt), die Materialpruefung alle vier Ticks.
     */
    public static BlueprintBuilder.Preview preview(Level level, Player player, ItemStack wand, ItemStack octant) {
        List<Object> materialKey = new ArrayList<>();
        if (hasEnchantment(wand, level, ModEnchantments.COLOR_PALETTE)) {
            for (ItemStack s : BuildingWandItem.paletteStacks(player, wand)) {
                materialKey.add(s.getItem());
            }
        } else {
            ItemStack first = BuildingWandItem.firstBuildingStack(player, wand);
            materialKey.add(first == null ? null : first.getItem());
        }
        CustomData data = octant.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        boolean small = layoutCache != null && layoutCache.layout() != null && layoutCache.layout().size() <= 262_144;
        List<Object> lKey = java.util.Arrays.asList(data, wand.getItem(), materialKey, player.getDirection(),
                small ? level.getGameTime() / 40 : 0L);
        if (!Objects.equals(lKey, layoutKey) || layoutCache == null) {
            layoutKey = lKey;
            layoutCache = plan(level, player, wand, octant);
        }
        List<Object> pKey = java.util.Arrays.asList(lKey, level.getGameTime() / 4, player.getAbilities().instabuild);
        if (Objects.equals(pKey, previewKey)) {
            return previewCache;
        }
        previewKey = pKey;
        if (layoutCache.layout() == null) {
            previewCache = BlueprintBuilder.Preview.EMPTY;
            return previewCache;
        }
        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        Map<BlockPos, BlockState> missing = new LinkedHashMap<>();
        BlueprintBuilder.Planner planner = new BlueprintBuilder.Planner(level, player, wand, layoutCache.layout(),
                BlueprintBuilder.simulatedSupply(player, wand), player.getAbilities().instabuild, true).onMissing((pos, state) -> {
                    if (missing.size() < BlueprintBuilder.MAX_PREVIEW) {
                        missing.put(pos, state);
                    }
                });
        planner.run(BlueprintBuilder.PREVIEW_VISITS, BlueprintBuilder.MAX_PREVIEW, (pos, state) -> {
            placed.put(pos, state);
            return true;
        });
        previewCache = new BlueprintBuilder.Preview(placed, missing);
        return previewCache;
    }
}
