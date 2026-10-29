package com.simpleblocks.saw;

import com.simpleblocks.SimpleBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Shapecutter (owner 17.10., principle A4b): block, item, menu type, the recipe type {@code simpleblocks:sawing}
 * and the saw's recipe list. The list is every {@code minecraft:stonecutting} recipe plus every {@code simpleblocks:sawing}
 * recipe (one entry per input/result/count), so the saw is a strict superset of the stonecutter and every module that
 * ships stonecutting or sawing recipes (Simple Octets, Simple End, ...) shows up without code here. The server builds the
 * list once per recipe manager and sends it to the client ({@link Payload}, on join and after /reload); the client menu
 * uses exactly that list, so the button indices match.
 */
public final class Sawing {
    public static final String NAME = "shapecutter";
    /** The id before the rename (owner 21.10.); {@link com.simpleblocks.SimpleBlocks#registerIdMoves} maps it to {@link #NAME}. */
    public static final String OLD_NAME = "shaping_saw";
    /** Recipe advancement of the crafting recipe, before and after the rename. */
    public static final String ADVANCEMENT = "recipes/decorations/" + NAME, OLD_ADVANCEMENT = "recipes/decorations/" + OLD_NAME;
    public static final Identifier TYPE_ID = SimpleBlocks.id("sawing");
    public static final RecipeType<SawingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return TYPE_ID.toString();
        }
    };
    public static final Block SAW = new ShapecutterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONECUTTER)
            .setId(ResourceKey.create(Registries.BLOCK, SimpleBlocks.id(NAME))));
    public static Item SAW_ITEM;
    public static MenuType<ShapecutterMenu> MENU;

    /** One cut the saw offers: the input, the result, and whether only the saw has it (no stonecutting recipe). */
    public record Entry(Identifier id, Ingredient input, ItemStackTemplate result, boolean sawOnly) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::id, Ingredient.CONTENTS_STREAM_CODEC, Entry::input,
                ItemStackTemplate.STREAM_CODEC, Entry::result, ByteBufCodecs.BOOL, Entry::sawOnly, Entry::new);

        public ItemStack resultStack() {
            return result.create();
        }
    }

    /** Server to client: the saw's recipe list. */
    public record Payload(List<Entry> entries) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(SimpleBlocks.id("saw_recipes"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), Payload::entries, Payload::new);

        public static Payload current(MinecraftServer server) {
            return new Payload(serverEntries(server.getRecipeManager()));
        }

        @Override
        public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static RecipeManager cachedManager;
    private static List<Entry> cached = List.of();
    private static volatile List<Entry> clientEntries = List.of();
    /** Called on the client thread after a new list arrived (the JEI plugin adds the recipes late). */
    public static final List<Runnable> CLIENT_LISTENERS = new CopyOnWriteArrayList<>();

    private Sawing() {}

    // ------------------------------------------------------------ registration (inside each loader's registry event)

    public static void registerBlock() {
        Registry.register(BuiltInRegistries.BLOCK, SimpleBlocks.id(NAME), SAW);
    }

    public static void registerItem() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleBlocks.id(NAME));
        SAW_ITEM = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(SAW, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    public static void registerMenu() {
        MENU = Registry.register(BuiltInRegistries.MENU, SimpleBlocks.id(NAME), new MenuType<>(ShapecutterMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static void registerRecipeType() {
        Registry.register(BuiltInRegistries.RECIPE_TYPE, TYPE_ID, TYPE);
    }

    public static void registerRecipeSerializer() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, TYPE_ID, SawingRecipe.SERIALIZER);
    }

    // ------------------------------------------------------------ the recipe list

    /** The list for this side: the server's own, on the client the one the server sent. */
    public static List<Entry> entries(Level level) {
        if (level instanceof ServerLevel server) return serverEntries(server.getServer().getRecipeManager());
        return clientEntries;
    }

    public static synchronized List<Entry> serverEntries(RecipeManager manager) {
        if (manager != cachedManager) {
            cached = build(manager.getRecipes());
            cachedManager = manager;
        }
        return cached;
    }

    /** Stonecutting + sawing, one entry per (input items, result item, count), stonecutting kept first; sorted by result id. */
    static List<Entry> build(Collection<RecipeHolder<?>> recipes) {
        Map<String, Entry> unique = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof SingleItemRecipe recipe)) continue;
            boolean saw = recipe.getType() == TYPE;
            if (!saw && recipe.getType() != RecipeType.STONECUTTING) continue;
            var first = recipe.input().items().findFirst();
            if (first.isEmpty()) continue;
            ItemStack out = recipe.assemble(new SingleRecipeInput(new ItemStack(first.get().value())));
            if (out.isEmpty()) continue;
            List<String> inputs = recipe.input().items().map(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse("?")).sorted().toList();
            String key = BuiltInRegistries.ITEM.getKey(out.getItem()) + "*" + out.getCount() + "<" + String.join(",", inputs);
            Entry entry = new Entry(holder.id().identifier(), recipe.input(), ItemStackTemplate.fromNonEmptyStack(out), saw);
            Entry old = unique.get(key);
            if (old == null || old.sawOnly() && !saw) unique.put(key, entry);
        }
        List<Entry> out = new ArrayList<>(unique.values());
        out.sort(Comparator.comparing((Entry e) -> BuiltInRegistries.ITEM.getKey(e.result().item().value()).toString())
                .thenComparingInt(e -> e.result().count()).thenComparing(e -> e.id().toString()));
        return List.copyOf(out);
    }

    public static List<Entry> forInput(List<Entry> all, ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        return all.stream().filter(e -> e.input().test(stack)).toList();
    }

    public static boolean accepts(Level level, ItemStack stack) {
        return !stack.isEmpty() && entries(level).stream().anyMatch(e -> e.input().test(stack));
    }

    /** Client side of the payload (client thread). */
    public static void receive(List<Entry> entries) {
        clientEntries = List.copyOf(entries);
        CLIENT_LISTENERS.forEach(Runnable::run);
    }

    /** Drops the cached recipe manager and the list received from a server (server stop / disconnect). */
    public static synchronized void forgetAll() {
        cachedManager = null;
        cached = List.of();
        clientEntries = List.of();
    }

    public static List<Entry> clientEntries() {
        return clientEntries;
    }
}
