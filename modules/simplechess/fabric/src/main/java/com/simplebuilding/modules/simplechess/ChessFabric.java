package com.simplebuilding.modules.simplechess;

import com.simplechess.SimpleChess;
import com.simplechess.block.ChessPiecesBlockEntity;
import com.simplechess.registry.ChessItems;
import com.simplechess.registry.ChessRegistry;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/**
 * Fabric entry of Simple Chess: registries are open during init, so everything registers directly (blocks, block entity,
 * items, guide, tab). Optional sub-mod of SimpleBuilding; nothing of SimpleBuilding is needed (the palette colours and the
 * octets come with their source, {@code ChessColor#available}, {@code ChessOctets}).
 */
public final class ChessFabric implements ModInitializer {
    private static boolean initialized;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;
        FabricLoader loader = FabricLoader.getInstance();
        SimpleChess.init(loader.getConfigDir(), loader::isModLoaded);
        ChessRegistry.registerBlocks();
        ChessRegistry.registerBlockEntity(FabricBlockEntityTypeBuilder.create(ChessPiecesBlockEntity::new, ChessRegistry.CHESS_PIECES).build());
        ChessItems.register();
        com.simplechess.guide.ChessGuide.register();
        ChessRegistry.registerTab();
        SimpleChess.registerStation();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleChess.on()) return;
            var book = List.of(com.simplechess.guide.ChessGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (loader.isModLoaded("ftbquests")) com.simplechess.guide.ChessGuide.installQuests(loader.getConfigDir());
    }
}
