package com.simplemaps;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

/**
 * Wayfinder map: an endless map that explores while held in the main or off hand (like a Vanilla map) and opens
 * the map screen on use. It binds to the first dimension it is used in; {@link Kind} decides which dimensions it
 * accepts (dimension type tags, owner F8).
 */
public class WayfinderMapItem extends Item {
    public enum Kind { OVERWORLD, NETHER, END }

    public final Kind kind;

    public WayfinderMapItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public static boolean accepts(Kind kind, Holder<DimensionType> type) {
        boolean nether = type.is(MapsItems.NETHER_DIMENSIONS), end = type.is(MapsItems.END_DIMENSIONS);
        return switch (kind) {
            case OVERWORLD -> !nether && !end;
            case NETHER -> nether;
            case END -> end;
        };
    }

    public boolean accepts(Level level) {
        return accepts(kind, level.dimensionTypeRegistration());
    }

    /** The stack's map id; assigns a new one first if needed (server only). */
    public static int ensureId(ItemStack stack, MinecraftServer server) {
        MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
        if (ref == null) {
            ref = new MapsComponents.MapRef(WayfinderIds.allocate(server));
            stack.set(MapsComponents.MAP_ID, ref);
        }
        return ref.id();
    }

    /** The map's data in this level's dimension, binding it on first use; null if it belongs elsewhere or is refused. */
    public @Nullable WayfinderData dataFor(ItemStack stack, ServerLevel level) {
        if (!accepts(level)) return null;
        WayfinderData data = WayfinderData.get(level.getServer(), ensureId(stack, level.getServer()));
        data.bind(level.dimension());
        return data.dimension().filter(level.dimension()::equals).isPresent() ? data : null;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!MapsConfig.enabled) return;
        applyPending(stack, level);
        if (slot == null || slot.getType() != EquipmentSlot.Type.HAND || !(owner instanceof Player)) return;
        WayfinderData data = dataFor(stack, level);
        if (data == null) return;
        Reveal.step(level, owner, data, MapsConfig.revealRadius);
        StructureMarks.step(level, owner, data);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        if (level instanceof ServerLevel server) applyPending(stack, server);
    }

    /** Takes over the area recorded by the cartography table (owner F4), once. */
    public static void applyPending(ItemStack stack, ServerLevel level) {
        MapsComponents.Pending pending = stack.remove(MapsComponents.PENDING);
        if (pending == null) return;
        MinecraftServer server = level.getServer();
        WayfinderData data = WayfinderData.get(server, ensureId(stack, server));
        if (pending.kind() == MapsComponents.Pending.VANILLA) {
            MapItemSavedData vanilla = net.minecraft.world.item.MapItem.getSavedData(new MapId(pending.source()), server.overworld());
            if (vanilla == null) return;
            data.bind(vanilla.dimension);
            if (data.dimension().filter(vanilla.dimension::equals).isPresent()) data.mergeFrom(vanilla);
        } else if (pending.source() != stack.get(MapsComponents.MAP_ID).id()) {
            WayfinderData other = WayfinderData.get(server, pending.source());
            other.dimension().ifPresent(data::bind);
            if (other.dimension().isEmpty() || data.dimension().equals(other.dimension())) data.mergeFrom(other);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!MapsConfig.enabled) return InteractionResult.PASS;
        if (level.isClientSide()) SimpleMaps.openHand.accept(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("item.simplemaps.wayfinder_map.tooltip." + kind.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
        Waypoints waypoints = stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY);
        if (!waypoints.list().isEmpty()) {
            out.accept(Component.translatable("item.simplemaps.wayfinder_map.tooltip.waypoints", waypoints.list().size())
                    .withStyle(ChatFormatting.GRAY));
        }
        MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
        if (ref != null && flag.isAdvanced()) out.accept(Component.literal("#" + ref.id()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
