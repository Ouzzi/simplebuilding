package com.simplebuilding.modules.simplemobmanners;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;

/**
 * Bucket of Frog (queue N62): a water bucket used on a frog catches it like an axolotl or tadpole. Variant (the
 * {@code frog_variant} component), custom name and age ride along; releasing the bucket restores them. Frogs are not
 * {@code Bucketable} in Vanilla, so the pickup (mixin) and the release ({@link FrogBucketItem}) mirror the Vanilla code.
 */
public final class FrogBucket {
    public static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleMobManners.MOD_ID, "frog_bucket"));
    private static Item item;

    private FrogBucket() {}

    /** The item, created on first use (registered by the loader entry during the item registry event). */
    public static synchronized Item item() {
        if (item == null) {
            item = new FrogBucketItem(new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).setId(KEY));
        }
        return item;
    }

    /** Fabric/Forge-style direct registration (NeoForge registers {@link #item()} through its RegisterEvent helper). */
    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, KEY, item());
    }

    /** Water bucket on a living frog: the frog goes into a Bucket of Frog (Vanilla {@code Bucketable.bucketMobPickup} for frogs). */
    public static Optional<InteractionResult> pickup(Frog frog, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!SimpleMobManners.active() || !held.is(Items.WATER_BUCKET) || !frog.isAlive()) return Optional.empty();
        frog.playSound(SoundEvents.BUCKET_FILL_TADPOLE, 1.0F, 1.0F);
        ItemStack bucket = fill(frog);
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, bucket, false));
        if (!frog.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, bucket);
        }
        if (frog instanceof Leashable leashable) leashable.dropLeash();
        frog.discard();
        return Optional.of(InteractionResult.SUCCESS);
    }

    /** A Bucket of Frog holding this frog: variant component, name, age and the shared mob flags (like the tadpole bucket). */
    public static ItemStack fill(Frog frog) {
        ItemStack bucket = new ItemStack(item());
        bucket.copyFrom(DataComponents.FROG_VARIANT, frog);
        net.minecraft.world.entity.Bucketable.saveDefaultDataToBucketTag(frog, bucket);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.putInt("Age", frog.getAge()));
        return bucket;
    }

    /** Release: a new frog with everything the bucket carries; null on the client. */
    public static Frog release(net.minecraft.server.level.ServerLevel level, ItemStack bucket, net.minecraft.core.BlockPos pos) {
        Frog frog = net.minecraft.world.entity.EntityType.FROG.create(level,
                net.minecraft.world.entity.EntityType.createDefaultStackConfig(level, bucket, null), pos,
                net.minecraft.world.entity.EntitySpawnReason.BUCKET, true, false);
        if (frog == null) return null;
        net.minecraft.nbt.CompoundTag tag = bucket.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
        net.minecraft.world.entity.Bucketable.loadDefaultDataFromBucketTag(frog, tag);
        tag.getInt("Age").ifPresent(frog::setAge);
        level.addFreshEntityWithPassengers((Entity) frog);
        frog.playAmbientSound();
        return frog;
    }
}
