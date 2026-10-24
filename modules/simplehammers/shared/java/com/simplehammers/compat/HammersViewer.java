package com.simplehammers.compat;

import com.simplehammers.SimpleHammers;
import com.simplehammers.config.HammerConfig;
import com.simplehammers.engine.SledgehammerUpgrades;
import com.simplehammers.item.SledgehammerItem;
import com.simplehammers.registry.HammerRegistry;
import com.simplelib.api.RecipeViewer;
import com.simplelib.api.SimpleLibApi;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Simple Hammers in the recipe viewer (JEI24), free of JEI classes: the hammer ranks for the {@code min_tool} grades of
 * SimpleLib's in-world upgrade tab, and - only while no partner shows them (SimpleBuilding has its own tabs "Reshaping"
 * and "Crushing") - the carving pairs block -&gt; stairs -&gt; slab and the crushing of diamond, cracked diamond and
 * quartz blocks. Registered from {@link SimpleHammers#init}.
 */
public final class HammersViewer {
    public static final String NOTE = "jei.simplehammers.note.";

    private HammersViewer() {
    }

    public static void register() {
        RecipeViewer.toolGrade((grade, tool) -> !(tool instanceof SledgehammerItem)
                || SledgehammerUpgrades.hammerRank(tool) >= Math.max(0, SledgehammerUpgrades.GRADES.indexOf(grade)));
        RecipeViewer.inWorld(SimpleHammers.MOD_ID, HammersViewer::entries);
    }

    /** The hammer's own ways; empty while SimpleBuilding reads the upgrades (it shows these in its own tabs). */
    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        if (!SimpleHammers.libLoadsUpgrades()) return out;
        List<Item> hammers = hammers();
        if (hammers.isEmpty()) return out;
        crushes(out, hammers);
        reshapes(out, hammers);
        return out;
    }

    /** The sledgehammers, weakest first. */
    public static List<Item> hammers() {
        List<Item> out = new ArrayList<>(HammerRegistry.HAMMERS.values());
        out.sort(Comparator.comparingInt((Item hammer) -> SledgehammerUpgrades.hammerRank(hammer)));
        return out;
    }

    private static void crushes(List<InWorldUpgrade> out, List<Item> hammers) {
        Item pebble = SimpleLibApi.materialItem("diamond_pebble");
        List<Item> strong = hammers.stream().filter(SledgehammerItem::canCrushDiamondBlock).toList();
        List<Component> diamondNotes = List.of(Component.translatable(NOTE + "crush.strikes", SledgehammerItem.DIAMOND_BLOCK_STRIKES),
                Component.translatable(NOTE + "crush.iron"),
                Component.translatable(NOTE + "durability", SledgehammerItem.DIAMOND_CRUSH_DAMAGE));
        if (pebble != null && pebble != Items.AIR && !strong.isEmpty() && HammerConfig.diamondBlockPebbles > 0) {
            List<Item> blocks = new ArrayList<>(List.of(Items.DIAMOND_BLOCK));
            Block cracked = SimpleLibApi.materialBlock("cracked_diamond_block");
            if (cracked != null && cracked.asItem() != Items.AIR) blocks.add(cracked.asItem());
            for (Item block : blocks) {
                out.add(new InWorldUpgrade("simplehammers/crush/" + BuiltInRegistries.ITEM.getKey(block).getPath(),
                        List.of(InWorldUpgrade.Input.of(block, 1)), strong, new ItemStack(pebble, HammerConfig.diamondBlockPebbles), diamondNotes));
            }
        }
        out.add(new InWorldUpgrade("simplehammers/crush/quartz_block", List.of(InWorldUpgrade.Input.of(Items.QUARTZ_BLOCK, 1)), hammers,
                new ItemStack(Items.QUARTZ, SledgehammerItem.QUARTZ_BLOCK_QUARTZ),
                List.of(Component.translatable(NOTE + "crush.strikes", SledgehammerItem.QUARTZ_BLOCK_QUARTZ),
                        Component.translatable(NOTE + "durability", SledgehammerItem.QUARTZ_CRUSH_DAMAGE))));
    }

    /** Block -&gt; stairs -&gt; slab by the hammer's own naming rule ({@link SledgehammerItem#reshapeTarget}), every block. */
    private static void reshapes(List<InWorldUpgrade> out, List<Item> hammers) {
        List<Component> notes = List.of(Component.translatable(NOTE + "reshape.how"),
                Component.translatable(NOTE + "durability", SledgehammerItem.RESHAPE_DAMAGE));
        List<Block> blocks = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(blocks::add);
        blocks.sort(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        for (Block block : blocks) {
            Item from = block.asItem();
            if (from == Items.AIR) continue;
            SledgehammerItem.reshapeTarget(block, false, isFullBlock(block)).ifPresent(target -> {
                if (target.asItem() == Items.AIR) return;
                out.add(new InWorldUpgrade("simplehammers/reshape/" + BuiltInRegistries.BLOCK.getKey(block).toString().replace(':', '/'),
                        List.of(InWorldUpgrade.Input.of(from, 1)), hammers, new ItemStack(target.asItem()), notes));
            });
        }
    }

    private static boolean isFullBlock(Block block) {
        try {
            return block != Blocks.AIR && block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
