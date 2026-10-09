package com.simplebuilding.chess;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.custom.CheckerOctetItem;
import com.simplebuilding.items.custom.ChessPieceItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Die Items der Schach-Familie (docs/ai/PLAN-SCHACH-2026-10-06.md): je Farbe ein Achtel, je Farbe, Figur und Form (3D,
 * flach) eine Figur, je Schachbrett Treppe und Stufe. Registriert aus {@code ModItems} (nur mit {@code McVersion.CHESS});
 * ohne Registrierung sind alle Listen leer.
 */
public final class ChessItems {
    /** Eine Figur: Farbe, Art, flach oder 3D. */
    public record PieceKind(ChessColor color, ChessPiece piece, boolean flat) {
        public String id() {
            return pieceId(this.color, this.piece, this.flat);
        }
    }

    private static final Map<ChessColor, Item> OCTETS = new EnumMap<>(ChessColor.class);
    private static final Map<PieceKind, Item> PIECES = new LinkedHashMap<>();
    private static final List<Item> SHAPES = new ArrayList<>();

    private ChessItems() {
    }

    public static String octetId(ChessColor color) {
        return color.id() + "_octet";
    }

    public static String pieceId(ChessColor color, ChessPiece piece, boolean flat) {
        return color.id() + "_chess_" + piece.id() + (flat ? "_flat" : "");
    }

    /** Registriert alle Items ueber {@code registrar} (Name, Fabrik) - aus {@code ModItems}. */
    public static void register(BiFunction<String, Function<Item.Properties, Item>, Item> registrar) {
        for (ChessColor color : ChessColor.values()) {
            // Owner N18 asked for 128; 99 is the engine maximum (ItemStack and max_stack_size codecs allow 1..99).
            OCTETS.put(color, registrar.apply(octetId(color), s -> new CheckerOctetItem(color, s.stacksTo(CheckerOctetItem.MAX_STACK))));
        }
        for (ChessColor color : ChessColor.values()) {
            for (boolean flat : new boolean[]{false, true}) {
                for (ChessPiece piece : ChessPiece.values()) {
                    PieceKind kind = new PieceKind(color, piece, flat);
                    PIECES.put(kind, registrar.apply(kind.id(), s -> new ChessPieceItem(kind, s.stacksTo(16))));
                }
            }
        }
        for (ModBlocks.CheckerShapes shapes : ModBlocks.CHECKER_SHAPES) {
            for (Block block : List.of(shapes.stairs(), shapes.slab())) {
                String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                SHAPES.add(registrar.apply(name, s -> new BlockItem(block, s)));
            }
        }
    }

    public static Item octet(ChessColor color) {
        return OCTETS.get(color);
    }

    public static Item piece(ChessColor color, ChessPiece piece, boolean flat) {
        return PIECES.get(new PieceKind(color, piece, flat));
    }

    /** Alle Achtel in Farbreihenfolge. */
    public static List<Item> octets() {
        return List.copyOf(OCTETS.values());
    }

    /** Alle Figuren: je Farbe erst die sechs 3D-, dann die sechs flachen Figuren. */
    public static Map<PieceKind, Item> pieces() {
        return Collections.unmodifiableMap(PIECES);
    }

    /** Treppen und Stufen der Schachbretter, je Schachbrett Treppe vor Stufe. */
    public static List<Item> shapes() {
        return List.copyOf(SHAPES);
    }

    /** Die Items einer Farbe fuer eine Kreativ-Zeile: Achtel, Treppe/Stufe ihres Schachbretts, 3D-, flache Figuren. */
    public static List<Item> row(ChessColor color) {
        List<Item> row = new ArrayList<>();
        row.add(octet(color));
        for (ModBlocks.CheckerShapes shapes : ModBlocks.CHECKER_SHAPES) {
            if (shapes.color() == color) {
                row.add(shapes.stairs().asItem());
                row.add(shapes.slab().asItem());
            }
        }
        for (boolean flat : new boolean[]{false, true}) {
            for (ChessPiece piece : ChessPiece.values()) {
                row.add(piece(color, piece, flat));
            }
        }
        return row;
    }

    /** Gibt einen Stapel in die leere Haupthand, sonst ins Inventar; was nicht passt, faellt vor die Fuesse. */
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
