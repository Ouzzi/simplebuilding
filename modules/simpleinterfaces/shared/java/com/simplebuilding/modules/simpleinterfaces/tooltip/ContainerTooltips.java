package com.simplebuilding.modules.simpleinterfaces.tooltip;

import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import com.simplebuilding.modules.simpleinterfaces.SimpleInterfaces;
import com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

/**
 * Shulker box and ender chest tooltips (docs/ai/KONZEPT-CONTAINER-TOOLTIPS-2026-10-19.md, stages 1+2). Vanilla 26.3 only
 * lists five shulker lines and nothing for ender chests (no grid component besides the bundle). The decision is
 * client-free so GameTests can check it; the client mixins feed it the Shift state and the cached ender snapshot.
 */
public final class ContainerTooltips {
    public static final int COLUMNS = 9;
    /** At most six rows (bigger shulker tiers): the last cell then shows the hidden stacks. */
    public static final int MAX_CELLS = COLUMNS * 6;
    /** Vanilla container panel gray. */
    public static final int VANILLA_FACE = 0xC6C6C6;
    /** Ender chest teal, mixed into the panel gray. */
    public static final int ENDER_TINT = 0x1E7A72;
    public static final int ENDER_SIZE = 27;
    /** Client cache: the own ender chest from the last snapshot; null = unknown (no server part or nothing sent). */
    public static volatile @Nullable ItemContainerContents enderSnapshot;
    /** Server side, set by the loader entry: sends the payload, false when the client lacks the channel. */
    public static BiPredicate<ServerPlayer, EnderChestSnapshotPayload> sender = (p, payload) -> false;

    private ContainerTooltips() {}

    /** The tooltip image for {@code stack}, empty when nothing is to be shown (Vanilla tooltip unchanged). */
    public static Optional<ContainerPreview> preview(ItemStack stack, boolean shift, ContainersConfig local, boolean serverAllowsEnder,
            @Nullable ItemContainerContents ender) {
        if (stack.isEmpty() || local == null) return Optional.empty();
        boolean grid = !local.tooltipNeedsShift || shift;
        if (stack.is(Items.ENDER_CHEST)) {
            if (!local.tooltipEnderChest || !serverAllowsEnder || ender == null) return Optional.empty();
            List<ItemStack> slots = slots(ender, ENDER_SIZE);
            if (slots.stream().allMatch(ItemStack::isEmpty))
                return Optional.of(new ContainerPreview(List.of(), 0, 0, false, Component.translatable("simpleinterfaces.tooltip.ender_chest.empty"), false));
            return Optional.of(grid ? new ContainerPreview(slots, 0, mix(VANILLA_FACE, ENDER_TINT, 0.35), true, null, false)
                    : new ContainerPreview(List.of(), 0, 0, false, Component.translatable("simpleinterfaces.tooltip.ender_chest.shift"), false));
        }
        if (!local.tooltipShulker || !(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock box)
                || stack.has(DataComponents.CONTAINER_LOOT)) return Optional.empty();
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (!contents.nonEmptyItemCopyStream().findAny().isPresent()) return Optional.empty();
        if (!grid) return Optional.of(new ContainerPreview(List.of(), 0, 0, false, Component.translatable("simpleinterfaces.tooltip.shulker.shift"), false));
        List<ItemStack> slots = slots(contents, Math.max(ENDER_SIZE, contents.size()));
        applyOversized(stack, slots);
        int more = 0;
        if (slots.size() > MAX_CELLS) {
            more = (int) slots.subList(MAX_CELLS - 1, slots.size()).stream().filter(s -> !s.isEmpty()).count();
            slots = new ArrayList<>(slots.subList(0, MAX_CELLS));
            slots.set(MAX_CELLS - 1, ItemStack.EMPTY);
        }
        DyeColor dye = box.getColor() != null ? box.getColor() : stack.get(DataComponents.BASE_COLOR);
        int face = local.tooltipColorByDye && dye != null ? mix(VANILLA_FACE, dye.getTextureDiffuseColor() & 0xFFFFFF, 0.5) : VANILLA_FACE;
        return Optional.of(new ContainerPreview(slots, more, face, true, null, true));
    }

    /** Simple Storage's tier shulker boxes keep counts above 99 in this item component (read by id and codec, no module dependency). */
    private static final Identifier OVERSIZED_COUNTS = Identifier.parse("simplestorage:container_counts");

    /**
     * Puts the real counts of oversized slots back ({@code [{slot,count}]} of the optional component, see
     * {@code ContainerCounts}); nothing happens when the component does not exist or the stack lacks it.
     */
    static void applyOversized(ItemStack stack, List<ItemStack> slots) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(OVERSIZED_COUNTS);
        if (type == null || type.codec() == null || !stack.has(type)) return;
        JsonElement json = encode(type, stack);
        if (json == null || !json.isJsonArray()) return;
        for (JsonElement e : json.getAsJsonArray()) {
            if (!e.isJsonObject() || !e.getAsJsonObject().has("slot") || !e.getAsJsonObject().has("count")) continue;
            int slot = e.getAsJsonObject().get("slot").getAsInt();
            if (slot >= 0 && slot < slots.size() && !slots.get(slot).isEmpty()) slots.get(slot).setCount(e.getAsJsonObject().get("count").getAsInt());
        }
    }

    private static <T> @Nullable JsonElement encode(DataComponentType<T> type, ItemStack stack) {
        T value = stack.get(type);
        return value == null ? null : type.codec().encodeStart(JsonOps.INSTANCE, value).result().orElse(null);
    }

    /** {@code size} stacks in slot order (missing slots empty), rounded up to full rows. */
    static List<ItemStack> slots(ItemContainerContents contents, int size) {
        int cells = (size + COLUMNS - 1) / COLUMNS * COLUMNS;
        List<ItemStack> out = new ArrayList<>(cells);
        contents.itemCopies().limit(cells).forEach(out::add);
        while (out.size() < cells) out.add(ItemStack.EMPTY);
        return out;
    }

    /** The ender inventory as Vanilla container contents (copies). */
    public static ItemContainerContents snapshot(PlayerEnderChestContainer ender) {
        List<ItemStack> items = new ArrayList<>(ender.getContainerSize());
        for (int i = 0; i < ender.getContainerSize(); i++) items.add(ender.getItem(i).copy());
        return ItemContainerContents.fromItems(items);
    }

    /** Server: sends the player's own ender chest; false when switched off or the client lacks the channel. */
    public static boolean send(ServerPlayer player) {
        if (!SimpleInterfaces.active() || !SimpleInterfaces.config().enableEnderChestTooltip) return false;
        return sender.test(player, new EnderChestSnapshotPayload(snapshot(player.getEnderChestInventory())));
    }

    /** Client: keeps the snapshot for the tooltip. */
    public static void receive(EnderChestSnapshotPayload payload) {
        enderSnapshot = payload.contents();
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }
}
