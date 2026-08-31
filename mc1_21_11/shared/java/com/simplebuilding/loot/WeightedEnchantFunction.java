package com.simplebuilding.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.trade.WeightedPicker;
import java.util.List;
import net.minecraft.core.Holder;
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
        // Auswahl-Logik liegt in com.simplebuilding.trade.WeightedPicker, damit die
        // code-registrierten Trades der 1.21.11-Linie exakt dieselbe Verteilung nutzen.
        List<PoolEntry> picks = WeightedPicker.pickOneOrTwo(
                this.pool, PoolEntry::weight, PoolEntry::enchantment, this.secondChance, context.getRandom());
        if (picks.isEmpty()) {
            return itemStack;
        }

        if (itemStack.is(Items.BOOK)) {
            itemStack = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
        }

        EnchantmentHelper.updateEnchantments(itemStack, enchantments -> {
            for (PoolEntry pick : picks) {
                enchantments.set(pick.enchantment(), pick.level());
            }
        });
        return itemStack;
    }
}
