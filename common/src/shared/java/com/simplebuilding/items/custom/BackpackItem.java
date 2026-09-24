package com.simplebuilding.items.custom;

import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.util.EnchantmentHelper;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

/**
 * Die vier Rucksaecke. Ein Rucksack ist zugleich Brust-Ausruestung und Block.
 *
 * <ul>
 *   <li><b>Anziehen:</b> Rechtsklick mit dem Rucksack in der Hand legt ihn in den Brust-Slot. Das
 *       macht Vanillas {@code Item#use} ueber die {@code EQUIPPABLE}-Komponente (swappable) - genau
 *       wie bei einem Brustpanzer, samt Tausch mit einem getragenen Brustpanzer oder einer Elytra.
 *       Der Inhalt reist in der Komponente {@code simplebuilding:backpack_contents} mit.</li>
 *   <li><b>Abstellen:</b> Schleichen + Rechtsklick auf einen Block stellt ihn als Block ab
 *       ({@link #useOn}). Ohne Schleichen gibt {@code useOn} PASS zurueck, damit Vanilla danach
 *       {@code use} aufruft und der Rucksack angezogen wird statt abgestellt.</li>
 *   <li><b>Oeffnen:</b> nur getragen, ueber die Rucksack-Taste (Standard B), oder als abgestellter
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
        BackpackContents contents = stack.get(ModDataComponentTypes.BACKPACK_CONTENTS);
        if (contents != null) {
            stack.remove(ModDataComponentTypes.BACKPACK_CONTENTS);
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

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        BackpackContents contents = stack.getOrDefault(ModDataComponentTypes.BACKPACK_CONTENTS, BackpackContents.EMPTY);
        builder.accept(Component.translatable("tooltip.simplebuilding.backpack.slots", contents.size(), this.tier.slotCount())
                .withStyle(ChatFormatting.GRAY));
        List<ItemStack> stacks = contents.stacks();
        for (int i = 0; i < Math.min(TOOLTIP_ENTRIES, stacks.size()); i++) {
            ItemStack content = stacks.get(i);
            builder.accept(Component.literal(content.getCount() + "x ").append(content.getHoverName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (stacks.size() > TOOLTIP_ENTRIES) {
            builder.accept(Component.translatable("tooltip.simplebuilding.backpack.more", stacks.size() - TOOLTIP_ENTRIES)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        // Component.keybind loest der Client auf - der gemeinsame Code fasst so keine Client-Klasse an.
        builder.accept(Component.translatable("tooltip.simplebuilding.backpack.open_hint",
                Component.keybind("key.simplebuilding.open_backpack")).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.simplebuilding.backpack.place_hint").withStyle(ChatFormatting.GRAY));
    }

    // =================================================================================
    // Helfer, die Menue, Block, Verzauberungen und Netzwerk teilen
    // =================================================================================

    /** Der getragene Rucksack im Brust-Slot, sonst {@link ItemStack#EMPTY}. */
    public static ItemStack wornBackpack(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof BackpackItem ? chest : ItemStack.EMPTY;
    }

    /** Darf {@code stack} in einen Rucksack-Slot? Keine Rucksaecke, keine Shulkerkisten. */
    public static boolean mayStore(ItemStack stack) {
        return !(stack.getItem() instanceof BackpackItem) && stack.getItem().canFitInsideContainerItems();
    }

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
        return stackMultiplier(EnchantmentHelper.getEnchantmentLevel(backpack, level, ModEnchantments.DEEP_POCKETS));
    }

    /** Stapelfaktor aus einer Verzauberungsliste (abgestellter Rucksack: Komponenten des Blocks). */
    public static int stackMultiplier(ItemEnchantments enchantments) {
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(ModEnchantments.DEEP_POCKETS)) {
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
    public static ItemStack wornBackpackWith(Player player, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        ItemStack worn = wornBackpack(player);
        return !worn.isEmpty() && hasEnchantment(worn, player.level(), key) ? worn : ItemStack.EMPTY;
    }

    // =================================================================================
    // Direkter Zugriff auf die Komponente (Meisterbauer, Trichter, Konstrukteurs Hand).
    // Ein offenes Menue auf diesem Rucksack laedt danach neu (WornBackpackContainer#syncFromSource).
    // =================================================================================

    public static BackpackContents contents(ItemStack backpack) {
        return backpack.getOrDefault(ModDataComponentTypes.BACKPACK_CONTENTS, BackpackContents.EMPTY);
    }

    private static void setContents(ItemStack backpack, BackpackContents contents) {
        if (contents.isEmpty()) {
            backpack.remove(ModDataComponentTypes.BACKPACK_CONTENTS);
        } else {
            backpack.set(ModDataComponentTypes.BACKPACK_CONTENTS, contents);
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
        int funnel = EnchantmentHelper.getEnchantmentLevel(worn, player.level(), ModEnchantments.FUNNEL);
        if (funnel <= 0) {
            return false;
        }
        BackpackItem item = (BackpackItem) worn.getItem();
        com.simplebuilding.screen.BackpackContainer view =
                new com.simplebuilding.screen.BackpackContainer(item.getTier(), stackMultiplier(worn, player.level()));
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

    /**
     * Konstrukteurs Hand am getragenen Rucksack: ist beim Platzieren der Blockstapel in der Hand
     * leer geworden, fuellt ein gleicher Stapel aus dem Rucksack sie wieder auf (hoechstens ein
     * normaler Stapel).
     *
     * @param placed Kopie des Handstapels von vor dem Platzieren
     */
    public static void refillHandFromWornBackpack(Player player, net.minecraft.world.InteractionHand hand, ItemStack placed) {
        if (placed.isEmpty() || player.getAbilities().instabuild || !player.getItemInHand(hand).isEmpty()) {
            return;
        }
        ItemStack worn = wornBackpackWith(player, ModEnchantments.CONSTRUCTORS_TOUCH);
        if (worn.isEmpty()) {
            return;
        }
        int index = findEntry(worn, s -> ItemStack.isSameItemSameComponents(s, placed));
        if (index < 0) {
            return;
        }
        ItemStack refill = take(worn, index, placed.getMaxStackSize());
        if (!refill.isEmpty()) {
            player.setItemInHand(hand, refill);
            player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        }
    }
}
