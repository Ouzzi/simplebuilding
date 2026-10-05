package com.simplebuilding.compat;

import com.simplebuilding.crucible.CrucibleCompat;
import com.simplebuilding.version.McVersion;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** Vanilla cooking recipes grouped like crucible jobs, without a dependency on JEI or SimpleLib. */
public final class CrucibleRecipeCatalog {
    public record Entry(ItemStack input, ItemStack result, int heat, int baseTicks, boolean warming) {}
    private record Key(Item item, int heat) {}

    public static List<Entry> build(Collection<? extends AbstractCookingRecipe> recipes) {
        if (!McVersion.CRUCIBLE) return List.of();
        var fastest = new LinkedHashMap<Key, Entry>();
        Set<Item> cooks = new HashSet<>();
        for (AbstractCookingRecipe recipe : recipes) {
            recipe.input().items().forEach(holder -> {
                ItemStack input = new ItemStack(holder);
                ItemStack output = recipe.assemble(new SingleRecipeInput(input));
                if (output.isEmpty()) return;
                cooks.add(input.getItem());
                int heat = CrucibleCompat.requiredHeat(input, recipe.getType());
                Key key = new Key(input.getItem(), heat);
                Entry entry = new Entry(input, output, heat, Math.max(1, recipe.cookingTime()), false);
                Entry previous = fastest.get(key);
                if (previous == null || entry.baseTicks() < previous.baseTicks()) fastest.put(key, entry);
            });
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack input = new ItemStack(item);
            if (!cooks.contains(item) && CrucibleCompat.warmable(input)) {
                fastest.put(new Key(item, 1), new Entry(input, input.copy(), 1, CrucibleCompat.warmingTicks(), true));
            }
        }
        return fastest.values().stream().sorted(Comparator
                .comparing((Entry entry) -> BuiltInRegistries.ITEM.getKey(entry.input().getItem()).toString())
                .thenComparingInt(Entry::heat)).toList();
    }

    private CrucibleRecipeCatalog() {}
}
