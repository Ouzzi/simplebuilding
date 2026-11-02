package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * Mob Essence (Brood hollow K1): a rare extra drop of a mob a player killed. The species sits in the stack's custom data
 * ({@code species}), so essences of one species stack and the name reads "Essence: Zombie".
 */
public class MobEssenceItem extends Item {
    public static final String SPECIES = "species";

    public MobEssenceItem(Properties properties) { super(properties); }

    public static ItemStack of(EntityType<?> type, int count) {
        ItemStack st = new ItemStack(BossMobs.ESSENCE, count);
        CompoundTag tag = new CompoundTag();
        tag.putString(SPECIES, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return st;
    }

    /** The species of an essence stack, or null (no or an unknown species). */
    public static @Nullable EntityType<?> species(ItemStack st) {
        if (!st.is(BossMobs.ESSENCE)) return null;
        CustomData data = st.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        Identifier id = Identifier.tryParse(data.copyTag().getStringOr(SPECIES, ""));
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    public Component getName(ItemStack st) {
        EntityType<?> type = species(st);
        return type == null ? super.getName(st) : Component.translatable("item.simplemobs.mob_essence.of", type.getDescription());
    }
}
