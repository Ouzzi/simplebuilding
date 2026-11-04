package com.simplelib.api.aging;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A block that ages in stages like Vanilla copper weathers (DW04B-1, docs/ai/PLAN-B-PALE01-2026-11-04.md): stage 0 is the
 * base block (Vanilla or your own), each random tick an unwaxed stage may turn into the next one, the last stage is the end.
 * A honeycomb waxes a stage (the waxed copy never ages), an axe takes the wax off or scrapes one stage back - with Vanilla's
 * particles and sounds; a dispenser waxes too. The logic is {@link Aging}; this class only describes and creates the blocks.
 *
 * <pre>{@code
 * // 1. describe: base block + the names of the aged stages (the last one is the end stage)
 * AgingChain PUMPKIN = AgingChain.builder(MyMod.id("pumpkin"), Blocks.PUMPKIN)
 *         .stages("faded_pumpkin", "bleached_pumpkin", "pale_pumpkin")
 *         .when(AgingCondition.biome(MY_BIOMES)).chance(() -> MyConfig.pumpkinChance).build();
 * // 2. register in the loader's block and item registration
 * PUMPKIN.registerBlocks(); PUMPKIN.registerItems();
 * // 3. data: modules/simplelib/tools/aging_data.py writes blockstates, models, items, loot, wax recipes and tags
 * }</pre>
 *
 * Every created block copies the base block's properties ({@code ofFullCopy}) and, by default, its class (the
 * {@code (Properties)} constructor): it behaves exactly like the base block. Different behaviour: give a subclass with
 * {@link Builder#factory}. Full freedom: implement Vanilla's {@code ChangeOverTimeBlock} yourself instead.
 */
public final class AgingChain {
    /** Vanilla copper's chance per random tick ({@code ChangeOverTimeBlock}: about once per in-game day per block). */
    public static final double VANILLA_CHANCE = 0.05688889F;

    /** Builds one stage (the copied properties already carry the id). */
    @FunctionalInterface
    public interface StageProperties {
        BlockBehaviour.Properties apply(int stage, BlockBehaviour.Properties properties);
    }

    private final Identifier id;
    private final Identifier group;
    private final List<Block> stages;
    private final List<Block> waxed;
    private final Map<Block, Identifier> created;
    private final DoubleSupplier chance;
    private final AgingCondition condition;
    private final UnaryOperator<Item.Properties> itemProperties;
    private final String hint;
    private final List<Item> items = new ArrayList<>();
    private boolean registered;

    private AgingChain(Builder b, List<Block> stages, List<Block> waxed, Map<Block, Identifier> created) {
        this.id = b.id;
        this.group = b.group == null ? b.id : b.group;
        this.stages = List.copyOf(stages);
        this.waxed = List.copyOf(waxed);
        this.created = Collections.unmodifiableMap(created);
        this.chance = b.chance;
        this.condition = b.condition;
        this.itemProperties = b.item;
        this.hint = b.hint;
    }

    public static Builder builder(Identifier id, Block base) {
        return new Builder(id, base);
    }

    public Identifier id() {
        return id;
    }

    /** Chains of one group age as one front (a younger neighbour of the group holds an older block back, like copper). */
    public Identifier group() {
        return group;
    }

    public Block base() {
        return stages.get(0);
    }

    /** All stages, the base block first, the end stage last. */
    public List<Block> stages() {
        return stages;
    }

    /** The waxed copy of stage i (index = stage); empty when the chain has no wax. */
    public List<Block> waxed() {
        return waxed;
    }

    public int endStage() {
        return stages.size() - 1;
    }

    /** The blocks this chain created (not the base), in tab order: stages, then the waxed copies. */
    public List<Block> createdBlocks() {
        return List.copyOf(created.keySet());
    }

    /** The block items of {@link #createdBlocks}, after {@link #registerItems}. */
    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    public double chance() {
        return Math.max(0, Math.min(1, chance.getAsDouble()));
    }

    public AgingCondition condition() {
        return condition;
    }

    /** Registers the created blocks and links the chain into {@link Aging} (call in the loader's block registration). */
    public void registerBlocks() {
        created.forEach((block, key) -> Registry.register(BuiltInRegistries.BLOCK, key, block));
        if (!registered) {
            registered = true;
            Aging.register(this);
        }
    }

    /** One block item per created block (properties from the builder's {@link Builder#item}). */
    public void registerItems() {
        if (!items.isEmpty()) return;
        created.forEach((block, key) -> {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, key);
            Item.Properties properties = itemProperties.apply(new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            boolean waxedItem = waxed.contains(block);
            boolean ages = !waxedItem && stages.indexOf(block) < endStage();
            String line = waxedItem ? WAXED_TOOLTIP : ages ? hint : null;
            items.add(Registry.register(BuiltInRegistries.ITEM, itemKey, new AgingItem(block, properties, line)));
        });
    }

    /** Tooltip of every waxed item (the wax is not visible): "Waxed: keeps its look". */
    public static final String WAXED_TOOLTIP = "tooltip.simplelib.aging.waxed";

    /** Block item with one grey line: waxed, or the chain's hint on the stages that still age. */
    static final class AgingItem extends BlockItem {
        private final String line;

        AgingItem(Block block, Item.Properties properties, String line) {
            super(block, properties);
            this.line = line;
        }

        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    java.util.function.Consumer<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            if (line != null) lines.accept(net.minecraft.network.chat.Component.translatable(line).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    public static final class Builder {
        private final Identifier id;
        private final Block base;
        private final List<String> names = new ArrayList<>();
        private Identifier group;
        private DoubleSupplier chance = () -> VANILLA_CHANCE;
        private AgingCondition condition = AgingCondition.ALWAYS;
        private Function<BlockBehaviour.Properties, Block> factory;
        private StageProperties properties = (stage, p) -> p;
        private UnaryOperator<Item.Properties> item = p -> p;
        private boolean wax = true;
        private String waxedPrefix = "waxed_";
        private String hint;

        private Builder(Identifier id, Block base) {
            this.id = id;
            this.base = base;
        }

        /** Paths of the aged stages in the chain's namespace, oldest last (= end stage). At least one. */
        public Builder stages(String... paths) {
            names.addAll(List.of(paths));
            return this;
        }

        /** Chance per random tick that an unwaxed stage tries to age (0 = never, 1 = every tick); read on every tick. */
        public Builder chance(DoubleSupplier chance) {
            this.chance = chance;
            return this;
        }

        public Builder when(AgingCondition condition) {
            this.condition = condition;
            return this;
        }

        public Builder group(Identifier group) {
            this.group = group;
            return this;
        }

        /** The block class of the stages (default: the base block's class through its {@code (Properties)} constructor). */
        public Builder factory(Function<BlockBehaviour.Properties, Block> factory) {
            this.factory = factory;
            return this;
        }

        /** Change the copied properties per stage (1..end; waxed copies get their stage's), e.g. the map color. */
        public Builder properties(StageProperties properties) {
            this.properties = properties;
            return this;
        }

        /** Item properties of every block item (e.g. compostable, equippable like the base item). */
        public Builder item(UnaryOperator<Item.Properties> item) {
            this.item = item;
            return this;
        }

        /** Without waxed copies (honeycomb does nothing). */
        public Builder noWax() {
            this.wax = false;
            return this;
        }

        /** Lang key of a tooltip line on the stages that still age (e.g. "Fades in the Pale Garden"); none by default. */
        public Builder hint(String langKey) {
            this.hint = langKey;
            return this;
        }

        public Builder waxedPrefix(String prefix) {
            this.waxedPrefix = prefix;
            return this;
        }

        public AgingChain build() {
            if (names.isEmpty()) throw new IllegalStateException(id + ": an aging chain needs at least one aged stage");
            Function<BlockBehaviour.Properties, Block> make = factory != null ? factory : sameClass(base);
            Map<Block, Identifier> created = new LinkedHashMap<>();
            List<Block> stages = new ArrayList<>(List.of(base));
            for (int i = 0; i < names.size(); i++) {
                Identifier key = Identifier.fromNamespaceAndPath(id.getNamespace(), names.get(i));
                boolean end = i == names.size() - 1;
                BlockBehaviour.Properties p = properties.apply(i + 1, copy(key));
                Block block = make.apply(end ? p : p.randomTicks());
                stages.add(block);
                created.put(block, key);
            }
            List<Block> waxed = new ArrayList<>();
            if (wax) {
                for (int i = 0; i < names.size(); i++) {
                    String stagePath = i == 0 ? baseName() : names.get(i - 1);
                    Identifier key = Identifier.fromNamespaceAndPath(id.getNamespace(), waxedPrefix + stagePath);
                    Block block = make.apply(properties.apply(i, copy(key)));
                    waxed.add(block);
                    created.put(block, key);
                }
            }
            return new AgingChain(this, stages, waxed, created);
        }

        private String baseName() {
            return BuiltInRegistries.BLOCK.getKey(base).getPath();
        }

        private BlockBehaviour.Properties copy(Identifier key) {
            return BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK, key));
        }

        private static Function<BlockBehaviour.Properties, Block> sameClass(Block base) {
            try {
                Constructor<? extends Block> c = base.getClass().getDeclaredConstructor(BlockBehaviour.Properties.class);
                c.setAccessible(true);
                return p -> {
                    try {
                        return c.newInstance(p);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Cannot copy " + base + " for an aging chain", e);
                    }
                };
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(base.getClass().getName() + " has no (Properties) constructor; give the aging chain a factory", e);
            }
        }
    }
}
