package com.simplebuilding.compat.rei;

import com.simplebuilding.compat.MobDropCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

/**
 * One {@link MobDropCatalog} drop as an REI display: inputs are the victim's and the killer's spawn
 * eggs, the output cycles through the possible results. Client-side only, no serializer.
 */
final class MobDropDisplay extends BasicDisplay {

    static final CategoryIdentifier<MobDropDisplay> CATEGORY = CategoryIdentifier.of("simplebuilding", "mob_drop");

    private final MobDropCatalog.Drop drop;

    MobDropDisplay(MobDropCatalog.Drop drop) {
        super(List.of(EntryIngredients.of(drop.victimIcon()), EntryIngredients.of(drop.killerIcon())),
                List.of(EntryIngredients.ofItems(new ArrayList<ItemLike>(drop.results()))),
                Optional.of(Identifier.fromNamespaceAndPath("simplebuilding", "mob_drop/" + drop.id().replace(':', '/'))));
        this.drop = drop;
    }

    MobDropCatalog.Drop drop() {
        return drop;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
