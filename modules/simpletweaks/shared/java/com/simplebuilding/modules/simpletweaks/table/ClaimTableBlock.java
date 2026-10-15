package com.simplebuilding.modules.simpletweaks.table;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The claim table: a wooden table with a map and a seal. Use opens the claim map (who claimed what, around the table).
 * No block entity and no container: nothing is stored in the block, the data comes from the server ledger when asked.
 */
public final class ClaimTableBlock extends Block {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("simpletweaks", "claim_table");
    private static Block block;
    private static Item item;

    private ClaimTableBlock(Properties properties) { super(properties); }

    public static void registerBlock() {
        var key = ResourceKey.create(Registries.BLOCK, ID);
        block = Registry.register(BuiltInRegistries.BLOCK, key, new ClaimTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                .strength(2.5F).sound(SoundType.WOOD).ignitedByLava().setId(key)));
    }
    public static void registerItem() {
        var key = ResourceKey.create(Registries.ITEM, ID);
        item = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }
    public static Block block() { return block != null ? block : BuiltInRegistries.BLOCK.getValue(ID); }
    public static Item item() { return item != null ? item : BuiltInRegistries.ITEM.getValue(ID); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !Claims.enabled(level.getServer())) return InteractionResult.PASS;
        if (!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks")) return InteractionResult.PASS;
        if (!TableService.open(sp, pos)) sp.sendSystemMessage(Component.translatable("message.simpletweaks.claim_table.no_client"), true);
        return InteractionResult.SUCCESS;
    }
}
