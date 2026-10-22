package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.CustomDiscItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Pocket Jukebox (Bedrock preview 26.70.21, plan docs/ai/PLAN-B-POCKETJUKE-2026-10-21.md): a bundle for up to
 * {@link #CAPACITY} discs (Vanilla discs and burned custom discs, one each). Discs go in and out like a bundle (left
 * click puts in, right click with an empty hand / on an empty slot takes the newest out, bundle sounds); use in the
 * main hand plays and pauses ({@link PocketPlayback}), the off hand only holds it. 17 colours like Vanilla's bundles,
 * dyed by a {@code crafting_transmute} recipe that keeps the discs and the position.
 *
 * <p>The contents live in Vanilla's {@code bundle_contents}, so the bundle item model (open while a disc is selected)
 * and the bundle tooltip grid work as they are. It is not in {@code #minecraft:bundles} on purpose: Vanilla's bundle
 * dye recipes take that tag and would turn the jukebox into a plain bundle (the price: no mouse wheel selection).
 *
 * <p>Shift + right click on a block puts it down as its {@link PocketJukeboxBlock} with discs and position (plan
 * docs/ai/PLAN-B-PJUKE21-2026-10-22.md); a jukebox playing in the hand goes on playing there.
 */
public class PocketJukeboxItem extends BlockItem {
    public static final int CAPACITY = 20;
    /** Vanilla bundle bar colours. */
    private static final int BAR_COLOR = 0xFF7087FF;
    private static final int FULL_BAR_COLOR = 0xFFFF5555;

    /** null = undyed. */
    public final DyeColor color;

    public PocketJukeboxItem(Block block, Properties properties, DyeColor color) {
        super(block, properties.stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
        this.color = color;
    }

    /** Only Shift + right click places it; otherwise PASS, so {@link #use} plays or pauses. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive() || !SimpleMusic.on()) return InteractionResult.PASS;
        boolean wasPlaying = player instanceof ServerPlayer server && PocketPlayback.pauseIfActive(server, context.getItemInHand());
        BlockPlaceContext place = new BlockPlaceContext(context);
        InteractionResult result = super.useOn(context);
        if (wasPlaying && result.consumesAction() && context.getLevel().getBlockEntity(place.getClickedPos()) instanceof PocketJukeboxBlockEntity be) {
            be.start();
        }
        return result;
    }

    /** A disc the jukebox takes: anything with a Vanilla jukebox song, or a burned custom disc. */
    public static boolean isDisc(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof PocketJukeboxItem)
                && (stack.has(DataComponents.JUKEBOX_PLAYABLE) || stack.getItem() instanceof CustomDiscItem && CustomDiscItem.track(stack) != null);
    }

    public static BundleContents contents(ItemStack jukebox) {
        return jukebox.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }

    /** The disc at {@code fromEnd} (0 = the one put in first), or EMPTY. */
    public static ItemStack disc(BundleContents contents, int fromEnd) {
        int index = contents.size() - 1 - fromEnd;
        return index < 0 || index >= contents.size() ? ItemStack.EMPTY : contents.items().get(index).create();
    }

    /** Puts one of {@code disc} in (newest first, like the bundle) and shrinks it; false when full or no disc. */
    public static boolean insert(ItemStack jukebox, ItemStack disc) {
        if (!isDisc(disc)) return false;
        BundleContents contents = contents(jukebox);
        if (contents.size() >= CAPACITY) return false;
        List<ItemStackTemplate> items = new ArrayList<>(contents.size() + 1);
        items.add(ItemStackTemplate.fromNonEmptyStack(disc, 1));
        items.addAll(contents.items());
        jukebox.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
        disc.shrink(1);
        return true;
    }

    /** Takes the newest disc out, or EMPTY. */
    public static ItemStack removeOne(ItemStack jukebox) {
        BundleContents contents = contents(jukebox);
        if (contents.isEmpty()) return ItemStack.EMPTY;
        List<ItemStackTemplate> items = new ArrayList<>(contents.items());
        ItemStack out = items.remove(0).create();
        jukebox.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
        return out;
    }

    /** Marks the disc at {@code index} as selected (-1 = none): the open model and the tooltip highlight. */
    static void select(ItemStack jukebox, int index) {
        BundleContents contents = contents(jukebox);
        if (contents.getSelectedItemIndex() == index) return;
        BundleContents.Mutable mutable = contents.asMutable();
        mutable.toggleSelectedItem(-1);
        if (index >= 0) mutable.toggleSelectedItem(index);
        jukebox.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack jukebox, Slot slot, ClickAction action, Player player) {
        ItemStack other = slot.getItem();
        if (action == ClickAction.PRIMARY && !other.isEmpty()) {
            if (!isDisc(other)) return false;
            if (contents(jukebox).size() >= CAPACITY || !slot.allowModification(player)) {
                sound(player, SoundEvents.BUNDLE_INSERT_FAIL);
                return true;
            }
            ItemStack taken = slot.safeTake(other.getCount(), 1, player);
            if (insert(jukebox, taken)) sound(player, SoundEvents.BUNDLE_INSERT);
            if (!taken.isEmpty()) slot.safeInsert(taken);
            changed(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack out = removeOne(jukebox);
            if (!out.isEmpty()) {
                ItemStack rest = slot.safeInsert(out);
                if (!rest.isEmpty()) insert(jukebox, rest);
                else sound(player, SoundEvents.BUNDLE_REMOVE_ONE);
                changed(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack jukebox, ItemStack cursor, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (action == ClickAction.PRIMARY && !cursor.isEmpty()) {
            if (!isDisc(cursor)) return false;
            if (!slot.allowModification(player)) return false;
            sound(player, insert(jukebox, cursor) ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_INSERT_FAIL);
            changed(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && cursor.isEmpty()) {
            if (!slot.allowModification(player)) return false;
            ItemStack out = removeOne(jukebox);
            if (!out.isEmpty()) {
                carried.set(out);
                sound(player, SoundEvents.BUNDLE_REMOVE_ONE);
                changed(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SimpleMusic.on()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (contents(stack).isEmpty()) {
            if (!level.isClientSide()) sound(player, SoundEvents.BUNDLE_INSERT_FAIL);
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer server) PocketPlayback.toggle(server, stack);
        return InteractionResult.SUCCESS;
    }

    /** A stack that played when it left the inventory (or before a restart) shows closed again. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (contents(stack).getSelectedItemIndex() >= 0 && !(entity instanceof ServerPlayer player && PocketPlayback.active(player, stack))) {
            select(stack, -1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(1 + 12 * contents(stack).size() / CAPACITY, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return contents(stack).size() >= CAPACITY ? FULL_BAR_COLOR : BAR_COLOR;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new PocketContents(contents(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.simplemusic.pocket_jukebox.use").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.simplemusic.pocket_jukebox.place").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, lines, flag);
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        BundleContents contents = entity.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return;
        entity.getItem().set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        ItemUtils.onContainerDestroyed(entity, contents.itemCopies());
    }

    static void sound(Entity entity, SoundEvent sound) {
        entity.playSound(sound, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void changed(Player player) {
        if (player.containerMenu != null) player.containerMenu.slotsChanged(player.getInventory());
    }
}
