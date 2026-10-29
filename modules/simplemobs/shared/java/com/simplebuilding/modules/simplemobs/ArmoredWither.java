package com.simplebuilding.modules.simplemobs;

import com.simplelib.api.SimpleRarity;
import com.simplelib.registry.LibComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The armored Wither (BS28b-2, owner 28.10., docs/ai/PLAN-A-HEADS29-2026-10-29.md): the two outer skulls of the Wither figure may
 * wear helmets up to Diamond (put on with Simple Mobheads, read here through its component {@code simplemobheads:helmet} by
 * name - without Mobheads there are none and nothing here happens). With a helmet on an outer skull the Vanilla Wither does
 * not come; the figure waits for the Netherite or Enderite crown of the middle skull ({@link BlightedPattern}), then the
 * Blighted or Abyssal Wither comes, a little stronger the more precious the side helmets are, and gives the helmets' materials
 * back when it dies.
 */
public final class ArmoredWither {
    private static final Identifier HELMET_COMPONENT = Identifier.fromNamespaceAndPath("simplemobheads", "helmet");
    /** Entity tag prefix of a side helmet on the Wither: {@code simplemobs.helmet.<n>=<item id>} (saved with the entity). */
    public static final String TAG = "simplemobs.helmet.";

    /** Per strength point: 3 % more health, 0.2 more armor, 5 more experience (all helmets of Diamond: 10 points, +30 %). */
    public static final double HEALTH_PER_POINT = 0.03, ARMOR_PER_POINT = 0.2;
    public static final int XP_PER_POINT = 5;
    /** A helmet is five pieces of its material; the Wither gives back 1 to 1.5 times what the side helmets are made of. */
    public static final int PIECES = 5;
    public static final double LOOT_MAX_FACTOR = 1.5;

    private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "armored_health");
    private static final Identifier ARMOR_ID = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "armored_armor");

    private ArmoredWither() {}

    /** Strength points of a side helmet: leather 1, copper/chainmail/gold 2, iron 3, diamond 5, anything else 0. */
    public static int points(Item helmet) {
        if (helmet == Items.LEATHER_HELMET) return 1;
        if (helmet == Items.COPPER_HELMET || helmet == Items.CHAINMAIL_HELMET || helmet == Items.GOLDEN_HELMET) return 2;
        if (helmet == Items.IRON_HELMET) return 3;
        if (helmet == Items.DIAMOND_HELMET) return 5;
        return 0;
    }

    /** What a helmet is made of: five of it. The Chainmail Helmet has no material (it gives the forbidden fire instead). */
    public static @Nullable Item material(Item helmet) {
        if (helmet == Items.LEATHER_HELMET) return Items.LEATHER;
        if (helmet == Items.COPPER_HELMET) return Items.COPPER_INGOT;
        if (helmet == Items.IRON_HELMET) return Items.IRON_INGOT;
        if (helmet == Items.GOLDEN_HELMET) return Items.GOLD_INGOT;
        if (helmet == Items.DIAMOND_HELMET) return Items.DIAMOND;
        return null;
    }

    private static @Nullable DataComponentType<?> component() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(HELMET_COMPONENT);
    }

    /** The helmet on the wither skull at {@code pos} (Simple Mobheads), or empty. */
    public static ItemStack helmetAt(Level level, BlockPos pos) {
        DataComponentType<?> type = component();
        if (type == null || !(level.getBlockEntity(pos) instanceof SkullBlockEntity skull)) return ItemStack.EMPTY;
        return skull.components().get(type) instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    /** Takes the helmet off the skull without a drop (the Wither that comes uses it up). */
    public static void consume(Level level, BlockPos pos) {
        DataComponentType<?> type = component();
        if (type == null || !(level.getBlockEntity(pos) instanceof SkullBlockEntity skull)) return;
        PatchedDataComponentMap map = new PatchedDataComponentMap(DataComponentMap.EMPTY);
        map.setAll(skull.collectComponents()); // the implicit ones too, applyComponents would reset them otherwise
        map.remove(type);
        skull.applyComponents(DataComponentMap.EMPTY, map.asPatch());
        skull.setChanged();
    }

    /** Strength points of a set of side helmets. */
    public static int points(List<Item> helmets) {
        int sum = 0;
        for (Item helmet : helmets) sum += points(helmet);
        return sum;
    }

    /** Makes the Wither a little stronger for its side helmets and remembers them for the loot (tags). */
    public static void apply(BlightedWither wither, List<Item> helmets) {
        int index = 0;
        for (Item helmet : helmets) wither.addTag(TAG + index++ + "=" + BuiltInRegistries.ITEM.getKey(helmet));
        int points = points(helmets);
        if (points <= 0) return;
        AttributeInstance health = wither.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, points * HEALTH_PER_POINT, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance armor = wither.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.addOrReplacePermanentModifier(new AttributeModifier(ARMOR_ID, points * ARMOR_PER_POINT, AttributeModifier.Operation.ADD_VALUE));
        }
        wither.setHealth(wither.getMaxHealth());
        wither.addExperience(points * XP_PER_POINT);
    }

    /** The side helmets a Wither remembers (tags), in the order they were put on. */
    public static List<Item> helmets(Entity wither) {
        java.util.TreeMap<Integer, Item> byIndex = new java.util.TreeMap<>();
        for (String tag : wither.entityTags()) {
            int eq = tag.indexOf('=');
            if (!tag.startsWith(TAG) || eq <= TAG.length()) continue;
            try {
                Identifier id = Identifier.tryParse(tag.substring(eq + 1));
                if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> byIndex.put(Integer.parseInt(tag.substring(TAG.length(), eq)), item));
            } catch (NumberFormatException ignored) {
                // a foreign tag that only looks like ours
            }
        }
        return new ArrayList<>(byIndex.values());
    }

    /**
     * What the dead Wither gives for its side helmets: per material 1 to 1.5 times the pieces of all helmets of it (two Diamond
     * Helmets: 10 to 15 Diamonds), and for a Chainmail Helmet the forbidden fire.
     */
    public static List<ItemStack> loot(List<Item> helmets, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        List<Item> done = new ArrayList<>();
        for (Item helmet : helmets) {
            if (helmet == Items.CHAINMAIL_HELMET) {
                out.add(forbiddenFire());
                continue;
            }
            Item material = material(helmet);
            if (material == null || done.contains(helmet)) continue;
            done.add(helmet);
            int base = PIECES * (int) helmets.stream().filter(h -> h == helmet).count();
            int count = base + random.nextInt((int) (base * (LOOT_MAX_FACTOR - 1.0)) + 1);
            out.add(new ItemStack(material, count));
        }
        return out;
    }

    /**
     * The "forbidden" item of the old chainmail recipe (Beta/Alpha: five pieces of fire in the helmet shape, fire being no item
     * you could hold): a Fire Charge named Fire in the Cursed rarity of Simple Lib. A collectible, it does nothing.
     */
    public static ItemStack forbiddenFire() {
        ItemStack fire = new ItemStack(Items.FIRE_CHARGE);
        fire.set(DataComponents.ITEM_NAME, Component.translatable("item.simplemobs.forbidden_fire"));
        fire.set(LibComponents.RARITY, SimpleRarity.CURSED);
        return fire;
    }
}
