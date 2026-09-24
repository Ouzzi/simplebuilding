package com.simplebuilding.blueprint;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.items.custom.BuildingWandItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Baut eine Blaupause mit dem Baustab: Baustab in der Haupthand, Blaupause in der Nebenhand,
 * Rechtsklick auf einen Block.
 *
 * <p>Ausrichtung: das Bauwerk steht vor dem Spieler, seine lokale z-Achse zeigt in Blickrichtung,
 * seine Mitte (x) liegt auf dem Zielblock, seine Unterkante auf dessen Hoehe. Blickt der Spieler
 * nach Sueden, entsteht das Bauwerk genau so, wie es gescannt wurde; Strg+Mausrad dreht es in
 * Viertelschritten dazu ({@link ModDataComponentTypes#BLUEPRINT_ROTATION}).
 *
 * <p>Gebaut wird nur, was geht: belegte Stellen und Stellen, an denen schon der richtige Block
 * steht, bleiben unberuehrt; fehlt Material, bleibt die Stelle frei (spaeter nachholen). Die
 * Materialsuche ist die des Baustabs ({@link BuildingWandItem#findSupply}), jeder gesetzte Block
 * kostet wie beim Baustab einen Haltbarkeitspunkt. Der Kreativmodus setzt alles.
 */
public final class BlueprintBuilder {
    /** Obergrenze je Klick, damit ein 128er-Wuerfel im Kreativmodus den Server nicht anhaelt. */
    public static final int MAX_PER_CLICK = 32768;
    /** Obergrenze der Geisterbloecke in der Vorschau. */
    public static final int MAX_PREVIEW = 4096;

    private BlueprintBuilder() {
    }

    public record Placement(BlockPos pos, BlockState state) {
    }

    public record Result(List<Placement> placed, int already, int occupied, int blocked, int missing,
                         Map<Item, Integer> missingItems, boolean wandBroke) {
    }

    /** Materialquelle der Planung: {@code take} bucht {@code count} Stueck ab, wenn sie reichen. */
    public interface Supply {
        boolean take(Item item, int count);
    }

    // =====================================================================================
    // GEOMETRIE
    // =====================================================================================

    /** Drehung aus Blickrichtung (Sueden = keine) plus gespeicherte Viertelschritte im Uhrzeigersinn. */
    public static Rotation rotationFor(Direction facing, int extraSteps) {
        int base = switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        return switch (Math.floorMod(base + extraSteps, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static int rotationSteps(ItemStack blueprint) {
        return blueprint.getOrDefault(ModDataComponentTypes.BLUEPRINT_ROTATION, 0);
    }

    /** Wohin gebaut wird: in den geklickten Block, wenn er ersetzbar ist (Gras), sonst davor. */
    public static BlockPos targetFor(Level level, BlockPos clicked, Direction face) {
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
    }

    /**
     * Alle Stellen des Bauwerks in Weltkoordinaten, in Bau-Reihenfolge: zuerst alles, was etwas
     * kostet (von unten nach oben), danach die kostenlosen Gegenstuecke (obere Tuerhaelften,
     * Bettkopfteile), damit deren Partner schon steht.
     */
    public static List<Placement> layout(BlueprintModel model, BlockPos target, Rotation rotation) {
        List<Placement> first = new ArrayList<>(model.size());
        List<Placement> partners = new ArrayList<>();
        int minX = model.minX(), minY = model.minY(), minZ = model.minZ();
        int half = model.sizeX() / 2;
        for (int k : model.sortedKeys()) {
            BlockState state = model.blocks().get(k);
            int lx = BlueprintModel.keyX(k) - minX - half;
            int ly = BlueprintModel.keyY(k) - minY;
            int lz = BlueprintModel.keyZ(k) - minZ;
            int rx, rz;
            switch (rotation) {
                case CLOCKWISE_90 -> { rx = -lz; rz = lx; }
                case CLOCKWISE_180 -> { rx = -lx; rz = -lz; }
                case COUNTERCLOCKWISE_90 -> { rx = lz; rz = -lx; }
                default -> { rx = lx; rz = lz; }
            }
            Placement p = new Placement(target.offset(rx, ly, rz), state.rotate(rotation));
            if (BlueprintMaterials.cost(state).count() == 0) {
                partners.add(p);
            } else {
                first.add(p);
            }
        }
        first.addAll(partners);
        return first;
    }

    // =====================================================================================
    // PLANUNG (Vorschau und Bau teilen sie)
    // =====================================================================================

    /**
     * Geht die Stellen in Bau-Reihenfolge durch und entscheidet je Stelle. Mit {@code placer != null}
     * wird wirklich gesetzt (Server), sonst nur geplant (Vorschau).
     */
    public static Result plan(Level level, Player player, ItemStack wand, List<Placement> layout, Supply supply,
                              boolean creative, int limit, Placer placer) {
        List<Placement> placed = new ArrayList<>();
        Set<BlockPos> placedPositions = new HashSet<>();
        Map<Item, Integer> missingItems = new LinkedHashMap<>();
        int already = 0, occupied = 0, blocked = 0, missing = 0;
        boolean broke = false;
        for (Placement p : layout) {
            if (placed.size() >= limit) {
                break;
            }
            BlockPos pos = p.pos();
            if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.isLoaded(pos)
                    || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, wand)) {
                blocked++;
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == p.state().getBlock()) {
                already++;
                continue;
            }
            if (!current.canBeReplaced()) {
                occupied++;
                continue;
            }
            BlueprintMaterials.Cost cost = BlueprintMaterials.cost(p.state());
            BlockState state = p.state();
            if (cost.count() == 0) {
                BlockPos partner = partnerOf(pos, state);
                if (partner != null && !placedPositions.contains(partner) && !level.getBlockState(partner).is(state.getBlock())) {
                    missing++;
                    continue;
                }
            } else if (!creative) {
                if (cost.creativeOnly() || !supply.take(cost.item(), cost.count())) {
                    missing++;
                    missingItems.merge(cost.item(), cost.count(), Integer::sum);
                    continue;
                }
            }
            if (!creative && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                // Wasser gibt es im Ueberlebensmodus nicht umsonst dazu.
                state = state.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            if (placer != null && !placer.place(pos, state)) {
                blocked++;
                continue;
            }
            placed.add(new Placement(pos, state));
            placedPositions.add(pos);
            if (placer != null && placer.wandBroken()) {
                broke = true;
                break;
            }
        }
        return new Result(placed, already, occupied, blocked, missing, missingItems, broke);
    }

    public interface Placer {
        boolean place(BlockPos pos, BlockState state);

        boolean wandBroken();
    }

    private static BlockPos partnerOf(BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        }
        return null;
    }

    /** Vorschau-Quelle: zaehlt den Vorrat einmal je Item und bucht nur in Gedanken ab. */
    public static Supply simulatedSupply(Player player, ItemStack wand) {
        Map<Item, Integer> left = new HashMap<>();
        return (item, count) -> {
            int have = left.computeIfAbsent(item, i -> BuildingWandItem.countSupply(player, wand, i));
            if (have < count) {
                return false;
            }
            left.put(item, have - count);
            return true;
        };
    }

    /** Echte Quelle: verbraucht aus Inventar, Buendeln und Rucksack wie der Baustab. */
    public static Supply realSupply(Player player, ItemStack wand) {
        return (item, count) -> {
            if (count > 1 && BuildingWandItem.countSupply(player, wand, item) < count) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                Runnable source = BuildingWandItem.findSupply(player, wand, item);
                if (source == null) {
                    return false;
                }
                source.run();
            }
            return true;
        };
    }

    // =====================================================================================
    // BAUEN (Server)
    // =====================================================================================

    public static InteractionResult useWand(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack wand = context.getItemInHand();
        ItemStack blueprint = player.getOffhandItem();
        Result result = build(level, player, wand, blueprint, context.getClickedPos(), context.getClickedFace());
        return result == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    /** Baut und meldet das Ergebnis in der Aktionsleiste; {@code null}, wenn abgelehnt. */
    public static Result build(Level level, Player player, ItemStack wand, ItemStack blueprint, BlockPos clicked, Direction face) {
        if (!(wand.getItem() instanceof BuildingWandItem wandItem) || !(blueprint.getItem() instanceof BlueprintItem)) {
            return null;
        }
        BlueprintContent content = blueprint.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(content.code());
        if (parsed.model().isEmpty()) {
            tell(player, Component.translatable("simplebuilding.blueprint.build.empty").withStyle(ChatFormatting.RED));
            return null;
        }
        if (!parsed.ok()) {
            tell(player, Component.translatable("simplebuilding.blueprint.build.errors", parsed.problems().size()).withStyle(ChatFormatting.RED));
            return null;
        }
        int size = parsed.model().maxEdge();
        int edge = BlueprintTiers.edgeFor(wandItem);
        if (size > edge) {
            int needed = BlueprintTiers.tierIndexFor(size);
            tell(player, Component.translatable("simplebuilding.blueprint.build.too_big", size, edge,
                    BlueprintTiers.wandName(Math.max(0, needed))).withStyle(ChatFormatting.RED));
            return null;
        }
        BlockPos target = targetFor(level, clicked, face);
        Rotation rotation = rotationFor(player.getDirection(), rotationSteps(blueprint));
        List<Placement> layout = layout(parsed.model(), target, rotation);
        boolean creative = player.getAbilities().instabuild;
        Placer placer = new Placer() {
            @Override
            public boolean place(BlockPos pos, BlockState state) {
                if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
                    return false;
                }
                if (!creative) {
                    wand.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
                return true;
            }

            @Override
            public boolean wandBroken() {
                return wand.isEmpty();
            }
        };
        Result result = plan(level, player, wand, layout, realSupply(player, wand), creative, MAX_PER_CLICK, placer);
        if (!result.placed().isEmpty()) {
            SoundType sound = result.placed().get(0).state().getSoundType();
            level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
        Component message = Component.translatable("simplebuilding.blueprint.build.done", result.placed().size(), result.missing())
                .withStyle(result.missing() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
        tell(player, message);
        return result;
    }

    private static void tell(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    // =====================================================================================
    // VORSCHAU (Client)
    // =====================================================================================

    private static Object previewKey;
    private static Map<BlockPos, BlockState> previewCache = Map.of();

    /**
     * Die Geisterbloecke fuer die Vorschau: genau das, was ein Klick jetzt setzen wuerde (mit
     * simuliertem Vorrat). Leer, wenn die Blaupause fehlerhaft oder fuer den Stab zu gross ist.
     * Zwischengespeichert und nur alle vier Ticks neu berechnet.
     */
    public static Map<BlockPos, BlockState> preview(Level level, Player player, ItemStack wand, ItemStack blueprint, BlockHitResult hit) {
        if (!(wand.getItem() instanceof BuildingWandItem wandItem)) {
            return Map.of();
        }
        BlueprintContent content = blueprint.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
        BlockPos target = targetFor(level, hit.getBlockPos(), hit.getDirection());
        Rotation rotation = rotationFor(player.getDirection(), rotationSteps(blueprint));
        List<Object> key = List.of(content.code(), target, rotation, level.getGameTime() / 4, wand.getItem(), player.getAbilities().instabuild);
        if (Objects.equals(key, previewKey)) {
            return previewCache;
        }
        previewKey = key;
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(content.code());
        if (!parsed.ok() || parsed.model().isEmpty() || parsed.model().maxEdge() > BlueprintTiers.edgeFor(wandItem)) {
            previewCache = Map.of();
            return previewCache;
        }
        boolean creative = player.getAbilities().instabuild;
        Result result = plan(level, player, wand, layout(parsed.model(), target, rotation),
                simulatedSupply(player, wand), creative, MAX_PREVIEW, null);
        Map<BlockPos, BlockState> map = new LinkedHashMap<>();
        for (Placement p : result.placed()) {
            map.put(p.pos(), p.state());
        }
        previewCache = map;
        return map;
    }
}
