package com.simplebuilding.items.custom;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
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

    private static final int DRAWER_MAX_TYPES = 5;

    public ReinforcedBundleItem(Settings settings) {
        super(settings);
    }

    /**
     * Öffentliche Hilfsmethode, um den ausgewählten Index im Bundle zu setzen.
     * Ermöglicht Zugriff auf die protected Methode setSelectedStackIndex für den Packet-Handler.
     */
    public static void setBundleSelectedItem(ItemStack stack, int index) {
        setSelectedStackIndex(stack, index);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }

    // --- Helper für Click Invertierung ---
    private ClickType getInsertClick() {
        // Prüfen ob Invertierung in der Config aktiv ist
        if (Simplebuilding.getConfig().tools.invertBundleInteractions) {
            return ClickType.RIGHT;
        }
        return ClickType.LEFT;
    }

    private ClickType getRemoveClick() {
        if (Simplebuilding.getConfig().tools.invertBundleInteractions) {
            return ClickType.LEFT;
        }
        return ClickType.RIGHT;
    }
    // -------------------------------------

    @Override
    public boolean onStackClicked(ItemStack bundle, Slot slot, ClickType clickType, PlayerEntity player) {
        ClickType insertClick = getInsertClick();
        ClickType removeClick = getRemoveClick();

        if (clickType != insertClick && clickType != removeClick) return false;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        ItemStack itemInSlot = slot.getStack();

        // Logik angepasst auf dynamische ClickTypes
        if (clickType == removeClick && itemInSlot.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(bundle, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                slot.insertStack(removed);
                return true;
            }
        } else if (clickType == insertClick && !itemInSlot.isEmpty() && itemInSlot.getItem().canBeNested()) {
            int added = insertItemIntoBundle(bundle, contents, itemInSlot, getMaxCapacity(bundle, player));
            if (added > 0) {
                this.playInsertSound(player);
                itemInSlot.decrement(added);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onClicked(ItemStack bundle, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        ClickType insertClick = getInsertClick();
        ClickType removeClick = getRemoveClick();

        if (clickType != insertClick && clickType != removeClick) return false;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContentsComponent.DEFAULT;

        if (clickType == removeClick && cursorStack.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(bundle, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                cursorStackReference.set(removed);
                return true;
            }
        }
        else if (clickType == insertClick && !cursorStack.isEmpty() && cursorStack.getItem().canBeNested()) {
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

        // Dynamische Berechnung der Kapazität für den Tooltip
        Fraction frac = getMaxCapacityForVisuals(stack);
        int maxCapacity = (int) (frac.doubleValue() * 64);

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

    //
    protected int insertItemIntoBundle(ItemStack bundle, BundleContentsComponent contents, ItemStack stackToAdd, Fraction maxCap) {
        if (stackToAdd.isEmpty()) return 0;

        int drawerLevel = getDrawerLevel(bundle);

        // Drawer Restriction Check: Nur 1 Item-Typ erlaubt
        if (drawerLevel > 0) {
        boolean alreadyInBundle = false;
        int uniqueTypesCount = 0;

        // Wir zählen die einzigartigen Typen im Bundle
        List<ItemStack> distinctItems = new ArrayList<>();
        for (ItemStack s : contents.iterate()) {
            boolean isNewType = true;
            for (ItemStack distinct : distinctItems) {
                if (ItemStack.areItemsAndComponentsEqual(s, distinct)) {
                    isNewType = false;
                    break;
                }
            }
            if (isNewType) {
                distinctItems.add(s);
                uniqueTypesCount++;
            }

            // Check, ob unser neues Item schon dabei ist
            if (ItemStack.areItemsAndComponentsEqual(s, stackToAdd)) {
                alreadyInBundle = true;
            }
        }

        // REGEL: Wenn das Item NEU ist (nicht im Bundle) UND wir schon 5 oder mehr Typen haben -> Blockieren
        if (!alreadyInBundle && uniqueTypesCount >= DRAWER_MAX_TYPES) {
            return 0;
        }
    }

        // Capacity Check
        Fraction currentOccupancy = contents.getOccupancy();
        Fraction itemWeight = Fraction.getFraction(1, stackToAdd.getMaxCount());
        Fraction remainingSpace = maxCap.subtract(currentOccupancy);

        if (remainingSpace.compareTo(itemWeight) < 0) return 0;

        int maxStackSize = stackToAdd.getMaxCount();
        int countThatFits = (int) remainingSpace.multiplyBy(Fraction.getFraction(maxStackSize, 1)).doubleValue();
        int countToAdd = Math.min(stackToAdd.getCount(), countThatFits);

        if (countToAdd <= 0) return 0;

        // --- NEW SORTING & MERGING LOGIC ---

        // 1. Create a mutable list of current items
        List<ItemStack> newItems = new ArrayList<>();
        for (ItemStack s : contents.iterate()) {
            newItems.add(s.copy());
        }

        // --- MERGING LOGIC (KORRIGIERT) ---
        // Wir suchen nach existierenden Stacks, um sie zusammenzufügen und nach oben zu ziehen.

        int totalCount = countToAdd;
        int maxItemCount = stackToAdd.getMaxCount(); // Normalerweise 64

        // Wir entfernen ALLE existierenden Instanzen dieses Items aus der Liste,
        // addieren ihre Menge zum "totalCount" und fügen sie dann sauber gestapelt oben wieder ein.
        // Das ist wichtig für den Drawer, der riesige Mengen halten kann.

        // Rückwärts iterieren ist sicherer beim Entfernen, aber hier nutzen wir removeIf oder sammeln Indizes.
        // Einfacher: Neue Liste bauen ohne die Matches und Count zählen.

        List<ItemStack> itemsKept = new ArrayList<>();

        for (ItemStack stack : newItems) {
            if (ItemStack.areItemsAndComponentsEqual(stack, stackToAdd)) {
                totalCount += stack.getCount();
            } else {
                itemsKept.add(stack);
            }
        }

        // Jetzt füllen wir die Items ganz oben (Index 0) wieder auf.
        // Wir teilen "totalCount" in Stacks der Größe 64 auf.

        List<ItemStack> itemsToAddBack = new ArrayList<>();
        while (totalCount > 0) {
            int take = Math.min(totalCount, maxItemCount);
            ItemStack chunk = stackToAdd.copy();
            chunk.setCount(take);
            itemsToAddBack.add(chunk); // Zur temporären Liste
            totalCount -= take;
        }

        // Da wir an Index 0 (oben) einfügen wollen, fügen wir die neuen Stacks VOR die behaltenen.
        // itemsToAddBack enthält z.B. [64, 64, 10]. Wir wollen, dass der "angebrochene" Stack (10)
        // ganz oben liegt, damit er zuerst rausgenommen wird (LIFO - Last In First Out).
        // Das passiert automatisch, wenn wir sie der Reihe nach an Index 0 einfügen oder als Block davor setzen.

        itemsKept.addAll(0, itemsToAddBack);

        // 4. Update Bundle
        BundleItem.setSelectedStackIndex(bundle, -1);
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(itemsKept));

        return countToAdd;
    }


    private int getDrawerLevel(ItemStack stack) {
        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(ModEnchantments.DRAWER)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    protected void addToBundleList(List<ItemStack> list, ItemStack stackToAdd) {
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

    protected ItemStack removeSelectedOrFirstItem(ItemStack bundle, BundleContentsComponent contents) {
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

    protected void removeOneItemFromBundle(ItemStack bundle, BundleContentsComponent contents, int targetIndex) {
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
        // Basis: Netherite Bundle hat 2x Kapazität (128 Items), normales 1x (64 Items)
        Fraction capacity = stack.isOf(ModItems.NETHERITE_BUNDLE) ? Fraction.getFraction(2, 1) : Fraction.getFraction(1, 1);

        // Ergebnis: Normal = 96 Items, Netherite = 192 Items
        capacity = capacity.multiplyBy(Fraction.getFraction(3, 2));

        if (player == null || player.getEntityWorld() == null) return capacity;

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);

        // --- Drawer Logic ---
        var drawer = enchantments.getOptional(ModEnchantments.DRAWER);
        if (drawer.isPresent()) {
            int level = EnchantmentHelper.getLevel(drawer.get(), stack);
            if (level > 0) {
                // Formel: Basis * (1 + Level * 0.125)
                // Beispiel Level 1: 1 * (1 + 0.125) = 1.125
                // Beispiel Level 8: 1 * (1 + 1.0) = 2.0 (200%)

                // Wir nutzen Brüche: 0.125 = 1/8.
                // Multiplikator = 1 + (level/8) = (8+level)/8
                Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                capacity = capacity.multiplyBy(drawerBonus);
            }
        }

        // --- Deep Pockets Logic ---
        var deepPockets = enchantments.getOptional(ModEnchantments.DEEP_POCKETS);

        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getLevel(deepPockets.get(), stack);
            if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
            if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
        }
        return capacity;
    }

    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
        Fraction capacity = stack.isOf(ModItems.NETHERITE_BUNDLE) ? Fraction.getFraction(2, 1) : Fraction.getFraction(1, 1);
        capacity = capacity.multiplyBy(Fraction.getFraction(3, 2));

        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().getKey().isPresent()) {
                String id = entry.getKey().getKey().get().getValue().toString();

                // --- Drawer Visuals ---
                if (id.contains("drawer")) {
                    int level = entry.getIntValue();
                    if (level > 0) {
                        // Gleiche Formel wie oben: (8 + level) / 8
                        Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                        capacity = capacity.multiplyBy(drawerBonus);
                    }
                }
                // Deep Pockets
                if (id.contains("deep_pockets")) {
                    int level = entry.getIntValue();
                    if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
                    if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
                }

            }
        }
        return capacity;
    }

    protected void playRemoveOneSound(PlayerEntity entity) {
        entity.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
    }

    protected void playInsertSound(PlayerEntity entity) {
        entity.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
    }

    public boolean canAutoPickup(ItemStack bundle, ItemStack itemOnGround, net.minecraft.world.World world) {
        var registry = world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var funnel = enchantments.getOptional(ModEnchantments.FUNNEL);

        if (funnel.isEmpty()) return false;

        int level = EnchantmentHelper.getLevel(funnel.get(), bundle);

        if (level <= 0) return false; // Kein Funnel

        if (level == 1) {
            // LEVEL 1: Nur aufheben, wenn Item schon im Bundle ist (Filter)
            BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null) return false;

            for (ItemStack s : contents.iterate()) {
                if (ItemStack.areItemsAndComponentsEqual(s, itemOnGround)) {
                    return true; // Match gefunden, darf aufheben
                }
            }
            return false; // Nicht im Bundle, liegen lassen
        }

        // LEVEL 2+: Alles aufheben (wie bisher)
        return true;
    }
}