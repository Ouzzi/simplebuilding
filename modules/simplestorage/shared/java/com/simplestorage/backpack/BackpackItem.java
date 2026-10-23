package com.simplestorage.backpack;

import com.simplelib.api.AccessorySlots;
import com.simplestorage.bundle.BundleEnchantments;
import com.simplestorage.registry.StorageComponents;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Die vier Rucksaecke (split stage 8c-2, docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md: from SimpleBuilding's
 * {@code BackpackItem}, old {@code simplebuilding:} ids migrate through SimpleLib IdMoves). Ein Rucksack ist zugleich Brust-Ausruestung und Block.
 *
 * <ul>
 *   <li><b>Anziehen:</b> Rechtsklick mit dem Rucksack in der Hand legt ihn in den Brust-Slot. Das
 *       macht Vanillas {@code Item#use} ueber die {@code EQUIPPABLE}-Komponente (swappable) - genau
 *       wie bei einem Brustpanzer, samt Tausch mit einem getragenen Brustpanzer oder einer Elytra.
 *       Der Inhalt reist in der Komponente {@code simplestorage:backpack_contents} mit.</li>
 *   <li><b>Abstellen:</b> Schleichen + Rechtsklick auf einen Block stellt ihn als Block ab
 *       ({@link #useOn}). Ohne Schleichen gibt {@code useOn} PASS zurueck, damit Vanilla danach
 *       {@code use} aufruft und der Rucksack angezogen wird statt abgestellt.</li>
 *   <li><b>Oeffnen:</b> ueber die Rucksack-Taste (Standard B) - der getragene Rucksack, sonst seit
 *       2026-09-28 der erste im Inventar ({@link #carriedBackpackSlot}) -, oder als abgestellter
 *       Block per Rechtsklick.</li>
 * </ul>
 *
 * <p>Rucksaecke passen in keinen Behaelter ({@link #canFitInsideContainerItems()}): weder in
 * Shulkerkisten noch in Buendel noch in einen anderen Rucksack.
 */
public class BackpackItem extends BlockItem {
    /** So viele Eintraege nennt der Tooltip namentlich, der Rest wird gezaehlt. */
    private static final int TOOLTIP_ENTRIES = 5;

    private final BackpackTier tier;

    public BackpackItem(BackpackTier tier, Block block, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    public BackpackTier getTier() {
        return this.tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return super.useOn(context);
        }
        // Kein Schleichen: nicht abstellen. PASS laesst Vanilla anschliessend Item#use aufrufen,
        // und das zieht den Rucksack ueber die EQUIPPABLE-Komponente an.
        return InteractionResult.PASS;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /**
     * Ein zerstoertes Rucksack-Item (Lava, Kaktus, Void ausgenommen) verliert seinen Inhalt nicht:
     * alles faellt als Item-Entities heraus, wie bei einer Shulkerkiste. Uebergrosse Stapel aus
     * Tiefe Taschen werden dabei in normale Stapel geteilt - ein Item-Entity mit mehr als 99
     * Gegenstaenden liesse sich nicht speichern.
     */
    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        BackpackContents contents = stack.get(StorageComponents.BACKPACK_CONTENTS);
        if (contents != null) {
            stack.remove(StorageComponents.BACKPACK_CONTENTS);
            Level level = itemEntity.level();
            if (!level.isClientSide()) {
                for (ItemStack content : contents.stacks()) {
                    for (ItemStack part : splitIntoNormalStacks(content)) {
                        level.addFreshEntity(new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), part));
                    }
                }
            }
        }
        super.onDestroyed(itemEntity);
    }

    /**
     * Tooltip-Bild: der Inhalt als Raster (mit Umschalt) bzw. der Umschalt-Hinweis. Ein leerer
     * Rucksack hat nichts zu zeigen und bekommt kein Bild.
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BackpackContents contents = contents(stack);
        if (contents.entries().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BackpackTooltipData(contents, this.tier.slotCount()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        BackpackContents contents = stack.getOrDefault(StorageComponents.BACKPACK_CONTENTS, BackpackContents.EMPTY);
        builder.accept(Component.translatable("tooltip.simplestorage.backpack.slots", contents.size(), this.tier.slotCount())
                .withStyle(ChatFormatting.GRAY));
        List<ItemStack> stacks = contents.stacks();
        for (int i = 0; i < Math.min(TOOLTIP_ENTRIES, stacks.size()); i++) {
            ItemStack content = stacks.get(i);
            builder.accept(Component.literal(content.getCount() + "x ").append(content.getHoverName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (stacks.size() > TOOLTIP_ENTRIES) {
            builder.accept(Component.translatable("tooltip.simplestorage.backpack.more", stacks.size() - TOOLTIP_ENTRIES)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        // Component.keybind loest der Client auf - der gemeinsame Code fasst so keine Client-Klasse an.
        builder.accept(Component.translatable("tooltip.simplestorage.backpack.open_hint",
                Component.keybind("key.simplestorage.open_backpack")).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.simplestorage.backpack.place_hint").withStyle(ChatFormatting.GRAY));
    }

    // =================================================================================
    // Helfer, die Menue, Block, Verzauberungen und Netzwerk teilen
    // =================================================================================

    /**
     * The worn backpack: the one in the chest slot, else one in an accessory slot (Curios/Trinkets,
     * {@link AccessorySlots}), else {@link ItemStack#EMPTY}. The live stack in either case.
     */
    public static ItemStack wornBackpack(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof BackpackItem ? chest : accessoryBackpack(entity);
    }

    /** The backpack in an accessory slot (Curios/Trinkets), or EMPTY - also without an accessory mod. */
    public static ItemStack accessoryBackpack(LivingEntity entity) {
        return AccessorySlots.findFirst(entity, s -> s.getItem() instanceof BackpackItem);
    }

    /**
     * Pseudo slot index for "the backpack in an accessory slot" ({@link #carriedBackpackSlot},
     * {@link BackpackOpenData#lockedSlot()}). Not an inventory index, so the
     * backpack menu locks no inventory slot for it - the accessory slots are not part of that menu.
     */
    public static final int ACCESSORY_SLOT = -2;

    /** The backpack stack behind a slot index from {@link #carriedBackpackSlot} (live, may be EMPTY). */
    public static ItemStack carriedStack(Player player, int slot) {
        if (slot == ACCESSORY_SLOT) {
            return accessoryBackpack(player);
        }
        return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    /** Inventar-Index des Brust-Slots ({@code Inventory#getItem}): 36 + Index der Brust = 38. */
    public static final int CHEST_INVENTORY_SLOT = net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE + EquipmentSlot.CHEST.getIndex();

    /**
     * Inventar-Index des Rucksacks, den die Rucksack-Taste oeffnet (Besitzer 2026-09-28): der
     * getragene zuerst, sonst der erste im Inventar (Schnellleiste, Hauptinventar, dann Nebenhand);
     * -1 ohne Rucksack. Client (ob die Taste fragt) und Server (was geoeffnet wird) rechnen dasselbe.
     * A backpack in an accessory slot counts as worn: after the chest slot, before the inventory
     * ({@link #ACCESSORY_SLOT}, read it back with {@link #carriedStack}).
     */
    public static int carriedBackpackSlot(Player player) {
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        if (inventory.getItem(CHEST_INVENTORY_SLOT).getItem() instanceof BackpackItem) {
            return CHEST_INVENTORY_SLOT;
        }
        if (!accessoryBackpack(player).isEmpty()) {
            return ACCESSORY_SLOT;
        }
        for (int slot = 0; slot < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).getItem() instanceof BackpackItem) {
                return slot;
            }
        }
        return inventory.getItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND).getItem() instanceof BackpackItem
                ? net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND : -1;
    }

    /**
     * Darf {@code stack} in einen Rucksack-Slot? Keine Rucksaecke, keine Shulkerkisten, nichts aus
     * dem Item-Tag {@code simplestorage:not_allowed_in_backpack} (fuer Modpacks, standardmaessig leer; SimpleBuildings
     * alter Tag {@code simplebuilding:not_allowed_in_backpack} gilt weiter, falls ein Modpack ihn fuellt).
     */
    public static boolean mayStore(ItemStack stack) {
        return !(stack.getItem() instanceof BackpackItem) && stack.getItem().canFitInsideContainerItems()
                && !stack.is(NOT_ALLOWED) && !stack.is(OLD_NOT_ALLOWED);
    }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> NOT_ALLOWED = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, com.simplestorage.SimpleStorage.id("not_allowed_in_backpack"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> OLD_NOT_ALLOWED = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, com.simplestorage.SimpleStorage.old("not_allowed_in_backpack"));

    /**
     * Stapelfaktor aus Tiefe Taschen: Stufe I verdoppelt, Stufe II (oder hoeher) vervierfacht -
     * dieselben Stufen wie die Kapazitaet eines Buendels mit Tiefe Taschen.
     *
     * <p>Warum genau x2/x4: Tiefe Taschen soll auf allen Behaeltern der Mod dasselbe bedeuten
     * (Buendel und Koecher verdoppeln bzw. vervierfachen ihre Kapazitaet), und das Vierfache der
     * groessten Vanilla-Stapelgroesse ist genau die Zaehlgrenze, die der Rucksack-Inhalt speichern
     * kann ({@link BackpackContents#MAX_COUNT} = 99 x 4). Eine dritte Stufe haette keinen Platz.
     */
    public static int stackMultiplier(int deepPocketsLevel) {
        if (deepPocketsLevel >= 2) {
            return 4;
        }
        return deepPocketsLevel == 1 ? 2 : 1;
    }

    public static int stackMultiplier(ItemStack backpack, Level level) {
        return stackMultiplier(BundleEnchantments.level(backpack, level, BundleEnchantments.DEEP_POCKETS));
    }

    /** Stapelfaktor aus einer Verzauberungsliste (abgestellter Rucksack: Komponenten des Blocks). */
    public static int stackMultiplier(ItemEnchantments enchantments) {
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(BundleEnchantments.DEEP_POCKETS)) {
                return stackMultiplier(entry.getIntValue());
            }
        }
        return 1;
    }

    /** Groesster Stapel, den ein Rucksack-Slot mit diesem Faktor von {@code stack} haelt. */
    public static int maxStackSizeIn(ItemStack stack, int multiplier) {
        int normal = stack.getMaxStackSize();
        // Nicht stapelbare Items bleiben bei 1: ein "Stapel" aus zwei Schwertern wuerde ueberall
        // dort brechen, wo Vanilla Stapelgroesse 1 annimmt.
        return normal > 1 ? normal * multiplier : 1;
    }

    /** Teilt einen (moeglicherweise uebergrossen) Stapel in Stapel normaler Groesse. */
    public static List<ItemStack> splitIntoNormalStacks(ItemStack stack) {
        List<ItemStack> parts = new java.util.ArrayList<>();
        ItemStack rest = stack.copy();
        while (!rest.isEmpty()) {
            parts.add(rest.split(Math.max(1, rest.getMaxStackSize())));
        }
        return parts;
    }

    /** Traegt {@code player} einen Rucksack mit der Verzauberung {@code key}? Liefert ihn oder EMPTY. */
    public static ItemStack wornBackpackWith(LivingEntity entity, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        ItemStack worn = wornBackpack(entity);
        return !worn.isEmpty() && BundleEnchantments.has(worn, entity.level(), key) ? worn : ItemStack.EMPTY;
    }

    // =================================================================================
    // Direkter Zugriff auf die Komponente (Trichter; Meisterbauer und Konstrukteurs Hand von SimpleBuilding ueber
    // SimpleLib CarriedStacks, siehe SimpleStorage#registerCarried).
    // Ein offenes Menue auf diesem Rucksack laedt danach neu (WornBackpackContainer#syncFromSource).
    // =================================================================================

    public static BackpackContents contents(ItemStack backpack) {
        return backpack.getOrDefault(StorageComponents.BACKPACK_CONTENTS, BackpackContents.EMPTY);
    }

    private static void setContents(ItemStack backpack, BackpackContents contents) {
        if (contents.isEmpty()) {
            backpack.remove(StorageComponents.BACKPACK_CONTENTS);
        } else {
            backpack.set(StorageComponents.BACKPACK_CONTENTS, contents);
        }
    }

    /** Index des ersten Eintrags, dessen Stapel {@code test} erfuellt, sonst -1. */
    public static int findEntry(ItemStack backpack, java.util.function.Predicate<ItemStack> test) {
        return contents(backpack).indexOf(test);
    }

    /** Kopie des Stapels am Eintrag {@code index}. */
    public static ItemStack entryStack(ItemStack backpack, int index) {
        return contents(backpack).stackAt(index);
    }

    /** Kopien aller Stapel im Rucksack. */
    public static List<ItemStack> entryStacks(ItemStack backpack) {
        return contents(backpack).stacks();
    }

    /** Ersetzt den Stapel am Eintrag {@code index} (etwa einen Koecher, aus dem ein Pfeil genommen wurde). */
    public static void replaceEntry(ItemStack backpack, int index, ItemStack stack) {
        BackpackContents contents = contents(backpack);
        if (index >= 0 && index < contents.size()) {
            setContents(backpack, contents.withStackAt(index, stack));
        }
    }

    /**
     * Ersetzt den Stapel im Komponenten-Slot {@code slot} (stabile Id statt Listenindex; leer = Eintrag weg): so schreibt
     * SimpleLib {@code CarriedStacks} zurueck, auch wenn ein anderer Eintrag inzwischen verschwunden ist.
     */
    public static void replaceSlot(ItemStack backpack, int slot, ItemStack stack) {
        BackpackContents contents = contents(backpack);
        List<BackpackContents.Entry> entries = contents.entries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).slot() == slot) {
                setContents(backpack, contents.withStackAt(i, stack));
                return;
            }
        }
        if (!stack.isEmpty()) {
            List<BackpackContents.Entry> copy = new java.util.ArrayList<>(entries);
            copy.add(BackpackContents.Entry.of(slot, stack));
            setContents(backpack, new BackpackContents(copy, contents.unreadable()));
        }
    }

    /**
     * Die Eintraege des getragenen Rucksacks fuer SimpleLib {@code CarriedStacks} (Kopien, Rueckschreiben ueber
     * {@link #replaceSlot}, Behaelter = der lebende Rucksack): SimpleBuildings Bauzauberstab, Pick-Taste und Konstrukteurs Hand
     * sowie die Koecher fragen dort nach ihrer Verzauberung am Rucksack (Meisterbauer, Konstrukteurs Hand).
     */
    public static List<com.simplelib.api.CarriedStacks.Entry> carriedEntries(Player player) {
        ItemStack worn = wornBackpack(player);
        if (worn.isEmpty()) return List.of();
        List<com.simplelib.api.CarriedStacks.Entry> out = new java.util.ArrayList<>();
        for (BackpackContents.Entry entry : contents(worn).entries()) {
            int slot = entry.slot();
            out.add(new com.simplelib.api.CarriedStacks.Entry(entry.toStack(), changed -> replaceSlot(worn, slot, changed), worn));
        }
        return out;
    }

    /** Nimmt ein Stueck aus dem Eintrag {@code index}. */
    public static void consumeOne(ItemStack backpack, int index) {
        take(backpack, index, 1);
    }

    /** Nimmt bis zu {@code amount} Stueck aus dem Eintrag {@code index} und liefert sie. */
    public static ItemStack take(ItemStack backpack, int index, int amount) {
        BackpackContents contents = contents(backpack);
        if (index < 0 || index >= contents.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = contents.stackAt(index);
        ItemStack taken = stack.split(amount);
        setContents(backpack, contents.withStackAt(index, stack));
        return taken;
    }

    /**
     * Trichter am getragenen Rucksack: legt {@code ground} (ganz oder teilweise) in den Rucksack,
     * wie der Trichter es bei Buendeln tut - Stufe I nur Sorten, die schon drin liegen, Stufe II
     * alles. Verkleinert {@code ground} um das Eingelegte.
     *
     * @return ob etwas eingelegt wurde
     */
    public static boolean tryFunnelPickup(Player player, ItemStack ground) {
        ItemStack worn = wornBackpack(player);
        if (worn.isEmpty() || ground.isEmpty() || !mayStore(ground)) {
            return false;
        }
        int funnel = BundleEnchantments.level(worn, player.level(), BundleEnchantments.FUNNEL);
        if (funnel <= 0) {
            return false;
        }
        BackpackItem item = (BackpackItem) worn.getItem();
        BackpackContainer view =
                new BackpackContainer(item.getTier(), stackMultiplier(worn, player.level()));
        view.load(contents(worn));
        if (funnel == 1 && !view.containsSameItem(ground)) {
            return false;
        }
        if (view.insert(ground) <= 0) {
            return false;
        }
        setContents(worn, view.toContents());
        player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        return true;
    }
}
