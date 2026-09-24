package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.BackpackBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.screen.BackpackContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Der abgestellte Rucksack.
 *
 * <p>Bewusst <em>kein</em> {@link Container}: Trichter und Komparatoren sehen ihn deshalb nicht.
 * Sie kennen nur Vanilla-Stapelgrenzen, und ein Trichter, der einen 256er-Stapel aus Tiefe Taschen
 * herauszieht, waere genau der Weg, auf dem ein uebergrosser Stapel ins Spiel entkommt.
 *
 * <p>Der Inhalt reist als Komponente {@code simplebuilding:backpack_contents}: beim Abstellen
 * liest {@link #applyImplicitComponents} sie vom Item, beim Abbauen kopiert die Loot-Tabelle
 * ({@code copy_components} ohne Filter) alle Komponenten zurueck auf das gedroppte Item - Inhalt,
 * Name, Verzauberungen. Verzauberungen und Name bleiben dazwischen in Vanillas
 * {@code components()} liegen; von dort liest der Block auch den Tiefe-Taschen-Faktor.
 */
public class BackpackBlockEntity extends BlockEntity {
    private static final String CONTENTS_TAG = "Contents";

    private final PlacedContainer container;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BACKPACK_BE, pos, state);
        BackpackTier tier = state.getBlock() instanceof BackpackBlock block ? block.getTier() : BackpackTier.BASIC;
        this.container = new PlacedContainer(tier);
        this.container.setChangeListener(this::setChanged);
    }

    public BackpackContainer container() {
        return this.container;
    }

    public BackpackTier tier() {
        return this.container.tier();
    }

    public int stackMultiplier() {
        return BackpackItem.stackMultiplier(this.components().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
    }

    public boolean isEmpty() {
        return this.container.isEmpty();
    }

    /** Titel des Menues: der Amboss-Name des Rucksacks, sonst der Block-Name. */
    public Component getDisplayName() {
        Component custom = this.components().get(DataComponents.CUSTOM_NAME);
        return custom != null ? custom : getBlockState().getBlock().getName();
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.container.load(input.read(CONTENTS_TAG, BackpackContents.CODEC).orElse(BackpackContents.EMPTY));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        BackpackContents contents = this.container.toContents();
        if (!contents.isEmpty()) {
            output.store(CONTENTS_TAG, BackpackContents.CODEC, contents);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.container.load(components.getOrDefault(ModDataComponentTypes.BACKPACK_CONTENTS, BackpackContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        BackpackContents contents = this.container.toContents();
        if (!contents.isEmpty()) {
            components.set(ModDataComponentTypes.BACKPACK_CONTENTS, contents);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(CONTENTS_TAG);
    }

    /** Container des abgestellten Rucksacks: Faktor und Gueltigkeit kommen vom Block. */
    private final class PlacedContainer extends BackpackContainer {
        PlacedContainer(BackpackTier tier) {
            super(tier, 1);
        }

        @Override
        public int multiplier() {
            return BackpackBlockEntity.this.stackMultiplier();
        }

        @Override
        public boolean stillValid(Player player) {
            return BackpackBlockEntity.this.stillValid(player);
        }
    }
}
