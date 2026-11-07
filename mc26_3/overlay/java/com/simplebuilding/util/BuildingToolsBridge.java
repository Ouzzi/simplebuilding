package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * SimpleBuilding's hooks into Simple Building Tools (26.3, b-bundle131). Simple Building Tools exists on Fabric
 * only, so mc26_3/forge and mc26_3/neoforge leave this file out of their sources and {@link LibServices}
 * reaches it by name (bundle 132: the direct references broke {@code :mc26_3:forge:compileJava}).
 */
public final class BuildingToolsBridge {
    private BuildingToolsBridge() {
    }

    /** Client: the editor for the placed blueprint at {@code pos}. */
    public static void openPlaced(BlockPos pos, ItemStack stack) {
        com.simplebuildingtools.items.custom.BlueprintItem.openPlaced(pos, stack);
    }

    public static void init() {
        // b-bundle131: Simple Building Tools edits a blueprint lying on SimpleBuilding's placed_blueprint (right-click, empty hand, N23).
        com.simplebuildingtools.networking.ModMessageHandlers.setPlacedAccess(new com.simplebuildingtools.networking.ModMessageHandlers.PlacedAccess() {
            @Override
            public @Nullable ItemStack get(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
                var be = com.simplebuilding.networking.ModMessageHandlers.editablePlacedBlueprint(pos, player);
                return be != null ? be.getTemplate() : null;
            }

            @Override
            public void set(net.minecraft.server.level.ServerPlayer player, BlockPos pos, ItemStack stack) {
                var be = com.simplebuilding.networking.ModMessageHandlers.editablePlacedBlueprint(pos, player);
                if (be != null) be.setTemplate(stack);
            }
        });
    }

    /** b-btools07: SimpleBuilding's switches steer Simple Building Tools' octant selection (key H, opacity, Constructor's Touch inversion). */
    public static void configureClient() {
        com.simplebuildingtools.client.ClientState.configure(
                () -> com.simplebuilding.client.ClientState.showHighlights,
                () -> Simplebuilding.getConfig().tools.buildingHighlightOpacity,
                () -> Simplebuilding.getConfig().tools.invertOctantSneak);
    }
}
