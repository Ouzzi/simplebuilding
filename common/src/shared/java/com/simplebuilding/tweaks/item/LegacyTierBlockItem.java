package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.easter.EasterEggs;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Item eines alten, abgeloesten Stufenblocks (z. B. {@code netherite_flypad}): liegt es im Inventar
 * eines Spielers, tauscht es sich beim naechsten Inventar-Tick gegen die neue Stufe (Anzahl und
 * Komponenten bleiben). In Truhen bleibt es, bis es jemand aufnimmt; gesetzt wird der alte Block,
 * der sich selbst umbaut.
 */
public class LegacyTierBlockItem extends BlockItem {
    private final Supplier<Item> target;

    public LegacyTierBlockItem(Block block, Item.Properties properties, Supplier<Item> target) {
        super(block, properties);
        this.target = target;
    }

    public Item target() {
        return target.get();
    }

    /**
     * Der Stapel als neue Stufe (Anzahl und Komponenten bleiben); eine Easter-Stufe wird auf die Stufe
     * des neuen Blocks umgerechnet, samt Namen.
     */
    public ItemStack migrate(ItemStack stack) {
        ItemStack copy = stack.transmuteCopy(target());
        if (EasterEggs.stageOf(copy) > 0 && copy.getItem() instanceof BlockItem blockItem) {
            copy.remove(EasterEggs.EASTER_STAGE);
            copy.remove(DataComponents.ITEM_NAME);
            EasterEggs.mark(copy, EasterEggs.tierIndex(blockItem.getBlock()));
        }
        return copy;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof Player player) {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i) == stack) {
                    inventory.setItem(i, migrate(stack));
                    return;
                }
            }
        }
    }
}
