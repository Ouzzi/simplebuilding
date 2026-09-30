package com.simplefun.registry;

import com.simplefun.Constants;
import com.simplefun.entity.BrickProjectileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final ResourceKey<EntityType<?>> BRICK_PROJECTILE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "brick_projectile"));

    public static final EntityType<BrickProjectileEntity> BRICK_PROJECTILE =
            EntityType.Builder.<BrickProjectileEntity>of(BrickProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(BRICK_PROJECTILE_KEY);
}
