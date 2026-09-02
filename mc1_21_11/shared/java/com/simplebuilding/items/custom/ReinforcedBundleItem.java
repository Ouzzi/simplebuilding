package com.simplebuilding.items.custom;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.BlockHitResult;

import static com.simplebuilding.util.EnchantmentHelper.*;

public class ReinforcedBundleItem extends BundleItem {

    private static final int DRAWER_MAX_TYPES = 5;

    // MC 1.21.11: BundleContents haelt noch direkt ItemStacks (ItemStackTemplate gibt es erst ab 26.2)
    // und weight() liefert die Fraction unmittelbar statt als DataResult.
    private static Fraction bundleWeight(BundleContents contents) {
        return contents.weight();
    }

    private static List<ItemStack> stacksAsTemplates(List<ItemStack> stacks) {
        return List.copyOf(stacks);
    }

    private static List<ItemStack> itemsAsStacks(BundleContents contents) {
        return contents.itemCopyStream().toList();
    }

    private static ItemStack getItemAt(BundleContents contents, int index) {
        return contents.getItemUnsafe(index).copy();
    }

    public ReinforcedBundleItem(Properties settings) {
        super(settings);
    }

    /**
     * Öffentliche Hilfsmethode, um den ausgewählten Index im Bundle zu setzen.
     * Ermöglicht Zugriff auf die protected Methode setSelectedStackIndex für den Packet-Handler.
     */
    public static void setBundleSelectedItem(ItemStack stack, int index) {
        toggleSelectedItem(stack, index);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return true;
    }

    // --- Helper für Click Invertierung ---
    private ClickAction getInsertClick() {
        // Prüfen ob Invertierung in der Config aktiv ist
        if (Simplebuilding.getConfig().tools.invertBundleInteractions) {
            return ClickAction.SECONDARY;
        }
        return ClickAction.PRIMARY;
    }

    private ClickAction getRemoveClick() {
        if (Simplebuilding.getConfig().tools.invertBundleInteractions) {
            return ClickAction.PRIMARY;
        }
        return ClickAction.SECONDARY;
    }
    // -------------------------------------

