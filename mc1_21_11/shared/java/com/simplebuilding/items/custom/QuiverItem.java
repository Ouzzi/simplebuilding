package com.simplebuilding.items.custom;

import com.simplebuilding.compat.accessory.AccessorySlots;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class QuiverItem extends ReinforcedBundleItem {

    public QuiverItem(Properties settings) {
        super(settings);
    }

    // Rechtsklick bleibt wirkungslos, weil der geerbte Buendel-Rechtsklick Pfeile auf den Boden
    // werfen wuerde. Damit kommt Item#use nie an - und deshalb traegt die EQUIPPABLE-Komponente in
    // ModItems#quiverChestSlot kein swappable: Angelegt wird der Koecher im Inventarbildschirm.
    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    // Der Pfeilfilter haengt am KONFIGURIERTEN Einlegeklick, nicht an einer festen Konstante:
    // mit tools.invertBundleInteractions wandert das Einlegen auf SECONDARY, und ein Filter auf
    // PRIMARY liesse dann beliebiges Material in den Koecher.
    @Override
    public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction clickAction, Player player) {
        if (clickAction == getInsertClick() && !slot.getItem().isEmpty()) {
            if (!slot.getItem().is(ItemTags.ARROWS)) return false;
        }
        return super.overrideStackedOnOther(bundle, slot, clickAction, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bundle, ItemStack cursorStack, Slot slot, ClickAction clickAction, Player player, SlotAccess cursorStackReference) {
        if (clickAction == getInsertClick() && !cursorStack.isEmpty()) {
            if (!cursorStack.is(ItemTags.ARROWS)) {
                clearBundleSelection(bundle); // falls through to a swap, closed as in super
                return false;
            }
        }
        return super.overrideOtherStackedOnMe(bundle, cursorStack, slot, clickAction, player, cursorStackReference);
    }

    @Override
    public boolean tryInsertStackFromWorld(ItemStack bundle, ItemStack stackToInsert, Player player) {
        if (!stackToInsert.is(ItemTags.ARROWS)) return false;
        return super.tryInsertStackFromWorld(bundle, stackToInsert, player);
    }

    @Override
    protected Fraction getTierCapacityMultiplier(Item item) {
        if (item == ModItems.ENDERITE_QUIVER) {
            return Fraction.getFraction(3, 1);
        }
        if (item == ModItems.NETHERITE_QUIVER) {
            return Fraction.getFraction(2, 1);
        }
        // Der verstaerkte Koecher sitzt zwischen Koecher und Netherit-Koecher; 3/2 entspricht dem
        // Abstand vom Vanilla-Buendel zum verstaerkten Buendel und laesst die anderen drei Stufen,
        // wie sie waren.
        if (item == ModItems.REINFORCED_QUIVER) {
            return Fraction.getFraction(3, 2);
        }
        return Fraction.getFraction(1, 1);
    }

    /**
     * Koecher bekommen den 1,5-Faktor des Buendels NICHT obendrauf: ihre Grundkapazitaet ist allein
     * der Stufenfaktor, also 64 / 96 / 128 / 192 Pfeile. Die 3/2 des verstaerkten Koechers sind sein
     * Stufenfaktor, kein Buendel-Bonus.
     */
    @Override
    protected Fraction getBaseCapacity(Item item) {
        return getTierCapacityMultiplier(item);
    }

    @Override
    protected Fraction getMaxCapacity(ItemStack stack, Player player) {
        Fraction capacity = getBaseCapacity(stack.getItem());

        if (player == null || player.level() == null) return capacity;

        var registry = player.level().registryAccess();
        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);

        // 1. DRAWER Logic (Hinzugefügt!)
        var drawer = enchantments.get(ModEnchantments.DRAWER);
        if (drawer.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(drawer.get(), stack);
            if (level > 0) {
                // Multiplikator = (16 + level) / 8 - dieselbe Steigerung wie beim Buendel,
                // und mit derselben offenen Frage: siehe den Kommentar in
                // ReinforcedBundleItem#getMaxCapacity. Kurz: Commit ce9f495 hat die 8 wortlos
                // zur 16 gemacht, ob gewollt sagt keine Quelle.
                Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                capacity = capacity.multiplyBy(drawerBonus);
            }
        }

        // 2. Deep Pockets Logic
        var deepPockets = enchantments.get(ModEnchantments.DEEP_POCKETS);
        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(deepPockets.get(), stack);
            if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
            if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
        }
        return capacity;
    }

    @Override
    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
        Fraction capacity = getBaseCapacity(stack.getItem());

        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().unwrapKey().isPresent()) {
                String id = entry.getKey().unwrapKey().get().identifier().toString();

                // Deep Pockets
                if (id.contains("deep_pockets")) {
                    int level = entry.getIntValue();
                    if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
                    if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
                }

                // Drawer (Hinzugefügt!)
                if (id.contains("drawer")) {
                    int level = entry.getIntValue();
                    if (level > 0) {
                        Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                        capacity = capacity.multiplyBy(drawerBonus);
                    }
                }
            }
        }
        return capacity;
    }

    /**
     * Der Pfeil, den ein Bogen oder (seit 2026-09-28) eine Armbrust aus einem Koecher bekommt, sonst
     * EMPTY. Reihenfolge: Nebenhand, Brust-Slot, accessory slots (Curios/Trinkets), Schnellleiste,
     * restliches Inventar (nur Koecher mit Konstrukteurs Hand), zuletzt ein Koecher <em>im</em>
     * getragenen Rucksack - nur, wenn der Rucksack Meisterbauer traegt (Besitzer 2026-09-28).
     */
    public static ItemStack findProjectileForBow(Player player) {
        // 1. Offhand
        ItemStack arrow = findArrowInQuiver(player.getOffhandItem());
        if (!arrow.isEmpty()) return arrow;

        // 2. Brustslot. Erreichbar, weil die vier Koecher in ModItems eine EQUIPPABLE-Komponente
        // fuer EquipmentSlot.CHEST tragen; ohne sie nimmt Vanillas Ruestungsslot keinen Koecher an
        // und diese Stufe waere toter Code.
        arrow = findArrowInQuiver(player.getItemBySlot(EquipmentSlot.CHEST));
        if (!arrow.isEmpty()) return arrow;

        // 2b. Accessory slots (Curios/Trinkets): a quiver worn on the back or belt counts as worn
        arrow = findArrowInQuiver(accessoryQuiverWithArrows(player));
        if (!arrow.isEmpty()) return arrow;

        // 3. Hotbar (ohne Constructors Touch)
        for (int i = 0; i < 9; i++) {
            arrow = findArrowInQuiver(player.getInventory().getItem(i));
            if (!arrow.isEmpty()) return arrow;
        }

        // 4. Restliches Inventar (nur mit Constructors Touch)
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack quiver = player.getInventory().getItem(i);
            if (isRemoteQuiver(quiver, player)) {
                arrow = findFirstArrow(quiver);
                if (!arrow.isEmpty()) return arrow;
            }
        }

        // 5. Koecher im getragenen Rucksack (nur mit Meisterbauer am Rucksack)
        ItemStack backpack = BackpackItem.wornBackpackWith(player, ModEnchantments.MASTER_BUILDER);
        int index = quiverInBackpack(backpack);
        return index < 0 ? ItemStack.EMPTY : findArrowInQuiver(BackpackItem.entryStack(backpack, index));
    }

    /** Eintrag des ersten Koechers mit Pfeilen im Rucksack, sonst -1 (auch ohne Rucksack). */
    private static int quiverInBackpack(ItemStack backpack) {
        return backpack.isEmpty() ? -1 : BackpackItem.findEntry(backpack, s -> !findArrowInQuiver(s).isEmpty());
    }

    /**
     * Die Armbrust (Besitzer 2026-09-28): wie der Bogen, nur dass ein Geschoss in der Hand (eine
     * Feuerwerksrakete oder ein Pfeil in der Nebenhand) vorgeht - Vanillas eigene Reihenfolge fuer
     * gehaltene Munition, sonst liesse sich keine Rakete mehr laden, solange der Koecher Pfeile hat.
     */
    public static ItemStack findProjectileForCrossbow(Player player, ItemStack crossbow) {
        if (crossbow.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem weapon
                && !net.minecraft.world.item.ProjectileWeaponItem.getHeldProjectile(player, weapon.getSupportedHeldProjectiles()).isEmpty()) {
            return ItemStack.EMPTY;
        }
        return findProjectileForBow(player);
    }

    public static void consumeProjectileForBow(Player player) {
        // Gleiche Reihenfolge wie beim Finden

        // 1. Offhand
        if (tryConsumeArrow(player.getOffhandItem())) return;

        // 2. Brustslot. getItemBySlot gibt den lebenden Stapel zurueck, das gekuerzte
        // BUNDLE_CONTENTS steht also sofort im Slot; LivingEntity#detectEquipmentUpdates vergleicht
        // ueber ItemStack.matches, sieht die geaenderte Komponente und schickt sie zum Client - ein
        // setItemSlot waere hier ueberfluessig.
        if (tryConsumeArrow(player.getItemBySlot(EquipmentSlot.CHEST))) return;

        // 2b. Accessory slots: the live stack, same as the chest slot - the accessory mod syncs the
        // changed component on its own per-tick comparison
        if (tryConsumeArrow(accessoryQuiverWithArrows(player))) return;

        // 3. Hotbar (ohne Constructors Touch)
        for (int i = 0; i < 9; i++) {
            if (tryConsumeArrow(player.getInventory().getItem(i))) return;
        }

        // 4. Restliches Inventar (nur mit Constructors Touch)
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack quiver = player.getInventory().getItem(i);
            if (isRemoteQuiver(quiver, player) && tryConsumeArrow(quiver)) {
                return;
            }
        }

        // 5. Koecher im getragenen Rucksack mit Meisterbauer: der Eintrag ist eine Kopie, also zurueckschreiben
        ItemStack backpack = BackpackItem.wornBackpackWith(player, ModEnchantments.MASTER_BUILDER);
        int index = quiverInBackpack(backpack);
        if (index >= 0) {
            ItemStack quiver = BackpackItem.entryStack(backpack, index);
            if (tryConsumeArrow(quiver)) {
                BackpackItem.replaceEntry(backpack, index, quiver);
            }
        }
    }

    /**
     * Pfeil-Aufnahme (Besitzer 2026-09-28): ein aufgehobener Pfeil - liegender Pfeil-Gegenstand oder
     * steckengebliebenes, aufsammelbares Geschoss - geht nur in einen Koecher mit Trichter (Stufe I nur
     * Sorten, die schon drin liegen; Stufe II jede Pfeilsorte). Haende zuerst, dann das Inventar
     * einschliesslich Brust-Slot, then quivers in accessory slots (Curios/Trinkets). Verkleinert
     * {@code arrows} um das Eingelegte.
     *
     * @return ob etwas eingelegt wurde
     */
    public static boolean tryFunnelArrows(Player player, ItemStack arrows) {
        if (arrows.isEmpty() || !arrows.is(ItemTags.ARROWS)) {
            return false;
        }
        boolean any = false;
        for (InteractionHand hand : InteractionHand.values()) {
            any |= funnelInto(player.getItemInHand(hand), arrows, player);
        }
        for (int i = 0; i < player.getInventory().getContainerSize() && !arrows.isEmpty(); i++) {
            any |= funnelInto(player.getInventory().getItem(i), arrows, player);
        }
        // Quivers in accessory slots (Curios/Trinkets) last, so the order without an accessory mod
        // stays exactly as before
        for (ItemStack quiver : AccessorySlots.findAll(player, s -> s.getItem() instanceof QuiverItem)) {
            any |= funnelInto(quiver, arrows, player);
        }
        return any;
    }

    private static boolean funnelInto(ItemStack stack, ItemStack arrows, Player player) {
        return !arrows.isEmpty() && stack.getItem() instanceof QuiverItem quiver
                && quiver.canAutoPickup(stack, arrows, player.level())
                && quiver.tryInsertStackFromWorld(stack, arrows, player);
    }

    /** The first quiver with arrows in an accessory slot (Curios/Trinkets), else EMPTY. */
    private static ItemStack accessoryQuiverWithArrows(Player player) {
        return AccessorySlots.findFirst(player, s -> !findArrowInQuiver(s).isEmpty());
    }

    private static ItemStack findArrowInQuiver(ItemStack stack) {
        if (!(stack.getItem() instanceof QuiverItem)) return ItemStack.EMPTY;
        return findFirstArrow(stack);
    }

    private static boolean isRemoteQuiver(ItemStack stack, Player player) {
        return stack.getItem() instanceof QuiverItem
                && hasEnchantment(stack, player.level(), ModEnchantments.CONSTRUCTORS_TOUCH);
    }

    private static ItemStack findFirstArrow(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack s : contents.itemsCopy()) {
            if (s.is(ItemTags.ARROWS)) return s.copy();
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryConsumeArrow(ItemStack bundle) {
        if (!(bundle.getItem() instanceof QuiverItem)) return false;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        List<ItemStack> newItems = new ArrayList<>();
        boolean found = false;

        for (ItemStack s : contents.itemsCopy()) {
            if (!found && s.is(ItemTags.ARROWS)) {
                ItemStack copy = s.copy();
                copy.shrink(1);
                if (!copy.isEmpty()) newItems.add(copy);
                found = true;
            } else {
                newItems.add(s);
            }
        }

        if (found) {
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(newItems)));
            return true;
        }
        return false;
    }

}