package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.mixin.AbstractHorseEntityAccessor;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;


public class SwiftRideHelper {

    private static final Identifier BOOST_ID = Identifier.of(Simplebuilding.MOD_ID, "swift_ride_boost");
    private static final Identifier JUMP_BOOST_ID = Identifier.of(Simplebuilding.MOD_ID, "horse_jump_boost");
    private static int debugTicker = 0;
    private static final boolean DEBUG_ENABLED = true;

    public static void applyBoost(LivingEntity entity) {
        boolean doLog = DEBUG_ENABLED && (debugTicker++ % 40 == 0);

        if (!entity.hasControllingPassenger()) return;

        ItemStack saddle = getSaddle(entity);
        int level = 0;

        if (!saddle.isEmpty()) {
             var registry = entity.getEntityWorld().getRegistryManager();
             var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
             var key = lookup.getOptional(ModEnchantments.SWIFT_RIDE);
             if (key.isPresent()) {
                 level = EnchantmentHelper.getLevel(key.get(), saddle);
             }
        }

        // VERSUCH 2 (Workaround): Ghast hat keinen Sattel-ItemStack, aber wird geritten
        // Wir simulieren Level 1 (oder max level?), damit er schnell ist.
        // Du kannst entscheiden: Soll er immer schnell sein oder gar nicht?
        // Ich setze ihn hier auf Level 1 als Standard.
        if (saddle.isEmpty() && isHappyGhast(entity)) {
             // Workaround: Wir nehmen an, der unsichtbare Sattel hat Level 1
             // (oder wir lesen einen Config-Wert "BaseGhastSpeed")
             level = 1;
             if (doLog) Simplebuilding.LOGGER.info("DEBUG: Ghast Workaround aktiv (Level 1 angenommen)");
        }



        if (level > 0) {
            float multiplier;
            String type;

            if (isHappyGhast(entity)) {
                multiplier = Simplebuilding.getConfig().enchantments.swiftRide.ghastSpeedMultiplier;
                type = "Happy Ghast";
            } else if (entity instanceof AbstractHorseEntity) {
                multiplier = Simplebuilding.getConfig().enchantments.swiftRide.horseSpeedMultiplier;
                type = "Pferd";
            } else {
                multiplier = Simplebuilding.getConfig().enchantments.swiftRide.otherSpeedMultiplier;
                type = "Anderes";
            }

            double boostValue = level * multiplier;

            applyToAttribute(entity, EntityAttributes.MOVEMENT_SPEED, boostValue);
            applyToAttribute(entity, EntityAttributes.FLYING_SPEED, boostValue);

            if (doLog) {
                Simplebuilding.LOGGER.info("--- SWIFT RIDE APPLIED ---");
                Simplebuilding.LOGGER.info("Entity: " + type + " | Level: " + level);
                Simplebuilding.LOGGER.info("Boost: +" + (boostValue * 100) + "%");
            }
        }

        if (entity instanceof AbstractHorseEntity horse) {
            applyJumpBoost(horse);
        }
    }

    private static void applyJumpBoost(AbstractHorseEntity horse) {
        // Rüstung holen (Standard-Methode für Pferde funktioniert hier meistens)
        ItemStack armor = horse.getBodyArmor();

        if (armor.isEmpty()) {
            removeJumpBoost(horse);
            return;
        }

        var registry = horse.getEntityWorld().getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var key = lookup.getOptional(ModEnchantments.HORSE_JUMP);

        if (key.isPresent()) {
            int level = EnchantmentHelper.getLevel(key.get(), armor);
            if (level > 0) {
                float multiplier = Simplebuilding.getConfig().enchantments.horseJump.jumpStrengthMultiplier;
                double boost = level * multiplier;

                applyToAttribute(horse, EntityAttributes.JUMP_STRENGTH, boost, JUMP_BOOST_ID);
            } else {
                removeJumpBoost(horse);
            }
        }
    }