    @Override
    public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction clickAction, Player player) {
        ClickAction insertClick = getInsertClick();
        ClickAction removeClick = getRemoveClick();

        if (clickAction != insertClick && clickAction != removeClick) return false;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContents.EMPTY;

        ItemStack itemInSlot = slot.getItem();

        // Logik angepasst auf dynamische ClickTypes
        if (clickAction == removeClick && itemInSlot.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(bundle, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                slot.safeInsert(removed);
                return true;
            }
        } else if (clickAction == insertClick && !itemInSlot.isEmpty() && itemInSlot.getItem().canFitInsideContainerItems()) {
            int added = insertItemIntoBundle(bundle, contents, itemInSlot, getMaxCapacity(bundle, player));
            if (added > 0) {
                this.playInsertSound(player);
                itemInSlot.shrink(added);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bundle, ItemStack cursorStack, Slot slot, ClickAction clickAction, Player player, SlotAccess cursorStackReference) {
        ClickAction insertClick = getInsertClick();
        ClickAction removeClick = getRemoveClick();

        if (clickAction != insertClick && clickAction != removeClick) return false;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContents.EMPTY;

        if (clickAction == removeClick && cursorStack.isEmpty()) {
            ItemStack removed = removeSelectedOrFirstItem(bundle, contents);
            if (!removed.isEmpty()) {
                this.playRemoveOneSound(player);
                cursorStackReference.set(removed);
                return true;
            }
        }
        else if (clickAction == insertClick && !cursorStack.isEmpty() && cursorStack.getItem().canFitInsideContainerItems()) {
            int added = insertItemIntoBundle(bundle, contents, cursorStack, getMaxCapacity(bundle, player));
            if (added > 0) {
                this.playInsertSound(player);
                cursorStack.shrink(added);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack bundleStack = context.getItemInHand();

        assert player != null;

        boolean hasMasterBuilder = hasEnchantment(bundleStack, player.level(), ModEnchantments.MASTER_BUILDER);
        if (hasMasterBuilder) {
            BundleContents contents = bundleStack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null && !contents.isEmpty()) {

                int index = contents.getSelectedItem();
                if (index == -1 || index >= contents.size()) index = 0;

                boolean hasColorPalette = hasEnchantment(bundleStack, player.level(), ModEnchantments.COLOR_PALETTE);
                if (hasColorPalette) {
                    index = player.level().getRandom().nextInt(contents.size());
                }

                ItemStack blockToPlace = getItemAt(contents, index).copy();

                if (blockToPlace.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                    net.minecraft.world.item.context.UseOnContext blockContext = new net.minecraft.world.item.context.UseOnContext(
                            context.getLevel(),
                            context.getPlayer(),
                            context.getHand(),
                            blockToPlace,
                            new BlockHitResult(
                                    context.getClickLocation(),
                                    context.getClickedFace(),
                                    context.getClickedPos(),
                                    false
                            )
                    );

                    InteractionResult result = blockItem.useOn(blockContext);

                    if (result.consumesAction()) {
                        if (!player.getAbilities().instabuild) {
                            removeOneItemFromBundle(bundleStack, contents, index);
                        }
                        return result;
                    }
                }
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(net.minecraft.world.level.Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);

        if (contents != null && !contents.isEmpty()) {
            int index = contents.getSelectedItem();
            if (index == -1 || index >= contents.size()) index = 0;

            ItemStack itemToDrop = getItemAt(contents, index);

            if (itemToDrop.getItem() instanceof net.minecraft.world.item.BlockItem) {
                return net.minecraft.world.InteractionResult.FAIL;
            }

            if (!world.isClientSide()) {
                ItemStack removed = removeSelectedOrFirstItem(stack, contents);
                user.drop(removed, true);
                this.playRemoveOneSound(user);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        return super.use(world, user, hand);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents != null && !contents.isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BundleContents data = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (data == null) return 0;
        Fraction current = bundleWeight(data);
        Fraction max = getMaxCapacityForVisuals(stack);
        float fillLevel = Math.min(1.0f, current.divideBy(max).floatValue());
        return Math.round(fillLevel * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        BundleContents data = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (data == null) return super.getBarColor(stack);
        Fraction current = bundleWeight(data);
        Fraction max = getMaxCapacityForVisuals(stack);
        float fillLevel = Math.min(1.0f, current.divideBy(max).floatValue());
        return Mth.hsvToRgb(Math.max(0.0F, (1.0F - fillLevel)) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return Optional.empty();

        // Dynamische Berechnung der Kapazität für den Tooltip
        Fraction frac = getMaxCapacityForVisuals(stack);
        int maxCapacity = (int) (frac.doubleValue() * 64);

        return Optional.of(new ReinforcedBundleTooltipData(contents, maxCapacity));
    }

    protected int insertItemIntoBundle(ItemStack bundle, BundleContents contents, ItemStack stackToAdd, Fraction maxCap) {
        if (stackToAdd.isEmpty()) return 0;

        int drawerLevel = getDrawerLevel(bundle);

        // Drawer Restriction Check: Nur 1 Item-Typ erlaubt
        if (drawerLevel > 0) {
        boolean alreadyInBundle = false;
        int uniqueTypesCount = 0;

        // Wir zählen die einzigartigen Typen im Bundle
        List<ItemStack> distinctItems = new ArrayList<>();
        for (ItemStack s : itemsAsStacks(contents)) {
            boolean isNewType = true;
            for (ItemStack distinct : distinctItems) {
                if (ItemStack.isSameItemSameComponents(s, distinct)) {
                    isNewType = false;
                    break;
                }
            }
            if (isNewType) {
                distinctItems.add(s);
                uniqueTypesCount++;
            }

            // Check, ob unser neues Item schon dabei ist
            if (ItemStack.isSameItemSameComponents(s, stackToAdd)) {
                alreadyInBundle = true;
            }
        }

        // REGEL: Wenn das Item NEU ist (nicht im Bundle) UND wir schon 5 oder mehr Typen haben -> Blockieren
        if (!alreadyInBundle && uniqueTypesCount >= DRAWER_MAX_TYPES) {
            return 0;
        }
    }

        // Capacity Check
        Fraction currentOccupancy = bundleWeight(contents);
        Fraction itemWeight = Fraction.getFraction(1, stackToAdd.getMaxStackSize());
        Fraction remainingSpace = maxCap.subtract(currentOccupancy);

        if (remainingSpace.compareTo(itemWeight) < 0) return 0;

        int maxStackSize = stackToAdd.getMaxStackSize();
        int countThatFits = (int) remainingSpace.multiplyBy(Fraction.getFraction(maxStackSize, 1)).doubleValue();
        int countToAdd = Math.min(stackToAdd.getCount(), countThatFits);

        if (countToAdd <= 0) return 0;

        // --- NEW SORTING & MERGING LOGIC ---

        // 1. Create a mutable list of current items
        List<ItemStack> newItems = new ArrayList<>();
        for (ItemStack s : itemsAsStacks(contents)) {
            newItems.add(s.copy());
        }

        // --- MERGING LOGIC (KORRIGIERT) ---
        // Wir suchen nach existierenden Stacks, um sie zusammenzufügen und nach oben zu ziehen.

        int totalCount = countToAdd;
        int maxItemCount = stackToAdd.getMaxStackSize(); // Normalerweise 64

        // Wir entfernen ALLE existierenden Instanzen dieses Items aus der Liste,
        // addieren ihre Menge zum "totalCount" und fügen sie dann sauber gestapelt oben wieder ein.
        // Das ist wichtig für den Drawer, der riesige Mengen halten kann.

        // Rückwärts iterieren ist sicherer beim Entfernen, aber hier nutzen wir removeIf oder sammeln Indizes.
        // Einfacher: Neue Liste bauen ohne die Matches und Count zählen.

        List<ItemStack> itemsKept = new ArrayList<>();

        for (ItemStack stack : newItems) {
            if (ItemStack.isSameItemSameComponents(stack, stackToAdd)) {
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
        BundleItem.toggleSelectedItem(bundle, -1);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(stacksAsTemplates(itemsKept)));

        return countToAdd;
    }

    protected void addToBundleList(List<ItemStack> list, ItemStack stackToAdd) {
        if (!list.isEmpty()) {
            ItemStack topStack = list.getFirst();
            if (ItemStack.isSameItemSameComponents(topStack, stackToAdd)) {
                int available = topStack.getMaxStackSize() - topStack.getCount();
                int toMerge = Math.min(available, stackToAdd.getCount());
                if (toMerge > 0) {
                    topStack.grow(toMerge);
                    stackToAdd.shrink(toMerge);
                }
            }
        }
        if (!stackToAdd.isEmpty()) {
            list.addFirst(stackToAdd);
        }
    }

    protected ItemStack removeSelectedOrFirstItem(ItemStack bundle, BundleContents contents) {
        if (contents.isEmpty()) return ItemStack.EMPTY;

        int selectedIndex = contents.getSelectedItem();
        if (selectedIndex == -1) selectedIndex = 0;
        if (selectedIndex >= contents.size()) selectedIndex = 0;

        ItemStack itemToRemove = getItemAt(contents, selectedIndex).copy();

        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStack s : itemsAsStacks(contents)) {
            if (i != selectedIndex) {
                newItems.add(s.copy());
            }
            i++;
        }

        BundleItem.toggleSelectedItem(bundle, -1);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(stacksAsTemplates(newItems)));
        return itemToRemove;
    }

    protected void removeOneItemFromBundle(ItemStack bundle, BundleContents contents, int targetIndex) {
        if (contents.isEmpty()) return;

        List<ItemStack> newItems = new ArrayList<>();
        for (ItemStack s : itemsAsStacks(contents)) newItems.add(s.copy());

        if (targetIndex < newItems.size()) {
            ItemStack targetStack = newItems.get(targetIndex);
            targetStack.shrink(1);
            if (targetStack.isEmpty()) {
                newItems.remove(targetIndex);
            }
        }

        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(stacksAsTemplates(newItems)));
    }

    public boolean tryInsertStackFromWorld(ItemStack bundle, ItemStack stackToInsert, Player player) {
        if (!stackToInsert.getItem().canFitInsideContainerItems()) return false;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) contents = BundleContents.EMPTY;

        Fraction maxCap = getMaxCapacity(bundle, player);

        int added = insertItemIntoBundle(bundle, contents, stackToInsert, maxCap);

        if (added > 0) {
            stackToInsert.shrink(added);
            this.playInsertSound(player);
            return true;
        }
        return false;
    }

    protected Fraction getTierCapacityMultiplier(ItemStack stack) {
        return getTierCapacityMultiplier(stack.getItem());
    }

    /**
     * Dieselbe Stufentabelle ohne Stack. Im Datagen laesst sich keiner bauen:
     * der ItemStack-Konstruktor liest ab MC 26.2 die Komponenten, die dort noch
     * nicht gebunden sind. Der Wiki-Export braucht die Stufe trotzdem, und eine
     * zweite Tabelle im Generator waere eine zweite Wahrheit.
     */
    protected Fraction getTierCapacityMultiplier(Item item) {
        if (item == ModItems.ENDERITE_BUNDLE) {
            return Fraction.getFraction(3, 1);
        }
        if (item == ModItems.NETHERITE_BUNDLE) {
            return Fraction.getFraction(2, 1);
        }
        return Fraction.getFraction(1, 1);
    }

    /**
     * Grundkapazitaet als Bruch eines Vanilla-Buendels, ohne Verzauberungen.
     * Buendel fassen das 1,5-fache ihrer Stufe; Koecher ueberschreiben das.
     */
    protected Fraction getBaseCapacity(Item item) {
        return getTierCapacityMultiplier(item).multiplyBy(Fraction.getFraction(3, 2));
    }

    /** Grundkapazitaet in Gegenstaenden bei 64er-Stapeln - fuer den Wiki-Export. */
    public int getBaseCapacityItems() {
        return getBaseCapacity(this).multiplyBy(Fraction.getFraction(64, 1)).intValue();
    }

    protected Fraction getMaxCapacity(ItemStack stack, Player player) {
        // Ergebnis: Normal = 96, Netherite = 192, Enderite = 288 Items
        Fraction capacity = getBaseCapacity(stack.getItem());

        if (player == null || player.level() == null) return capacity;

        var registry = player.level().registryAccess();
        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);

        // --- Drawer Logic ---
        var drawer = enchantments.get(ModEnchantments.DRAWER);
        if (drawer.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(drawer.get(), stack);
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
        var deepPockets = enchantments.get(ModEnchantments.DEEP_POCKETS);

        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(deepPockets.get(), stack);
            if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
            if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
        }
        return capacity;
    }

    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
        Fraction capacity = getBaseCapacity(stack.getItem());

        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().unwrapKey().isPresent()) {
                String id = entry.getKey().unwrapKey().get().identifier().toString();

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

    protected void playRemoveOneSound(Player entity) {
        entity.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    protected void playInsertSound(Player entity) {
        entity.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    public boolean canAutoPickup(ItemStack bundle, ItemStack itemOnGround, net.minecraft.world.level.Level world) {
        var registry = world.registryAccess();
        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var funnel = enchantments.get(ModEnchantments.FUNNEL);

        if (funnel.isEmpty()) return false;

        int level = EnchantmentHelper.getItemEnchantmentLevel(funnel.get(), bundle);

        if (level <= 0) return false; // Kein Funnel

        if (level == 1) {
            // LEVEL 1: Nur aufheben, wenn Item schon im Bundle ist (Filter)
            BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
            if (contents == null) return false;

            for (ItemStack s : itemsAsStacks(contents)) {
                if (ItemStack.isSameItemSameComponents(s, itemOnGround)) {
                    return true; // Match gefunden, darf aufheben
                }
            }
            return false; // Nicht im Bundle, liegen lassen
        }

        // LEVEL 2+: Alles aufheben (wie bisher)
        return true;
    }
}