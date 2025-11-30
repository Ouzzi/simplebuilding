package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// Dieses Item verhält sich wie ein Bundle im Inventar,
// kann aber als Block platziert werden (Shulker Box).
public class ReinforcedShulkerBoxItem extends BlockItem {

    public ReinforcedShulkerBoxItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public boolean canBeNested() {
        return false; 
    }

    // =============================================================
    // API: Funnel Support (Analog zum Bundle)
    // =============================================================
    public boolean tryInsertStackFromWorld(ItemStack shulkerStack, ItemStack stackToInsert, PlayerEntity player) {
        if (!stackToInsert.getItem().canBeNested()) return false;

        BundleContentsComponent contents = shulkerStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        Fraction maxCap = getMaxCapacity(shulkerStack, player);
        int added = insertItemIntoBundle(shulkerStack, contents, stackToInsert, maxCap);

        if (added > 0) {
            stackToInsert.decrement(added);
            this.playInsertSound(player);
            return true;
        }
        return false;
    }

    // =============================================================
    // 1. Kapazitäts-Berechnung (Deep Pockets)
    // =============================================================
    private Fraction getMaxCapacity(ItemStack stack, PlayerEntity player) {
        if (player == null || player.getEntityWorld() == null) return Fraction.getFraction(1, 1);

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var deepPockets = enchantments.getOptional(ModEnchantments.DEEP_POCKETS);

        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getLevel(deepPockets.get(), stack);
            // Shulker könnten standardmäßig schon mehr tragen, aber wir nutzen hier die Bundle-Logik
            if (level == 1) return Fraction.getFraction(2, 1);
            if (level >= 2) return Fraction.getFraction(4, 1);
        }
        return Fraction.getFraction(1, 1);
    }

    // =============================================================
    // 2. Interaktion: Shulker im Cursor klickt auf Slot
    // =============================================================
    @Override
    public boolean onStackClicked(ItemStack shulkerStack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT && clickType != ClickType.LEFT) return false;

        BundleContentsComponent contents = shulkerStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        ItemStack itemInSlot = slot.getStack();

        // RECHTS-KLICK auf leeren Slot -> Item HERAUSNEHMEN
        if (clickType == ClickType.RIGHT && itemInSlot.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(shulkerStack, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                slot.insertStack(removed);
                return true;
            }
        }
        // LINKS-KLICK auf Item -> Item HINEINLEGEN
        else if (clickType == ClickType.LEFT && !itemInSlot.isEmpty() && itemInSlot.getItem().canBeNested()) {
            int added = insertItemIntoBundle(shulkerStack, contents, itemInSlot, getMaxCapacity(shulkerStack, player));
            if (added > 0) {
                this.playInsertSound(player);
                itemInSlot.decrement(added);
                return true;
            }
        }
        return false;
    }

    // =============================================================
    // 3. Interaktion: Item im Cursor klickt auf Shulker im Slot
    // =============================================================
    @Override
    public boolean onClicked(ItemStack shulkerStack, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT && clickType != ClickType.LEFT) return false;

        BundleContentsComponent contents = shulkerStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        // RECHTS-KLICK mit leerer Hand -> Item HERAUSNEHMEN
        if (clickType == ClickType.RIGHT && cursorStack.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(shulkerStack, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                cursorStackReference.set(removed);
                return true;
            }
        }
        // LINKS-KLICK mit Item -> Item HINEINLEGEN
        else if (clickType == ClickType.LEFT && !cursorStack.isEmpty() && cursorStack.getItem().canBeNested()) {
            int added = insertItemIntoBundle(shulkerStack, contents, cursorStack, getMaxCapacity(shulkerStack, player));
            if (added > 0) {
                this.playInsertSound(player);
                cursorStack.decrement(added);
                return true;
            }
        }
        return false;
    }

    // =============================================================
    // 4. Use On Block: Placement vs. Master Builder
    // =============================================================
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack shulkerStack = context.getStack();
        World world = context.getWorld();

        // A) Wenn der Spieler SNEAKED: Platziere die Shulker Box selbst (Standard BlockItem Verhalten)
        if (player.isSneaking()) {
            // Optional: Hier Logik einfügen, um BundleContentsComponent in BlockEntityTag zu konvertieren, 
            // falls der BlockEntity das nicht automatisch macht.
            return super.useOnBlock(context);
        }

        // B) Master Builder Logik: Platziere Items AUS dem Shulker
        if (hasMasterBuilder(shulkerStack, world)) {
            BundleContentsComponent contents = shulkerStack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents != null && !contents.isEmpty()) {

                int index = contents.getSelectedStackIndex();
                if (index == -1 || index >= contents.size()) index = 0;

                if (hasColorPalette(shulkerStack, world)) {
                    index = world.getRandom().nextInt(contents.size());
                }

                ItemStack blockToPlace = contents.get(index).copy();

                if (blockToPlace.getItem() instanceof BlockItem blockItem) {
                    // Erstelle neuen Context für das innere Item
                    ItemUsageContext blockContext = new ItemUsageContext(
                            world,
                            player,
                            context.getHand(),
                            blockToPlace,
                            new BlockHitResult(
                                    context.getHitPos(),
                                    context.getSide(),
                                    context.getBlockPos(),
                                    false
                            )
                    );

                    ActionResult result = blockItem.useOnBlock(blockContext);

                    if (result.isAccepted()) {
                        if (!player.getAbilities().creativeMode) {
                            removeOneItemFromBundle(shulkerStack, contents, index);
                        }
                        return result;
                    }
                }
            }
        }

        // Fallback: Wenn kein Master Builder und nicht gesneaked,
        // könnten wir hier entscheiden, ob wir die Shulker Box platzieren oder nichts tun.
        // Standard Minecraft Bundle platziert nichts.
        // Standard Shulker platziert sich.
        // Entscheidung: Ohne Sneak verhält es sich wie ein Shulker (platziert sich).
        return super.useOnBlock(context); 
    }

    // =============================================================
    // 5. Use in Air: Drop Item (Bundle Style)
    // =============================================================
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Wenn man sneaked, wollen wir vielleicht nichts tun oder GUI öffnen? 
        // Bundle Logik: Rechtsklick droppt Item.
        
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

        if (contents != null && !contents.isEmpty()) {
            // Nur droppen, wenn wir nicht gerade versuchen einen Block zu platzieren (das fängt useOnBlock ab)
            // Hier droppen wir Items, wenn wir in die Luft klicken
            
            int index = contents.getSelectedStackIndex();
            if (index == -1 || index >= contents.size()) index = 0;

            if (!world.isClient()) {
                ItemStack removed = removeSelectedOrFirstItem(stack, contents);
                user.dropItem(removed, true);
                this.playRemoveOneSound(user);
            }
            return ActionResult.SUCCESS;
        }

        return super.use(world, user, hand);
    }

    // =============================================================
    // LOGIK & HELPER (Identisch zum Bundle)
    // =============================================================

    private boolean hasColorPalette(ItemStack stack, World world) {
        var registry = world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var cp = enchantments.getOptional(ModEnchantments.COLOR_PALETTE);
        return cp.isPresent() && EnchantmentHelper.getLevel(cp.get(), stack) > 0;
    }

    private boolean hasMasterBuilder(ItemStack stack, World world) {
        var registry = world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var mb = enchantments.getOptional(ModEnchantments.MASTER_BUILDER);
        return mb.isPresent() && EnchantmentHelper.getLevel(mb.get(), stack) > 0;
    }

    private int insertItemIntoBundle(ItemStack bundle, BundleContentsComponent contents, ItemStack stackToAdd, Fraction maxCap) {
        if (stackToAdd.isEmpty()) return 0;

        Fraction currentOccupancy = contents.getOccupancy();
        Fraction itemWeight = Fraction.getFraction(1, stackToAdd.getMaxCount());
        Fraction remainingSpace = maxCap.subtract(currentOccupancy);

        if (remainingSpace.compareTo(itemWeight) < 0) return 0;

        int maxStackSize = stackToAdd.getMaxCount();
        int countThatFits = (int) remainingSpace.multiplyBy(Fraction.getFraction(maxStackSize, 1)).doubleValue();
        int countToAdd = Math.min(stackToAdd.getCount(), countThatFits);

        if (countToAdd <= 0) return 0;

        List<ItemStack> newItems = new ArrayList<>();
        for(ItemStack s : contents.iterate()) newItems.add(s.copy());

        ItemStack toAdd = stackToAdd.copy();
        toAdd.setCount(countToAdd);
        addToBundleList(newItems, toAdd);

        // Reset Selection Index logic can be adapted
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
        return countToAdd;
    }

    private void addToBundleList(List<ItemStack> list, ItemStack stackToAdd) {
        if (!list.isEmpty()) {
            ItemStack topStack = list.get(0);
            if (ItemStack.areItemsAndComponentsEqual(topStack, stackToAdd)) {
                int available = topStack.getMaxCount() - topStack.getCount();
                int toMerge = Math.min(available, stackToAdd.getCount());
                if (toMerge > 0) {
                    topStack.increment(toMerge);
                    stackToAdd.decrement(toMerge);
                }
            }
        }
        if (!stackToAdd.isEmpty()) {
            list.add(0, stackToAdd);
        }
    }

    private ItemStack removeSelectedOrFirstItem(ItemStack bundle, BundleContentsComponent contents) {
        if (contents.isEmpty()) return ItemStack.EMPTY;

        int selectedIndex = contents.getSelectedStackIndex();
        if (selectedIndex == -1) selectedIndex = 0;
        if (selectedIndex >= contents.size()) selectedIndex = 0;

        ItemStack itemToRemove = contents.get(selectedIndex).copy();

        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (i != selectedIndex) {
                newItems.add(s.copy());
            }
            i++;
        }

        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
        return itemToRemove;
    }

    private void removeOneItemFromBundle(ItemStack bundle, BundleContentsComponent contents, int targetIndex) {
        if (contents.isEmpty()) return;

        List<ItemStack> newItems = new ArrayList<>();
        for (ItemStack s : contents.iterate()) newItems.add(s.copy());

        if (targetIndex < newItems.size()) {
            ItemStack targetStack = newItems.get(targetIndex);
            targetStack.decrement(1);
            if (targetStack.isEmpty()) {
                newItems.remove(targetIndex);
            }
        }
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
    }

    // =============================================================
    // VISUALS
    // =============================================================

    private Fraction getMaxCapacityForVisuals(ItemStack stack) {
        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().getKey().isPresent()) {
                String id = entry.getKey().getKey().get().getValue().toString();
                if (id.contains("deep_pockets")) {
                    int level = entry.getIntValue();
                    if (level == 1) return Fraction.getFraction(2, 1);
                    if (level >= 2) return Fraction.getFraction(4, 1);
                }
            }
        }
        return Fraction.getFraction(1, 1);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        return contents != null && !contents.isEmpty();
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        BundleContentsComponent data = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (data == null) return 0;
        Fraction current = data.getOccupancy();
        Fraction max = getMaxCapacityForVisuals(stack);
        float fillLevel = Math.min(1.0f, current.divideBy(max).floatValue());
        return Math.round(fillLevel * 13.0F);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        BundleContentsComponent data = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (data == null) return super.getItemBarColor(stack);
        Fraction current = data.getOccupancy();
        Fraction max = getMaxCapacityForVisuals(stack);
        float fillLevel = Math.min(1.0f, current.divideBy(max).floatValue());
        return MathHelper.hsvToRgb(Math.max(0.0F, (1.0F - fillLevel)) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    @Override
    public Optional<net.minecraft.item.tooltip.TooltipData> getTooltipData(ItemStack stack) {
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return Optional.empty();
        int maxCapacity = 64;
        Fraction frac = getMaxCapacityForVisuals(stack);
        if (frac.getNumerator() == 2) maxCapacity = 128;
        if (frac.getNumerator() == 4) maxCapacity = 256;
        return Optional.of(new ReinforcedBundleTooltipData(contents, maxCapacity));
    }

    private void playRemoveOneSound(PlayerEntity entity) {
        entity.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(PlayerEntity entity) {
        entity.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
    }
}
