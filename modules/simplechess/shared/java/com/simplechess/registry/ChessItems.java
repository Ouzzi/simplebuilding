package com.simplechess.registry;

import com.simplechess.ChessColor;
import com.simplechess.ChessPiece;
import com.simplechess.SimpleChess;
import com.simplechess.item.ChessPieceItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The items of Simple Chess (docs/ai/PLAN-SCHACH-2026-10-06.md, same paths as in SimpleBuilding): the checkers, per colour
 * an octet (only with Simple Octets, {@link ChessOctets}), per colour, piece and form (3D, flat) a piece, per checker stairs
 * and slab. Colours that are not {@link ChessColor#available} have none of them.
 */
public final class ChessItems {
    /** A piece: colour, kind, flat or 3D. */
    public record PieceKind(ChessColor color, ChessPiece piece, boolean flat) {
        public String id() {
            return pieceId(this.color, this.piece, this.flat);
        }
    }

    static final Map<ChessColor, Item> OCTETS = new EnumMap<>(ChessColor.class);
    private static final Map<PieceKind, Item> PIECES = new LinkedHashMap<>();
    private static final List<Item> SHAPES = new ArrayList<>();
    private static final List<Item> CHECKERS = new ArrayList<>();

    private ChessItems() {
    }

    public static String octetId(ChessColor color) {
        return color.id() + "_octet";
    }

    public static String pieceId(ChessColor color, ChessPiece piece, boolean flat) {
        return color.id() + "_chess_" + piece.id() + (flat ? "_flat" : "");
    }

    /** Registers every item; after {@link ChessRegistry#registerBlocks}. */
    public static void register() {
        if (!PIECES.isEmpty()) return;
        for (Block checker : ChessRegistry.checkers()) {
            CHECKERS.add(item(BuiltInRegistries.BLOCK.getKey(checker).getPath(), s -> new BlockItem(checker, s)));
        }
        if (SimpleChess.isLoaded("simpleoctets")) ChessOctets.registerItems();
        for (ChessColor color : ChessColor.values()) {
            if (!color.available()) continue;
            for (boolean flat : new boolean[]{false, true}) {
                for (ChessPiece piece : ChessPiece.values()) {
                    PieceKind kind = new PieceKind(color, piece, flat);
                    PIECES.put(kind, item(kind.id(), s -> new ChessPieceItem(kind, s.stacksTo(16))));
                }
            }
        }
        for (ChessRegistry.CheckerShapes shapes : ChessRegistry.shapes()) {
            for (Block block : List.of(shapes.stairs(), shapes.slab())) {
                SHAPES.add(item(BuiltInRegistries.BLOCK.getKey(block).getPath(), s -> new BlockItem(block, s)));
            }
        }
    }

    static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleChess.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    /** The octet of a colour; null without Simple Octets or for a colour that is not available. */
    public static @Nullable Item octet(ChessColor color) {
        return OCTETS.get(color);
    }

    public static @Nullable Item piece(ChessColor color, ChessPiece piece, boolean flat) {
        return PIECES.get(new PieceKind(color, piece, flat));
    }

    /** All octets in colour order (empty without Simple Octets). */
    public static List<Item> octets() {
        return List.copyOf(OCTETS.values());
    }

    /** All pieces: per colour first the six 3D, then the six flat pieces. */
    public static Map<PieceKind, Item> pieces() {
        return Collections.unmodifiableMap(PIECES);
    }

    /** Stairs and slabs of the checkers, per checker stairs before slab. */
    public static List<Item> shapes() {
        return List.copyOf(SHAPES);
    }

    /** The checker items in colour order. */
    public static List<Item> checkers() {
        return List.copyOf(CHECKERS);
    }

    /** Every item of the mod (checkers, octets, pieces, shapes). */
    public static List<Item> all() {
        List<Item> all = new ArrayList<>(CHECKERS);
        all.addAll(OCTETS.values());
        all.addAll(PIECES.values());
        all.addAll(SHAPES);
        return all;
    }

    /** The items of a colour for the creative tab: octet, stairs/slab of its checker, 3D, flat pieces. */
    public static List<Item> row(ChessColor color) {
        List<Item> row = new ArrayList<>();
        Item octet = octet(color);
        if (octet != null) row.add(octet);
        for (ChessRegistry.CheckerShapes shapes : ChessRegistry.shapes()) {
            if (shapes.color() == color) {
                row.add(shapes.stairs().asItem());
                row.add(shapes.slab().asItem());
            }
        }
        for (boolean flat : new boolean[]{false, true}) {
            for (ChessPiece piece : ChessPiece.values()) {
                Item item = piece(color, piece, flat);
                if (item != null) row.add(item);
            }
        }
        return row;
    }

    /** Puts a stack into the empty main hand, else into the inventory; what does not fit drops at the feet. */
    public static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            Block.popResource(player.level(), player.blockPosition(), stack);
        }
    }
}
