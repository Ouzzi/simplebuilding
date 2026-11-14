package com.simplebuilding.modules.simplechess;

import com.simplechess.client.ChessPiecesRenderer;
import com.simplechess.registry.ChessRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

/** Client entry: the chess pieces renderer for the pieces cell. */
public final class ChessFabricClient implements net.fabricmc.api.ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(ChessRegistry.CHESS_PIECES_BE, ChessPiecesRenderer::new);
    }
}
