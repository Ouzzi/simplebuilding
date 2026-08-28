package com.simplebuilding.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Wählt eine Verzauberung gewichtet aus einem festen Pool (Verzauberung + festes Level + Gewicht)
 * und wendet sie auf das Item an; Bücher erhalten stored_enchantments. Optional wird mit
 * {@code second_chance} eine zweite, andere Verzauberung aus demselben Pool gezogen.
 * Ersetzt die alte TradeOfferHelper-Logik (createRandomEnchantedBook/-Item) für die
 * datengetriebenen Villager-Trades in data/simplebuilding/villager_trade/.
 */
public class WeightedEnchantFunction extends LootItemConditionalFunction {
    public record PoolEntry(Holder<Enchantment> enchantment, int level, int weight) {
        public static final Codec<PoolEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Enchantment.CODEC.fieldOf("enchantment").forGetter(PoolEntry::enchantment),
                Codec.INT.optionalFieldOf("level", 1).forGetter(PoolEntry::level),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(PoolEntry::weight)
        ).apply(instance, PoolEntry::new));
    }

    public static final MapCodec<WeightedEnchantFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance)
                    .and(instance.group(
                            PoolEntry.CODEC.listOf().fieldOf("pool").forGetter(function -> function.pool),
                            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("second_chance", 0.0F).forGetter(function -> function.secondChance)
                    ))
                    .apply(instance, WeightedEnchantFunction::new)
    );

    private final List<PoolEntry> pool;
    private final float secondChance;

    private WeightedEnchantFunction(List<LootItemCondition> predicates, List<PoolEntry> pool, float secondChance) {
        super(predicates);
        this.pool = pool;
        this.secondChance = secondChance;
    }

    // MC 1.21.11: Loot-Funktionen werden ueber einen LootItemFunctionType registriert
    // (LootItemConditionalFunction#getType). Erst ab 26.2 haengt der MapCodec direkt
    // in der Registry und die Klasse liefert ihn ueber codec().
    @Override
    public LootItemFunctionType<WeightedEnchantFunction> getType() {
        return ModLootFunctions.WEIGHTED_ENCHANT;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        RandomSource random = context.getRandom();
        PoolEntry first = pickWeighted(random);
        if (first == null) {
            return itemStack;
        }

        if (itemStack.is(Items.BOOK)) {
            itemStack = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
        }

        PoolEntry second = null;
        if (this.pool.size() > 1 && random.nextFloat() < this.secondChance) {
            second = pickWeighted(random);
            int attempts = 0;
            while (second != null && second.enchantment().equals(first.enchantment()) && attempts < 10) {
                second = pickWeighted(random);
                attempts++;
            }
            if (second != null && second.enchantment().equals(first.enchantment())) {
                second = null;
            }
        }

        PoolEntry secondPick = second;
        EnchantmentHelper.updateEnchantments(itemStack, enchantments -> {
            enchantments.set(first.enchantment(), first.level());
            if (secondPick != null) {
                enchantments.set(secondPick.enchantment(), secondPick.level());
            }
        });
        return itemStack;
    }

    private PoolEntry pickWeighted(RandomSource random) {
        int totalWeight = 0;
        for (PoolEntry entry : this.pool) {
            totalWeight += entry.weight();
        }
        if (totalWeight <= 0) {
            return null;
        }
        int pick = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (PoolEntry entry : this.pool) {
            currentWeight += entry.weight();
            if (pick < currentWeight) {
                return entry;
            }
        }
        return this.pool.get(0);
    }
}
