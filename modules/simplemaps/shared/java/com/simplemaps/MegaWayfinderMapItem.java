package com.simplemaps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Mega wayfinder map (owner B23-K1): one map for every dimension. It explores the dimension its holder stands in (when
 * unlocked, {@link MegaMaps}) and shows the dimension picked last with the buttons in the screen's title bar; the shown
 * one sits in {@link MapsComponents#MAP_ID}/{@link MapsComponents#WAYPOINTS} like on a plain map.
 */
public class MegaWayfinderMapItem extends WayfinderMapItem {
    public MegaWayfinderMapItem(Properties properties) {
        super(Kind.OVERWORLD, properties);
    }

    public static MegaMaps maps(ItemStack stack) {
        return stack.getOrDefault(MapsComponents.MEGA, MegaMaps.EMPTY);
    }

    /** The kind of the shown dimension (jamming tint, coordinate scale). */
    @Override
    public Kind kind(ItemStack stack) {
        MegaMaps maps = maps(stack);
        return maps.entry(maps.selected()).map(MegaMaps.Entry::kindValue).orElse(Kind.OVERWORLD);
    }

    /** The mega map accepts every dimension it has unlocked; it binds nothing itself (one wayfinder id per dimension). */
    @Override
    public @Nullable WayfinderData dataFor(ItemStack stack, ServerLevel level) {
        String dim = level.dimension().identifier().toString();
        MegaMaps maps = maps(stack);
        if (!maps.unlocked(dim)) return null;
        MinecraftServer server = level.getServer();
        MegaMaps.Entry entry = maps.entry(dim).orElseThrow();
        int id = entry.mapId();
        if (id < 0 || !WayfinderIds.exists(server, id)) {
            id = WayfinderIds.allocate(server);
            maps = maps.withMap(dim, id, kindOf(level.dimensionTypeRegistration()));
        }
        WayfinderData data = WayfinderData.get(server, id);
        data.bind(level.dimension());
        if (maps.selected().isEmpty()) maps = maps.withSelected(dim);
        if (!maps.equals(maps(stack))) {
            stack.set(MapsComponents.MEGA, maps);
            syncShown(stack, maps);
        }
        return data.dimension().filter(level.dimension()::equals).isPresent() ? data : null;
    }

    /** Mirrors the shown entry into MAP_ID (and drops a stale one), so every map path shows it. */
    private static void syncShown(ItemStack stack, MegaMaps maps) {
        MegaMaps.Entry shown = maps.entry(maps.selected()).orElse(null);
        if (shown != null && shown.visited()) stack.set(MapsComponents.MAP_ID, new MapsComponents.MapRef(shown.mapId()));
        else stack.remove(MapsComponents.MAP_ID);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!MapsConfig.enabled) return;
        if (slot == null || slot.getType() != EquipmentSlot.Type.HAND || !(owner instanceof Player)) return;
        WayfinderData data = dataFor(stack, level);
        if (data == null) return;
        Reveal.step(level, owner, data, level.dimensionType().hasCeiling() ? MapsConfig.revealRadius / 2 : MapsConfig.revealRadius);
        StructureMarks.step(level, owner, data);
    }

    /**
     * Owner B23-K1 "remembers the last one": shows this dimension (visited ones only). The waypoints of the dimension
     * shown so far go back into its entry, the new one's come to the stack. Returns whether anything changed.
     */
    public static boolean select(ItemStack stack, String dimension) {
        if (!(stack.getItem() instanceof MegaWayfinderMapItem)) return false;
        MegaMaps maps = maps(stack);
        MegaMaps.Entry target = maps.entry(dimension).orElse(null);
        if (target == null || !target.visited() || dimension.equals(maps.selected())) return false;
        maps = maps.storeWaypoints(stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY)).withSelected(dimension);
        target = maps.entry(dimension).orElseThrow();
        maps = maps.with(new MegaMaps.Entry(target.dimension(), target.mapId(), target.kind(), target.icon(), Waypoints.EMPTY));
        stack.set(MapsComponents.MEGA, maps);
        if (target.waypoints().list().isEmpty()) stack.remove(MapsComponents.WAYPOINTS);
        else stack.set(MapsComponents.WAYPOINTS, target.waypoints());
        syncShown(stack, maps);
        return true;
    }

    /** Owner B23-K2: adds a dimension (anvil); the copy, or EMPTY when it cannot be added. */
    public static ItemStack unlocked(ItemStack stack, String dimension, Identifier icon) {
        MegaMaps next = maps(stack).unlock(dimension, icon);
        if (next == null) return ItemStack.EMPTY;
        ItemStack out = stack.copyWithCount(1);
        out.set(MapsComponents.MEGA, next);
        return out;
    }

    /**
     * Owner B23-K2: the anvil result for a mega map (first slot) and an item (second slot), or EMPTY. The rules come from
     * {@link MapsConfig#megaUnlockItems}/{@link MapsConfig#megaUnlockDefault}; only dimensions of this server count.
     */
    public static ItemStack anvilResult(ItemStack mega, ItemStack item, ServerLevel level) {
        if (!MapsConfig.enabled || !(mega.getItem() instanceof MegaWayfinderMapItem) || item.isEmpty()) return ItemStack.EMPTY;
        MinecraftServer server = level.getServer();
        String target = maps(mega).unlockTarget(MapsConfig.megaUnlockItems, MapsConfig.megaUnlockDefault,
                level.dimension().identifier().toString(),
                dim -> {
                    Identifier id = Identifier.tryParse(dim);
                    return id != null && server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) != null;
                },
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),
                item.getItem() instanceof net.minecraft.world.item.BlockItem);
        return target == null ? ItemStack.EMPTY : unlocked(mega, target, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()));
    }

    /** Crafting takes over the three maps in the grid (ids and waypoints), so nothing explored is lost. */
    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level) || stack.has(MapsComponents.MEGA)) return;
        List<ItemStack> inputs = new ArrayList<>();
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container instanceof CraftingContainer) inputs.add(slot.getItem());
        }
        inherit(stack, inputs, level.getServer());
    }

    /** Takes over the bound plain/Nether/End maps among {@code inputs} into their dimension's entry. */
    public static void inherit(ItemStack mega, List<ItemStack> inputs, MinecraftServer server) {
        MegaMaps maps = maps(mega);
        for (ItemStack in : inputs) {
            MapsComponents.MapRef ref = in.get(MapsComponents.MAP_ID);
            if (!(in.getItem() instanceof WayfinderMapItem) || in.getItem() instanceof MegaWayfinderMapItem || ref == null
                    || !WayfinderIds.exists(server, ref.id())) continue;
            var bound = WayfinderData.get(server, ref.id()).dimension();
            if (bound.isEmpty()) continue;
            String dim = bound.get().identifier().toString();
            MegaMaps.Entry entry = maps.entry(dim).orElse(null);
            if (entry == null || entry.visited()) continue;
            ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, bound.get().identifier()));
            Kind kind = target == null ? ((WayfinderMapItem) in.getItem()).kind : kindOf(target.dimensionTypeRegistration());
            maps = maps.with(new MegaMaps.Entry(dim, ref.id(), kind.ordinal(), entry.icon(),
                    in.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY)));
        }
        if (maps.entries().isEmpty()) return;
        // Shows the Overworld first when it came along, else the first map taken over.
        String first = maps.all().stream().filter(MegaMaps.Entry::visited).map(MegaMaps.Entry::dimension).findFirst().orElse("");
        MegaMaps.Entry shown = maps.entry(first).orElseThrow();
        maps = maps.with(new MegaMaps.Entry(shown.dimension(), shown.mapId(), shown.kind(), shown.icon(), Waypoints.EMPTY)).withSelected(first);
        mega.set(MapsComponents.MEGA, maps);
        if (!shown.waypoints().list().isEmpty()) mega.set(MapsComponents.WAYPOINTS, shown.waypoints());
        syncShown(mega, maps);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        MegaMaps maps = maps(stack);
        long visited = maps.all().stream().filter(MegaMaps.Entry::visited).count();
        out.accept(Component.translatable("item.simplemaps.mega_wayfinder_map.tooltip", visited, maps.all().size())
                .withStyle(ChatFormatting.GRAY));
        MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
        if (ref != null && flag.isAdvanced()) out.accept(Component.literal("#" + ref.id()).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Whether the shown dimension is this level's (else the map in hand shows the jamming signal). */
    @Override
    public boolean accepts(ItemStack stack, Level level) {
        return maps(stack).selected().equals(level.dimension().identifier().toString());
    }
}
