package com.simplechess.registry;

import com.simplechess.ChessColor;
import com.simplechess.SimpleChess;
import com.simplechess.block.ChessPiecesBlock;
import com.simplechess.block.ChessPiecesBlockEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks of Simple Chess (moved from SimpleBuilding with the same paths, N47 M7): the twelve quartz checkers with stairs
 * and slab, the chess pieces cell with its block entity, and the creative tab. The palette colours register only while their
 * source is loaded ({@link ChessColor#available}); the octet cell only with Simple Octets ({@link ChessOctets}). Loaders call
 * {@link #registerBlocks} before {@link ChessItems#register}, then {@link #registerTab}.
 */
public final class ChessRegistry {
    /** Stairs, slab and (N77, owner 16.11.2026) wall of a quartz checker (properties of the checker, Astralit glows with 5). */
    public record CheckerShapes(ChessColor color, Block checker, Block stairs, Block slab, Block wall) {
    }

    private static final Map<ChessColor, Block> CHECKERS = new EnumMap<>(ChessColor.class);
    private static final List<CheckerShapes> SHAPES = new ArrayList<>();
    /** Up to four chess pieces on a block (block entity, renderer); drops the pieces. */
    public static Block CHESS_PIECES;
    public static BlockEntityType<ChessPiecesBlockEntity> CHESS_PIECES_BE;
    public static CreativeModeTab TAB;

    private ChessRegistry() {
    }

    public static void registerBlocks() {
        if (CHESS_PIECES != null) return;
        for (ChessColor color : ChessColor.values()) {
            if (color.checkerId() == null || !color.available()) continue;
            Block checker = block(color.checkerId(), checkerProperties(color), s -> new RotatedPillarBlock(s.isValidSpawn((state, level, pos, type) -> false)));
            CHECKERS.put(color, checker);
        }
        for (Map.Entry<ChessColor, Block> e : CHECKERS.entrySet()) {
            Block checker = e.getValue();
            String name = e.getKey().checkerId();
            Block stairs = block(name + "_stairs", BlockBehaviour.Properties.ofFullCopy(checker), s -> new StairBlock(checker.defaultBlockState(), s));
            Block slab = block(name + "_slab", BlockBehaviour.Properties.ofFullCopy(checker), SlabBlock::new);
            Block wall = block(name + "_wall", BlockBehaviour.Properties.ofFullCopy(checker), s -> new net.minecraft.world.level.block.WallBlock(s.forceSolidOn()));
            SHAPES.add(new CheckerShapes(e.getKey(), checker, stairs, slab, wall));
        }
        CHESS_PIECES = block("chess_pieces", BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.STONE)
                .noLootTable().noOcclusion().pushReaction(PushReaction.POPPED), ChessPiecesBlock::new);
        if (SimpleChess.isLoaded("simpleoctets")) ChessOctets.registerBlock();
    }

    /** The block entity type (Fabric: from the module entry, through its block entity builder). */
    public static void registerBlockEntity(BlockEntityType<ChessPiecesBlockEntity> type) {
        CHESS_PIECES_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleChess.id("chess_pieces"), type);
    }

    /**
     * Properties as in SimpleBuilding: the Vanilla-made checkers copy their material block; the palette ones a fixed End stone
     * template like SimpleBuilding's astral/nihil End stone they copied there (hardness 3/9, pickaxe), Ender Quartz purple,
     * the Astralit ones glowing with 5 (half quartz, between the ore's 5 and the coated blocks' 10).
     */
    private static BlockBehaviour.Properties checkerProperties(ChessColor color) {
        return switch (color) {
            case PURPUR -> BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK);
            case LAPIS -> BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_BLOCK);
            case BLACKSTONE -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE);
            case RESIN -> BlockBehaviour.Properties.ofFullCopy(Blocks.RESIN_BRICKS);
            case NETHER_BRICK -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICKS);
            case RED_NETHER_BRICK -> BlockBehaviour.Properties.ofFullCopy(Blocks.RED_NETHER_BRICKS);
            default -> {
                boolean ender = color == ChessColor.ENDER_QUARTZ || color == ChessColor.POLISHED_ENDER_QUARTZ;
                BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(ender ? MapColor.COLOR_PURPLE : MapColor.SAND)
                        .requiresCorrectToolForDrops().strength(3.0F, 9.0F).sound(SoundType.STONE);
                yield color.light() > 0 ? p.lightLevel(state -> color.light()) : p;
            }
        };
    }

    static Block block(String name, BlockBehaviour.Properties properties, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleChess.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    /** The quartz checker of a colour; null for quartz and colours that are not registered. */
    public static @Nullable Block checker(ChessColor color) {
        return CHECKERS.get(color);
    }

    /** The registered checkers in colour order. */
    public static List<Block> checkers() {
        return List.copyOf(CHECKERS.values());
    }

    /** Stairs and slabs of the registered checkers, in colour order. */
    public static List<CheckerShapes> shapes() {
        return List.copyOf(SHAPES);
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleChess.id("chess"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplechess.chess"))
                .icon(() -> new ItemStack(ChessItems.piece(ChessColor.QUARTZ, com.simplechess.ChessPiece.KNIGHT, false)))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /**
     * The tab as in SimpleBuilding: the checkers, then per colour a row - octet, the checker's stairs and slab, the six 3D
     * pieces - and a second row with the six flat pieces; the guide at the end. Empty while the module is switched off.
     */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleChess.on()) return out;
        CHECKERS.values().forEach(b -> out.add(new ItemStack(b)));
        for (ChessColor color : ChessColor.values()) {
            if (!color.available()) continue;
            ChessItems.row(color).forEach(item -> out.add(new ItemStack(item)));
        }
        out.add(com.simplechess.guide.ChessGuide.stack());
        return out;
    }
}
