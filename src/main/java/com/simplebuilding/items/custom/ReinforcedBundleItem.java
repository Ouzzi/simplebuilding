package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ReinforcedBundleItem extends BundleItem {

    public ReinforcedBundleItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }

    @Override
    public boolean onStackClicked(ItemStack bundle, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT && clickType != ClickType.LEFT) return false;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        ItemStack itemInSlot = slot.getStack();

        if (clickType == ClickType.RIGHT && itemInSlot.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(bundle, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                slot.insertStack(removed);
                return true;
            }
        } else if (clickType == ClickType.LEFT && !itemInSlot.isEmpty() && itemInSlot.getItem().canBeNested()) {
            int added = insertItemIntoBundle(bundle, contents, itemInSlot, getMaxCapacity(bundle, player));
            if (added > 0) {
                this.playInsertSound(player);
                itemInSlot.decrement(added);
                return true;
            }
        }
        return false;
    }

    // =============================================================
    // 3. Interaktion: Item in der Hand (Cursor) klickt auf Bundle im Slot
    // =============================================================
    @Override
    public boolean onClicked(ItemStack bundle, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT && clickType != ClickType.LEFT) return false;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        if (clickType == ClickType.RIGHT && cursorStack.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(bundle, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                cursorStackReference.set(removed);
                return true;
            }
        }
        else if (clickType == ClickType.LEFT && !cursorStack.isEmpty() && cursorStack.getItem().canBeNested()) {
            int added = insertItemIntoBundle(bundle, contents, cursorStack, getMaxCapacity(bundle, player));
            if (added > 0) {
                this.playInsertSound(player);
                cursorStack.decrement(added);
                return true;
            }
        }
        return false;
    }

    @Override
    public ActionResult useOnBlock(net.minecraft.item.ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack bundleStack = context.getStack();

        assert player != null;
        if (hasMasterBuilder(bundleStack, player.getEntityWorld())) {
            BundleContentsComponent contents = bundleStack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents != null && !contents.isEmpty()) {

                int index = contents.getSelectedStackIndex();
                if (index == -1 || index >= contents.size()) index = 0;

                if (hasColorPalette(bundleStack, player.getEntityWorld())) {
                    index = player.getEntityWorld().getRandom().nextInt(contents.size());
                }

                ItemStack blockToPlace = contents.get(index).copy();

                if (blockToPlace.getItem() instanceof net.minecraft.item.BlockItem blockItem) {
                    net.minecraft.item.ItemUsageContext blockContext = new net.minecraft.item.ItemUsageContext(
                            context.getWorld(),
                            context.getPlayer(),
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
                            removeOneItemFromBundle(bundleStack, contents, index);
                        }
                        return result;
                    }
                }
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public net.minecraft.util.ActionResult use(net.minecraft.world.World world, PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

        if (contents != null && !contents.isEmpty()) {
            int index = contents.getSelectedStackIndex();
            if (index == -1 || index >= contents.size()) index = 0;

            ItemStack itemToDrop = contents.get(index);

            if (itemToDrop.getItem() instanceof net.minecraft.item.BlockItem) {
                return net.minecraft.util.ActionResult.FAIL;
            }

            if (!world.isClient()) {
                ItemStack removed = removeSelectedOrFirstItem(stack, contents);
                user.dropItem(removed, true);
                this.playRemoveOneSound(user);
            }
            return net.minecraft.util.ActionResult.SUCCESS;
        }

        return super.use(world, user, hand);
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
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return Optional.empty();
        int maxCapacity = 64;
        Fraction frac = getMaxCapacityForVisuals(stack);
        if (frac.getNumerator() == 2) maxCapacity = 128;
        if (frac.getNumerator() == 4) maxCapacity = 256;
        return Optional.of(new ReinforcedBundleTooltipData(contents, maxCapacity));
    }


    private boolean hasColorPalette(ItemStack stack, net.minecraft.world.World world) {
        var registry = world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var cp = enchantments.getOptional(ModEnchantments.COLOR_PALETTE);
        return cp.isPresent() && EnchantmentHelper.getLevel(cp.get(), stack) > 0;
    }

    private boolean hasMasterBuilder(ItemStack stack, net.minecraft.world.World world) {
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

        BundleItem.setSelectedStackIndex(bundle, -1);
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
        return countToAdd;
    }

    private void addToBundleList(List<ItemStack> list, ItemStack stackToAdd) {
        if (!list.isEmpty()) {
            ItemStack topStack = list.getFirst();
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
            list.addFirst(stackToAdd);
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

        BundleItem.setSelectedStackIndex(bundle, -1);
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
    public boolean tryInsertStackFromWorld(ItemStack bundle, ItemStack stackToInsert, PlayerEntity player) {
        if (!stackToInsert.getItem().canBeNested()) return false;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        Fraction maxCap = getMaxCapacity(bundle, player);

        int added = insertItemIntoBundle(bundle, contents, stackToInsert, maxCap);

        if (added > 0) {
            stackToInsert.decrement(added);
            this.playInsertSound(player);
            return true;
        }
        return false;
    }

    protected Fraction getMaxCapacity(ItemStack stack, PlayerEntity player) {
        if (player == null || player.getEntityWorld() == null) return Fraction.getFraction(1, 1);

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var deepPockets = enchantments.getOptional(ModEnchantments.DEEP_POCKETS);

        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getLevel(deepPockets.get(), stack);
            if (level == 1) return Fraction.getFraction(2, 1);
            if (level >= 2) return Fraction.getFraction(4, 1);
        }
        return Fraction.getFraction(1, 1);
    }

    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
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

    private void playRemoveOneSound(PlayerEntity entity) {
        entity.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(PlayerEntity entity) {
        entity.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
    }
}