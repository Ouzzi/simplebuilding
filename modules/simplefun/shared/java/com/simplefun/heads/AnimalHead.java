package com.simplefun.heads;

import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.SkullBlock;

public enum AnimalHead implements SkullBlock.Type {
  PIG(
      EntityTypes.PIG,
      SoundEvents.PIG_SOUNDS
          .get(net.minecraft.world.entity.animal.pig.PigSoundVariants.SoundSet.CLASSIC)
          .adultSounds()
          .ambientSound()
          .value(),
      "pig/pig_temperate"),
  COW(
      EntityTypes.COW,
      SoundEvents.COW_SOUNDS
          .get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.CLASSIC)
          .ambientSound()
          .value(),
      "cow/cow_temperate"),
  CHICKEN(
      EntityTypes.CHICKEN,
      SoundEvents.CHICKEN_SOUNDS
          .get(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.SoundSet.CLASSIC)
          .adultSounds()
          .ambientSound()
          .value(),
      "chicken/chicken_temperate"),
  SHEEP(EntityTypes.SHEEP, SoundEvents.SHEEP_AMBIENT, "sheep/sheep");
  public final EntityType<?> source;
  public final SoundEvent sound;
  public final String texture;

  AnimalHead(EntityType<?> e, SoundEvent s, String t) {
    source = e;
    sound = s;
    texture = t;
    SkullBlock.Type.TYPES.put(getSerializedName(), this);
  }

  public String getSerializedName() {
    return "simplefun:" + name().toLowerCase(java.util.Locale.ROOT);
  }

  public String path() {
    return name().toLowerCase(java.util.Locale.ROOT) + "_head";
  }

  public String wall() {
    return name().toLowerCase(java.util.Locale.ROOT) + "_wall_head";
  }
}
