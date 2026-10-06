package com.simplebuilding.compat.jade;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.compat.BlockInfo;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade plugin: only Jade ever loads this class (Fabric: the {@code jade} entrypoint in
 * fabric.mod.json; NeoForge: the {@link WailaPlugin} annotation), the mod runs without Jade.
 *
 * <p>Shows the pad owner, launchpad charges, potion pad potion and cooldown, forced chunks of a chunk
 * loader, the mod hopper's filter (all through Jade's server data), plus piston durability, tiered
 * chest slots and furnace speed (from the block state, client side). The data comes from
 * {@link BlockInfo}; one {@link BlockInfoClientProvider} per topic, so each can be switched off in
 * Jade's config ({@code config.jade.plugin_simplebuilding.<topic>}), plus one
 * {@link BlockInfoServerProvider} per server topic under the same id. The two halves must stay
 * separate classes: Jade throws on a physical client when a data provider is also a component
 * provider (since Minecraft 1.21.6), which kept the client from starting.
 */
@WailaPlugin
public class SimplebuildingJadePlugin implements IWailaPlugin {
    // Same two ids as BlockInfo.isModCauldron; not called from there because the 1.21.11 copy of BlockInfo lacks it.
    private static final java.util.Set<String> NO_FLUID_CAULDRONS = java.util.Set.of("simplesandwiches:milk_cauldron", "simplelib:reinforced_cauldron");
    private static final Map<BlockInfo.Topic, BlockInfoServerProvider> DATA = new EnumMap<>(BlockInfo.Topic.class);
    private static final Map<BlockInfo.Topic, BlockInfoClientProvider> PROVIDERS = new EnumMap<>(BlockInfo.Topic.class);

    static {
        for (BlockInfo.Topic topic : BlockInfo.Topic.values()) {
            if (topic.fromServer()) {
                DATA.put(topic, new BlockInfoServerProvider(topic));
            }
            PROVIDERS.put(topic, new BlockInfoClientProvider(topic));
        }
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        // The 1.21.11 copy also compiles this plugin, but has no crucible topic or bridge.
        for (BlockInfo.Topic topic : BlockInfo.Topic.values()) {
            if (topic.id().equals("crucible")) registration.registerBlockDataProvider(
                    DATA.get(topic), net.minecraft.world.level.block.entity.BlockEntity.class);
        }
        registration.registerBlockDataProvider(DATA.get(BlockInfo.Topic.OWNER), OwnedBlockEntity.class);
        BlockInfoServerProvider pads = DATA.get(BlockInfo.Topic.PAD_STATUS);
        registration.registerBlockDataProvider(pads, LaunchpadBlockEntity.class);
        registration.registerBlockDataProvider(pads, PotionPadBlockEntity.class);
        registration.registerBlockDataProvider(pads, ChunkLoaderBlockEntity.class);
        registration.registerBlockDataProvider(DATA.get(BlockInfo.Topic.HOPPER_FILTER), ModHopperBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        for (BlockInfo.Topic topic : BlockInfo.Topic.values()) {
            if (topic.id().equals("crucible")) registration.registerBlockComponent(PROVIDERS.get(topic), Block.class);
        }
        // Server topics: the tooltip only shows what the server sent, so any block may ask.
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.OWNER), Block.class);
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.PAD_STATUS), Block.class);
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.HOPPER_FILTER), Block.class);
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.PISTON_DURABILITY), NetheriteBreakerPistonBlock.class);
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.CHEST_SLOTS), TieredChestBlock.class);
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.CHEST_SLOTS), net.minecraft.world.level.block.EnderChestBlock.class);
        // Gestufte Shulkerkisten (BlockInfo antwortet nur fuer sie); ueber die Vanilla-Klasse, weil die
        // 1.21.11-Linie dieses Plugin mit ihrer eigenen Kopie des gemeinsamen Codes baut.
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.CHEST_SLOTS), net.minecraft.world.level.block.ShulkerBoxBlock.class);
        registration.registerBlockComponent(PROVIDERS.get(BlockInfo.Topic.FURNACE_SPEED), AbstractFurnaceBlock.class);
        // Milk cauldron and reinforced cauldron: only by topic id, the 1.21.11 copy of BlockInfo has no such topic.
        for (BlockInfo.Topic topic : BlockInfo.Topic.values()) {
            if (topic.id().equals("cauldron")) registration.registerBlockComponent(PROVIDERS.get(topic), AbstractCauldronBlock.class);
        }
        // Jade's universal fluid line ("Empty 1B") is wrong for these two cauldrons: they hold no fluid.
        registration.addTooltipCollectedCallback((root, accessor) -> {
            if (accessor instanceof BlockAccessor block
                    && NO_FLUID_CAULDRONS.contains(BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString())) {
                root.getTooltip().remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            }
        });
    }
}