    private static void removeJumpBoost(LivingEntity entity) {
        EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
        if (attribute != null && attribute.getModifier(JUMP_BOOST_ID) != null) {
            attribute.removeModifier(JUMP_BOOST_ID);
        }
    }


    private static void applyToAttribute(LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attributeKey, double boostValue, Identifier modifierId) {
        EntityAttributeInstance attribute = entity.getAttributeInstance(attributeKey);
        if (attribute != null) {
            if (attribute.getModifier(modifierId) != null) {
                // Performance: Nichts tun, wenn Wert gleich ist
                if (Math.abs(attribute.getModifier(modifierId).value() - boostValue) < 0.0001) return;
                attribute.removeModifier(modifierId);
            }
            attribute.addTemporaryModifier(new EntityAttributeModifier(
                    modifierId, boostValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    private static void applyToAttribute(LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attributeKey, double boostValue) {
        applyToAttribute(entity, attributeKey, boostValue, BOOST_ID); // BOOST_ID ist die SwiftRide ID
    }

    public static void removeBoost(LivingEntity entity) {
        removeAttribute(entity, EntityAttributes.MOVEMENT_SPEED);
        removeAttribute(entity, EntityAttributes.FLYING_SPEED);

        if (entity instanceof AbstractHorseEntity horse) {
            removeJumpBoost(horse);
        }
    }

    private static void removeAttribute(LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attributeKey) {
        EntityAttributeInstance attribute = entity.getAttributeInstance(attributeKey);
        if (attribute != null && attribute.getModifier(BOOST_ID) != null) {
            attribute.removeModifier(BOOST_ID);
        }
    }

    private static ItemStack getSaddle(LivingEntity entity) {
        try {
            // 1. Pferde
            if (entity instanceof AbstractHorseEntity horse && horse.hasSaddleEquipped()) {
                if (horse instanceof AbstractHorseEntityAccessor accessor) {
                    SimpleInventory inv = accessor.getItems();
                    ItemStack stack = inv.getStack(0);
                    if (stack.isOf(Items.SADDLE)) return stack;
                }
            }

            // 2. Equipment Slot
            ItemStack stack = entity.getEquippedStack(EquipmentSlot.SADDLE);
            if (stack.isOf(Items.SADDLE)) return stack;

            // 3. GHAST / REFLECTION SEARCH
            if (isHappyGhast(entity)) {
                Class<?> clazz = entity.getClass();

                // Gehe auch die Elternklassen durch (falls HappyGhast von einer Mod-Klasse erbt)
                while (clazz != null && clazz != Object.class) {
                    for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object value = field.get(entity);

                        if (value == null) continue;

                        // A. Direktes Inventar (SimpleInventory)
                        if (value instanceof net.minecraft.inventory.Inventory inv) {
                            if (inv.size() > 0) {
                                ItemStack s = inv.getStack(0); // Slot 0 probieren
                                if (s.isOf(Items.SADDLE)) return s;
                                // Alle Slots durchsuchen
                                for(int i=0; i<inv.size(); i++) {
                                    ItemStack si = inv.getStack(i);
                                    if (si.isOf(Items.SADDLE)) return si;
                                }
                            }
                        }

                        // B. Einzelner ItemStack
                        if (value instanceof ItemStack ghastStack) {
                            if (ghastStack.isOf(Items.SADDLE)) return ghastStack;
                        }

                        // C. DefaultedList<ItemStack> (So speichern Mobs oft Items intern)
                        if (value instanceof net.minecraft.util.collection.DefaultedList list) {
                            if (!list.isEmpty() && list.get(0) instanceof ItemStack) {
                                for (Object obj : list) {
                                    ItemStack s = (ItemStack) obj;
                                    if (s.isOf(Items.SADDLE)) return s;
                                }
                            }
                        }
                    }
                    clazz = clazz.getSuperclass(); // Eine Ebene höher suchen
                }
            }

            return ItemStack.EMPTY;

        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean isHappyGhast(LivingEntity entity) {
        return entity.getType().getUntranslatedName().equals("happy_ghast");
    }
}