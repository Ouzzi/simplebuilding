package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.networking.MasterBuilderPickPayload;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.OpenBackpackPayload;
import com.simplebuilding.screen.BackpackContainer;
import com.simplebuilding.screen.BackpackLayout;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackMenuProviders;
import com.simplebuilding.screen.BackpackOpenData;
import com.simplebuilding.util.ModTags;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The four backpacks, server side: wearing, the tiers, the contents through every upgrade, placing
 * and breaking, the menu behind the backpack key, and the four enchantments that reach them.
 *
 * <p>The word "backpack" means the item {@code simplebuilding:backpack} and its three upper tiers
 * here - not the main inventory, which older tests call the backpack.
 *
 * <p><b>Players.</b> Where a plain {@code Player} is enough (wearing, placing, the pickup, the hand
 * refill) the tests use {@code GameTestHelper#makeMockPlayer(GameType.SURVIVAL)}: it exists on both
 * Minecraft lines, is really in survival - so a right click moves the backpack instead of copying it,
 * as vanilla's {@code Equippable#swapWithEquipmentSlot} does for creative players - and it is never
 * put into the player list. Where the mod's code insists on a {@code ServerPlayer} (the menu gate,
 * the payload handlers, the building wand) the in-level mock is used and handed back at the end.
 *
 * <p><b>Menus are built, not sent.</b> The backpack key's real path ends in
 * {@code BackpackMenus#openWorn}, which on NeoForge sends {@code neoforge:advanced_open_screen}; a mock
 * player's connection refuses that packet. The tests therefore build the menu through the same
 * loader-neutral {@code BackpackMenuProviders#worn} the platform class uses and drive it directly, the
 * way {@code HopperTests} does.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The screen</b>: tint, the extra columns' art, the recipe book. Client side.</li>
 *   <li><b>A placed backpack's own menu</b> ({@code BackpackBlock#useWithoutItem}): it goes through the
 *       same platform send as the key.</li>
 *   <li><b>The creative break</b> ({@code BackpackBlock#playerWillDestroy}), which needs a creative
 *       player breaking the block through the game mode.</li>
 * </ul>
 */
public final class BackpackTests {

    /** First menu index of the backpack's own slots, behind vanilla's 46. */
    private static final int FIRST_BACKPACK_SLOT = BackpackMenu.BACKPACK_SLOT_START;

    /** Menu index of the chest armour slot, as in {@code InventoryMenu}. */
    private static final int CHEST_SLOT = 6;

    /** Where the building wand is clicked in {@link #masterBuilderOpensTheBackpackOnlyWhenTheBackpackCarriesIt}. */
    private static final BlockPos WAND_ANCHOR = new BlockPos(3, 3, 3);

    /** Upper bound for driving the wand's own tick; a single block needs one or two. */
    private static final int WAND_TICK_CAP = 20;

    /** More than an {@code ItemEntity}'s five hit points. */
    private static final float LETHAL_DAMAGE = 6.0F;

    private BackpackTests() {
    }

    // =====================================================================================
    // WEARING
    // =====================================================================================

    /**
     * A right click with a backpack in hand wears it in the chest slot, through vanilla's
     * {@code EQUIPPABLE} swap - the same click that wears a chestplate.
     *
     * <ul>
     *   <li><b>Empty chest:</b> the backpack goes on and the hand is empty afterwards (a survival
     *       player; creative would copy).</li>
     *   <li><b>A chestplate worn:</b> the backpack goes on, the chestplate comes into the hand, and the
     *       backpack's contents travel with it.</li>
     *   <li><b>And back:</b> right clicking with the chestplate swaps the backpack into the hand again,
     *       contents and all.</li>
     *   <li><b>Only players:</b> a zombie may not wear it - the component names players only, so no mob
     *       picks up a lost backpack and walks off with its contents.</li>
     * </ul>
     *
     * <p>What breaks this test: a backpack that is not swappable, one that equips into another slot,
     * losing the contents on the way, and allowing other entities to wear it.
     */
    public static void rightClickWearsTheBackpackAndSwapsItWithTheChestplate(GameTestHelper helper) {
        Player player = survivalPlayer(helper);

        // --- empty chest ---
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BACKPACK));
        rightClick(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BACKPACK),
                "a right click with the backpack did not put it into the chest slot, the chest holds "
                        + player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "the backpack went on but stayed in the survival player's hand as well: " + player.getMainHandItem());

        // --- a chestplate worn: swapped into the hand, the contents travel along ---
        BackpackContents contents = contentsOf(new ItemStack(Items.COBBLESTONE, 32), new ItemStack(Items.TORCH, 5));
        ItemStack netherite = backpackWith(ModItems.NETHERITE_BACKPACK, contents);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.setItemInHand(InteractionHand.MAIN_HAND, netherite);
        rightClick(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NETHERITE_BACKPACK),
                "a right click with the backpack did not swap it with the worn chestplate");
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_CHESTPLATE),
                "the chestplate did not come into the hand when the backpack took its place: "
                        + player.getMainHandItem());
        helper.assertValueEqual(BackpackItem.contents(player.getItemBySlot(EquipmentSlot.CHEST)), contents,
                "contents of the backpack after it was put on");

        // --- and back again with the chestplate ---
        rightClick(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)
                        && player.getMainHandItem().is(ModItems.NETHERITE_BACKPACK),
                "right clicking with the chestplate did not swap the backpack back into the hand");
        helper.assertValueEqual(BackpackItem.contents(player.getMainHandItem()), contents,
                "contents of the backpack after it was taken off again");

        // --- only players ---
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
        helper.assertTrue(!zombie.isEquippableInSlot(new ItemStack(ModItems.BACKPACK), EquipmentSlot.CHEST),
                "a zombie may wear the backpack; the component is meant for players only");
        helper.assertTrue(zombie.isEquippableInSlot(new ItemStack(Items.DIAMOND_CHESTPLATE), EquipmentSlot.CHEST),
                "a zombie may not wear a diamond chestplate either, so the check above proves nothing");
        zombie.discard();

        helper.succeed();
    }

    /**
     * The four tiers, their numbers and where their slots sit.
     *
     * <ul>
     *   <li><b>Armour</b> 1 / 2 / 3 / 4, read from the item's attribute modifiers for the chest slot -
     *       and 0 for the head slot, so the modifier is bound to the chest.</li>
     *   <li><b>Capacity</b> 9 / 18 / 33 / 50 slots, and the menu is vanilla's 46 slots plus exactly
     *       that many.</li>
     *   <li><b>The vanilla part</b> of the menu is the player inventory in {@code InventoryMenu}'s own
     *       order: every armour, main, hotbar and off hand slot reads the same inventory index as the
     *       player's inventory menu, and for the basic tier the top part sits at the same place.</li>
     *   <li><b>Slot ids</b> in the contents component are the same for every tier: rows use 0 to 35, the
     *       extra columns start at 36. The netherite backpack's first column slot is id 36, and the
     *       enderite backpack puts id 36 into its first column too - so an upgrade keeps a column item
     *       in a column. The basic backpack has no slot for id 36 at all.</li>
     *   <li><b>Layout</b>: the netherite column stands right of the grid at x 170, the enderite
     *       backpack's second column left of it, and the image grows by 18 pixels per row and column.</li>
     * </ul>
     *
     * <p>What breaks this test: another armour value or slot group in {@code ModItems#backpack},
     * another row or column count in {@code BackpackTier}, slot ids that depend on the tier, and a
     * vanilla part that is not {@code InventoryMenu}'s.
     */
    public static void tiersCarryTheirArmorSlotCountAndSlotLayout(GameTestHelper helper) {
        Player player = survivalPlayer(helper);
        Item[] items = {ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK};
        int[] armor = {1, 2, 3, 4};
        int[] slots = {9, 18, 33, 50};

        for (int i = 0; i < items.length; i++) {
            ItemStack stack = new ItemStack(items[i]);
            ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            helper.assertValueEqual(modifiers.compute(Attributes.ARMOR, 0.0D, EquipmentSlot.CHEST), (double) armor[i],
                    "armor of the worn " + items[i]);
            helper.assertValueEqual(modifiers.compute(Attributes.ARMOR, 0.0D, EquipmentSlot.HEAD), 0.0D,
                    "armor of " + items[i] + " in the head slot");

            BackpackTier tier = ((BackpackItem) items[i]).getTier();
            helper.assertValueEqual(tier.slotCount(), slots[i], "slots of " + items[i]);
            BackpackMenu menu = menuFor(player, tier);
            helper.assertValueEqual(menu.slots.size(), FIRST_BACKPACK_SLOT + slots[i], "menu slots of " + items[i]);
            assertVanillaPart(helper, player, menu, tier == BackpackTier.BASIC);
        }

        // --- slot ids that mean the same on every tier ---
        BackpackTier netherite = BackpackTier.NETHERITE;
        BackpackTier enderite = BackpackTier.ENDERITE;
        helper.assertValueEqual(netherite.componentSlot(netherite.rowSlots()), BackpackTier.COLUMN_SLOT_BASE,
                "component id of the netherite backpack's first column slot");
        helper.assertValueEqual(enderite.containerIndex(BackpackTier.COLUMN_SLOT_BASE), enderite.rowSlots(),
                "enderite slot that takes over the netherite column's first item");
        helper.assertTrue(enderite.isColumnSlot(enderite.containerIndex(BackpackTier.COLUMN_SLOT_BASE)),
                "the netherite column's first item lands in a row of the enderite backpack, not in its column");
        helper.assertValueEqual(BackpackTier.BASIC.containerIndex(BackpackTier.COLUMN_SLOT_BASE), -1,
                "basic backpack slot for a column id");
        helper.assertValueEqual(enderite.componentSlot(enderite.rowSlots() + enderite.columnHeight()),
                BackpackTier.COLUMN_SLOT_BASE + BackpackTier.MAX_COLUMN_HEIGHT,
                "component id of the enderite backpack's second column");

        // --- where they sit ---
        BackpackLayout basicLayout = new BackpackLayout(BackpackTier.BASIC);
        helper.assertValueEqual(basicLayout.backpackSlotX(0), 8, "x of the basic backpack's first slot");
        helper.assertValueEqual(basicLayout.backpackSlotY(0), 84, "y of the basic backpack's first slot");
        helper.assertValueEqual(basicLayout.mainRowY(0), 102, "y of the main inventory under one backpack row");
        helper.assertValueEqual(basicLayout.imageHeight(), 184, "height of the basic backpack screen");
        BackpackLayout netheriteLayout = new BackpackLayout(netherite);
        helper.assertValueEqual(netheriteLayout.backpackSlotX(netherite.rowSlots()), 170,
                "x of the netherite backpack's column");
        helper.assertValueEqual(netheriteLayout.backpackSlotY(netherite.slotCount() - 1), 84 + 18 * 5,
                "y of the netherite column's last slot, level with the last main inventory row");
        helper.assertValueEqual(netheriteLayout.imageWidth(), 194, "width of the netherite backpack screen");
        BackpackLayout enderiteLayout = new BackpackLayout(enderite);
        helper.assertValueEqual(enderiteLayout.backpackSlotX(enderite.rowSlots() + enderite.columnHeight()), 8,
                "x of the enderite backpack's left column");
        helper.assertValueEqual(enderiteLayout.imageWidth(), 212, "width of the enderite backpack screen");
        helper.assertValueEqual(enderiteLayout.imageHeight(), 238, "height of the enderite backpack screen");

        helper.succeed();
    }

    // =====================================================================================
    // CONTENTS THROUGH EVERY UPGRADE, PLACING AND BREAKING
    // =====================================================================================

    /**
     * Crafting and smithing a backpack up keeps everything it carries.
     *
     * <ul>
     *   <li><b>Basic recipe:</b> {@code NSN / PPP / III} (copper nuggets, string, leather sheets, iron
     *       bars) crafts one empty backpack.</li>
     *   <li><b>Reinforced recipe</b> ({@code simplebuilding:backpack_upgrade}, {@code DLD / LBL / LLL}):
     *       contents, name and enchantments of the backpack in the middle arrive on the reinforced one.
     *       The same items with the backpack in a corner craft nothing.</li>
     *   <li><b>Netherite and enderite smithing</b> keep them too, and an item in the netherite
     *       backpack's column keeps its slot id and sits in the enderite backpack's column afterwards.</li>
     *   <li><b>No skipped tier:</b> the enderite upgrade refuses a reinforced backpack.</li>
     * </ul>
     *
     * <p>What breaks this test: a reinforced recipe that builds a fresh backpack (plain
     * {@code crafting_shaped}), smithing that loses components, slot ids renumbered per tier, and a
     * tier chain with a gap or a shortcut.
     */
    public static void contentsSurviveTheReinforcedRecipeAndBothSmithingUpgrades(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // --- the basic backpack ---
        ItemStack n = new ItemStack(Items.COPPER_NUGGET);
        ItemStack s = new ItemStack(Items.STRING);
        ItemStack p = new ItemStack(ModItems.LEATHER_SHEET);
        ItemStack b = new ItemStack(Items.IRON_BARS);
        ItemStack basic = craft(helper, level, CraftingInput.of(3, 3, List.of(n, s, n, p, p, p, b, b, b)),
                "copper nuggets, string, leather sheets and iron bars", "simplebuilding:backpack");
        helper.assertTrue(basic.is(ModItems.BACKPACK) && basic.getCount() == 1,
                "the backpack pattern crafts " + basic + " instead of one backpack");
        helper.assertTrue(BackpackItem.contents(basic).isEmpty(), "a freshly crafted backpack is not empty");

        // --- basic -> reinforced ---
        BackpackContents contents = contentsOf(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIAMOND, 3));
        ItemStack worn = backpackWith(ModItems.BACKPACK, contents);
        worn.set(DataComponents.CUSTOM_NAME, Component.literal("Kit"));
        worn.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);

        ItemStack d = new ItemStack(ModItems.DIAMOND_PEBBLE);
        ItemStack reinforced = craft(helper, level, CraftingInput.of(3, 3, List.of(d, p, d, p, worn, p, p, p, p)),
                "the reinforced backpack pattern", "simplebuilding:reinforced_backpack");
        helper.assertTrue(reinforced.is(ModItems.REINFORCED_BACKPACK),
                "the reinforced backpack pattern crafts " + reinforced);
        helper.assertValueEqual(BackpackItem.contents(reinforced), contents,
                "contents the reinforced backpack kept from the backpack it was made of");
        assertKeptNameAndFunnel(helper, reinforced, "Kit", "the reinforced backpack");
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                        CraftingInput.of(3, 3, List.of(worn, p, d, p, d, p, p, p, p)), level).isEmpty(),
                "the reinforced backpack pattern with the backpack in a corner crafts something, so the "
                        + "upgrade recipe is not shaped");

        // --- reinforced -> netherite ---
        ItemStack netherite = smith(helper, level, new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), reinforced, new ItemStack(Items.NETHERITE_INGOT)),
                "the reinforced backpack on the netherite template");
        helper.assertTrue(netherite.is(ModItems.NETHERITE_BACKPACK), "netherite smithing made " + netherite);
        helper.assertValueEqual(BackpackItem.contents(netherite), contents,
                "contents the netherite backpack kept from the reinforced one");
        assertKeptNameAndFunnel(helper, netherite, "Kit", "the netherite backpack");

        // --- netherite -> enderite, with an item in the netherite column ---
        BackpackTier netheriteTier = BackpackTier.NETHERITE;
        BackpackContents withColumn = new BackpackContents(List.of(
                BackpackContents.Entry.of(0, new ItemStack(Items.COBBLESTONE, 64)),
                BackpackContents.Entry.of(netheriteTier.componentSlot(netheriteTier.rowSlots()), new ItemStack(Items.EMERALD, 7))));
        netherite.set(ModDataComponentTypes.BACKPACK_CONTENTS, withColumn);
        ItemStack enderite = smith(helper, level, new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE), netherite, new ItemStack(ModItems.ENDERITE_INGOT)),
                "the netherite backpack on the enderite template");
        helper.assertTrue(enderite.is(ModItems.ENDERITE_BACKPACK), "enderite smithing made " + enderite);
        helper.assertValueEqual(BackpackItem.contents(enderite), withColumn,
                "contents the enderite backpack kept from the netherite one");
        BackpackContainer opened = new BackpackContainer(BackpackTier.ENDERITE, 1);
        opened.load(BackpackItem.contents(enderite));
        int columnSlot = BackpackTier.ENDERITE.rowSlots();
        helper.assertTrue(opened.getItem(columnSlot).is(Items.EMERALD) && BackpackTier.ENDERITE.isColumnSlot(columnSlot),
                "the emeralds from the netherite column did not land in the enderite backpack's column");
        assertKeptNameAndFunnel(helper, enderite, "Kit", "the enderite backpack");

        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,
                        new SmithingRecipeInput(new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                                new ItemStack(ModItems.REINFORCED_BACKPACK), new ItemStack(ModItems.ENDERITE_INGOT)), level).isEmpty(),
                "the enderite upgrade accepts a reinforced backpack, so the netherite tier can be skipped");

        helper.succeed();
    }

    /**
     * Sneak + right click on a block sets the backpack down as a block; a plain right click does not.
     * Breaking the placed backpack gives back exactly one item, the backpack, with its contents, its
     * name and its enchantments - and nothing spills.
     *
     * <p>The player is in survival, so the placed backpack also leaves the hand.
     *
     * <p>What breaks this test: placing without sneaking, a block entity that does not take the item's
     * contents, a loot table without {@code copy_components} (the backpack drops empty), one that
     * spills the contents as separate stacks, and one that drops nothing.
     */
    public static void sneakRightClickPlacesTheBackpackAndBreakingItDropsEverything(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survivalPlayer(helper);
        BlockPos support = new BlockPos(2, 1, 2);
        BlockPos placed = support.above();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(placed, Blocks.AIR);

        BackpackContents contents = contentsOf(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIAMOND, 3));
        ItemStack backpack = backpackWith(ModItems.REINFORCED_BACKPACK, contents);
        backpack.set(DataComponents.CUSTOM_NAME, Component.literal("Camp"));
        backpack.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);

        // --- a plain right click on a block does not place it ---
        player.setShiftKeyDown(false);
        InteractionResult plain = backpack.useOn(clickTop(helper, player, support));
        helper.assertTrue(!plain.consumesAction() && helper.getBlockState(placed).isAir(),
                "a right click without sneaking placed the backpack; only sneak + right click may set it down");

        // --- sneaking places it ---
        player.setShiftKeyDown(true);
        backpack.useOn(clickTop(helper, player, support));
        player.setShiftKeyDown(false);
        helper.assertTrue(helper.getBlockState(placed).is(ModBlocks.REINFORCED_BACKPACK),
                "sneak + right click did not place the backpack, found " + helper.getBlockState(placed));
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "the placed backpack is still in the survival player's hand");
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(placed)) instanceof BackpackBlockEntity,
                "the placed backpack has no backpack block entity");
        BackpackBlockEntity entity = (BackpackBlockEntity) level.getBlockEntity(helper.absolutePos(placed));
        helper.assertValueEqual(entity.container().toContents(), contents, "contents of the placed backpack");

        // --- breaking it drops exactly the backpack, with everything ---
        level.destroyBlock(helper.absolutePos(placed), true);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds());
        helper.assertValueEqual(drops.size(), 1, "item entities after breaking the placed backpack");
        ItemStack dropped = drops.get(0).getItem();
        helper.assertTrue(dropped.is(ModItems.REINFORCED_BACKPACK),
                "breaking the placed backpack dropped " + dropped + " instead of the backpack");
        helper.assertValueEqual(BackpackItem.contents(dropped), contents,
                "contents of the backpack that dropped from the broken block");
        assertKeptNameAndFunnel(helper, dropped, "Camp", "the dropped backpack");
        drops.get(0).discard();

        helper.succeed();
    }

    // =====================================================================================
    // THE MENU
    // =====================================================================================

    /**
     * The backpack key ({@code OpenBackpackPayload}) opens a backpack menu only for a worn backpack.
     *
     * <ul>
     *   <li><b>Nothing on the chest:</b> the gate is shut and the handler leaves the player in vanilla's
     *       own inventory menu with its 46 slots - the client shows the normal inventory then.</li>
     *   <li><b>A backpack only in the hand:</b> still shut.</li>
     *   <li><b>Worn:</b> the gate opens, and the menu the provider builds is a backpack menu with 46 + 9
     *       slots.</li>
     *   <li><b>Another menu already open:</b> shut again, so the key never stacks a menu on a menu.</li>
     * </ul>
     *
     * <p>What breaks this test: a gate that does not look at the chest slot, one that accepts a
     * backpack anywhere else, and one that ignores an open menu.
     */
    public static void openKeyOpensTheMenuOnlyForTheWornBackpack(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper);
        player.getInventory().clearContent();

        // --- nothing on the chest ---
        helper.assertFalse(BackpackMenuProviders.canOpenWorn(player),
                "the backpack key may open a menu for a player with nothing on the chest");
        ModMessageHandlers.handleOpenBackpack(new OpenBackpackPayload(), player);
        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                "the backpack key opened " + player.containerMenu + " without a worn backpack");
        helper.assertValueEqual(player.inventoryMenu.slots.size(), 46, "slots of the vanilla inventory menu");

        // --- only in the hand ---
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BACKPACK));
        helper.assertFalse(BackpackMenuProviders.canOpenWorn(player),
                "the backpack key may open a menu for a backpack that is only held, not worn");
        ModMessageHandlers.handleOpenBackpack(new OpenBackpackPayload(), player);
        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                "the backpack key opened a menu for a backpack in the hand");

        // --- worn ---
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BACKPACK));
        helper.assertTrue(BackpackMenuProviders.canOpenWorn(player),
                "the backpack key may not open the menu of a worn backpack");
        AbstractContainerMenu menu = BackpackMenuProviders.worn(player).provider()
                .createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof BackpackMenu,
                "the worn backpack's provider built " + menu + " instead of a backpack menu");
        helper.assertValueEqual(menu.slots.size(), 46 + 9, "slots of the basic backpack's menu");

        // --- another menu open ---
        player.containerMenu = menu;
        helper.assertFalse(BackpackMenuProviders.canOpenWorn(player),
                "the backpack key may open a second menu on top of an open one");
        player.containerMenu = player.inventoryMenu;

        helper.succeed();
    }

    /**
     * Shift-click in the backpack menu: from the main inventory and the hotbar into the backpack first,
     * out of the backpack into the main inventory before the hotbar - and the worn backpack's own chest
     * slot stays locked.
     *
     * <ul>
     *   <li>Cobblestone from main inventory slot 9 lands in the first backpack slot; glass from hotbar
     *       slot 0 lands in the second. Both are written straight into the worn backpack's
     *       component.</li>
     *   <li>Shift-clicking the cobblestone out of the backpack puts it into the main inventory, not the
     *       hotbar.</li>
     *   <li>Shift-clicking the chest slot leaves the backpack where it is while its menu is open.</li>
     *   <li>With the backpack full, a main inventory stack falls back to vanilla's main-to-hotbar
     *       move.</li>
     * </ul>
     *
     * <p>What breaks this test: the backpack range after vanilla's main/hotbar swap, backpack stacks
     * going to the hotbar first, a chest slot that is not locked, and a container that does not write
     * through to the worn stack.
     */
    public static void shiftClickFillsTheBackpackFirstAndEmptiesItIntoTheMainInventory(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper);
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BACKPACK));
        BackpackMenu menu = openWorn(player);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        // --- main inventory and hotbar go into the backpack first ---
        inventory.setItem(9, new ItemStack(Items.COBBLESTONE, 32));
        inventory.setItem(0, new ItemStack(Items.GLASS, 10));
        shiftClick(menu, 9, player);
        helper.assertTrue(menu.getSlot(FIRST_BACKPACK_SLOT).getItem().is(Items.COBBLESTONE) && inventory.getItem(9).isEmpty(),
                "shift-click from the main inventory did not put the cobblestone into the backpack; the "
                        + "first backpack slot holds " + menu.getSlot(FIRST_BACKPACK_SLOT).getItem());
        shiftClick(menu, BackpackMenu.USE_ROW_SLOT_START, player);
        helper.assertTrue(menu.getSlot(FIRST_BACKPACK_SLOT + 1).getItem().is(Items.GLASS) && inventory.getItem(0).isEmpty(),
                "shift-click from the hotbar did not put the glass into the backpack");
        helper.assertValueEqual(BackpackItem.contents(chest), contentsOf(new ItemStack(Items.COBBLESTONE, 32), new ItemStack(Items.GLASS, 10)),
                "contents of the worn backpack after two shift-clicks into its menu");

        // --- out of the backpack: main inventory before hotbar ---
        shiftClick(menu, FIRST_BACKPACK_SLOT, player);
        helper.assertTrue(inventory.getItem(9).is(Items.COBBLESTONE) && !inventory.getItem(0).is(Items.COBBLESTONE),
                "shift-click out of the backpack did not put the cobblestone into the first main inventory "
                        + "slot; the hotbar is only the fallback");

        // --- the worn backpack's chest slot is locked ---
        shiftClick(menu, CHEST_SLOT, player);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BACKPACK),
                "shift-click took the worn backpack out of the chest slot while its own menu was open");

        // --- the backpack full: vanilla's main -> hotbar ---
        for (int i = 0; i < menu.backpack().getContainerSize(); i++) {
            menu.backpack().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        inventory.setItem(10, new ItemStack(Items.STONE, 5));
        shiftClick(menu, 10, player);
        boolean inHotbar = false;
        for (int i = 0; i < 9; i++) {
            inHotbar |= inventory.getItem(i).is(Items.STONE);
        }
        helper.assertTrue(inHotbar && inventory.getItem(10).isEmpty(),
                "with the backpack full, shift-click did not fall back to vanilla's move into the hotbar");

        helper.succeed();
    }

    /**
     * Deep Pockets multiplies a backpack slot's stack limit by 1 / 2 / 4 - for stackable items only -
     * and an oversized stack only ever leaves the backpack one normal stack at a time.
     *
     * <ul>
     *   <li><b>Limits</b> per level 0 / I / II: cobblestone 64 / 128 / 256, ender pearls 16 / 32 / 64, a
     *       sword 1 / 1 / 1.</li>
     *   <li><b>Merging:</b> with Deep Pockets II, four stacks of cobblestone shift-clicked in end up as
     *       one slot of 256, and the worn backpack's component stores 256.</li>
     *   <li><b>Taking out:</b> picking that slot up puts 64 on the cursor and leaves 192.</li>
     *   <li><b>Saving:</b> 192 is above vanilla's count limit of 99; it survives the component codec.</li>
     * </ul>
     *
     * <p>What breaks this test: another multiplier table, scaling unstackable items, a pickup that puts
     * the whole oversized stack on the cursor, and a codec capped at vanilla's 99.
     */
    public static void deepPocketsRaisesStackLimitsOnlyForStackables(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper);
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        int[] multipliers = {1, 2, 4};
        for (int level = 0; level <= 2; level++) {
            ItemStack backpack = new ItemStack(ModItems.BACKPACK);
            if (level > 0) {
                backpack.enchant(enchantment(helper, ModEnchantments.DEEP_POCKETS), level);
            }
            player.setItemSlot(EquipmentSlot.CHEST, backpack);
            Slot slot = openWorn(player).getSlot(FIRST_BACKPACK_SLOT);
            int m = multipliers[level];
            helper.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.COBBLESTONE)), 64 * m,
                    "cobblestone per backpack slot at Deep Pockets " + level);
            helper.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)), 16 * m,
                    "ender pearls per backpack slot at Deep Pockets " + level);
            helper.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)), 1,
                    "swords per backpack slot at Deep Pockets " + level + " - unstackable items never stack");
        }

        // --- Deep Pockets II: four stacks become one slot of 256 ---
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        backpack.enchant(enchantment(helper, ModEnchantments.DEEP_POCKETS), 2);
        player.setItemSlot(EquipmentSlot.CHEST, backpack);
        BackpackMenu menu = openWorn(player);
        for (int i = 9; i <= 12; i++) {
            inventory.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        for (int i = 9; i <= 12; i++) {
            shiftClick(menu, i, player);
        }
        helper.assertValueEqual(menu.getSlot(FIRST_BACKPACK_SLOT).getItem().getCount(), 256,
                "cobblestone in the first slot of a Deep Pockets II backpack after four stacks went in");
        helper.assertTrue(menu.getSlot(FIRST_BACKPACK_SLOT + 1).getItem().isEmpty(),
                "four stacks of cobblestone spilled into a second backpack slot instead of filling one to 256");
        helper.assertValueEqual(BackpackItem.contents(backpack).stackAt(0).getCount(), 256,
                "cobblestone the worn backpack's component stores");

        // --- an oversized stack leaves one normal stack at a time ---
        leftClick(menu, FIRST_BACKPACK_SLOT, player);
        helper.assertValueEqual(menu.getCarried().getCount(), 64,
                "cobblestone on the cursor after picking up the oversized stack");
        helper.assertValueEqual(menu.getSlot(FIRST_BACKPACK_SLOT).getItem().getCount(), 192,
                "cobblestone left in the slot after picking up one stack");
        menu.setCarried(ItemStack.EMPTY);

        // --- 192 is more than vanilla's 99, and the component still saves it ---
        BackpackContents written = BackpackItem.contents(backpack);
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag encoded = BackpackContents.CODEC.encodeStart(ops, written).getOrThrow();
        BackpackContents decoded = BackpackContents.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertValueEqual(decoded, written, "backpack contents after a save and load");
        helper.assertValueEqual(decoded.stackAt(0).getCount(), 192, "cobblestone after a save and load");

        helper.succeed();
    }

    // =====================================================================================
    // THE ENCHANTMENTS AT WORK
    // =====================================================================================

    /**
     * Funnel on the worn backpack vacuums picked-up items into it, the way Funnel does on a bundle.
     *
     * <ul>
     *   <li><b>Funnel II:</b> cobblestone touched by the player goes into the worn backpack, not into
     *       the inventory, and the item entity is gone.</li>
     *   <li><b>Funnel I:</b> only sorts that are already in the backpack - dirt joins the dirt inside,
     *       stone goes into the inventory.</li>
     *   <li><b>No Funnel:</b> everything goes into the inventory, the backpack stays empty.</li>
     * </ul>
     *
     * <p>What breaks this test: dropping the worn-backpack branch from {@code ItemEntityMixin}, a
     * Funnel I that takes anything, and a backpack without Funnel that vacuums anyway.
     */
    public static void funnelPullsPickedUpItemsIntoTheWornBackpack(GameTestHelper helper) {
        Player player = survivalPlayer(helper);

        // --- Funnel II ---
        ItemStack funnelTwo = new ItemStack(ModItems.BACKPACK);
        funnelTwo.enchant(enchantment(helper, ModEnchantments.FUNNEL), 2);
        player.setItemSlot(EquipmentSlot.CHEST, funnelTwo);
        ItemEntity cobble = drop(helper, new ItemStack(Items.COBBLESTONE, 8), new Vec3(2.5, 2.0, 2.5));
        cobble.playerTouch(player);
        helper.assertValueEqual(countIn(funnelTwo, Items.COBBLESTONE), 8,
                "cobblestone a worn Funnel II backpack took from the ground");
        helper.assertTrue(cobble.isRemoved() && carried(player, Items.COBBLESTONE) == 0,
                "the cobblestone went into the Funnel II backpack but also stayed on the ground or reached the inventory");

        // --- Funnel I: only what is already in there ---
        ItemStack funnelOne = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.DIRT, 1)));
        funnelOne.enchant(enchantment(helper, ModEnchantments.FUNNEL), 1);
        player.setItemSlot(EquipmentSlot.CHEST, funnelOne);
        drop(helper, new ItemStack(Items.DIRT, 5), new Vec3(3.5, 2.0, 2.5)).playerTouch(player);
        drop(helper, new ItemStack(Items.STONE, 5), new Vec3(4.5, 2.0, 2.5)).playerTouch(player);
        helper.assertValueEqual(countIn(funnelOne, Items.DIRT), 6, "dirt in the Funnel I backpack that already held dirt");
        helper.assertValueEqual(countIn(funnelOne, Items.STONE), 0,
                "stone a Funnel I backpack took although it held none - Funnel I only takes sorts that are already in");
        helper.assertValueEqual(carried(player, Items.STONE), 5, "stone that reached the inventory past the Funnel I backpack");

        // --- no Funnel ---
        ItemStack plain = new ItemStack(ModItems.BACKPACK);
        player.setItemSlot(EquipmentSlot.CHEST, plain);
        drop(helper, new ItemStack(Items.GRAVEL, 5), new Vec3(5.5, 2.0, 2.5)).playerTouch(player);
        helper.assertTrue(BackpackItem.contents(plain).isEmpty(), "a backpack without Funnel took an item from the ground");
        helper.assertValueEqual(carried(player, Items.GRAVEL), 5, "gravel that reached the inventory past a plain backpack");

        helper.succeed();
    }

    /**
     * Master Builder opens the worn backpack as a material source - but only Master Builder on the
     * backpack itself, not on the wand. And a backpack is never building material.
     *
     * <ul>
     *   <li><b>Plain wand, Master Builder backpack</b> holding glass: the wand builds its block out of
     *       the backpack.</li>
     *   <li><b>Master Builder wand, plain backpack:</b> the click is refused and nothing is spent.</li>
     *   <li><b>A spare backpack in the hotbar</b> and nothing else: refused - a backpack is a block item,
     *       but the wand must never place one.</li>
     *   <li><b>The pick:</b> {@code MasterBuilderPickPayload} for cobblestone takes one normal stack out of
     *       a worn Master Builder backpack holding 256 (Deep Pockets II) into the empty hand; the same
     *       pick from a backpack without Master Builder does nothing.</li>
     * </ul>
     *
     * <p>What breaks this test: reading Master Builder off the wand for the backpack, reading any worn
     * backpack, treating a backpack as a building block, and a pick that hands out the oversized stack
     * or ignores the enchantment.
     */
    public static void masterBuilderOpensTheBackpackOnlyWhenTheBackpackCarriesIt(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper);
        BlockPos target = WAND_ANCHOR.above();

        // --- plain wand, Master Builder backpack ---
        resetWandSite(helper);
        ItemStack builderBackpack = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.GLASS, 16)));
        builderBackpack.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        ItemStack plainWand = singleBlockWand();
        armWand(player, plainWand, builderBackpack);
        InteractionResult armed = useWand(helper, player, plainWand);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand refused a click although the worn Master Builder backpack held glass, it returned " + armed);
        driveUntilIdle(helper, player, plainWand);
        helper.assertBlockPresent(Blocks.GLASS, target);
        helper.assertValueEqual(countIn(builderBackpack, Items.GLASS), 15,
                "glass left in the Master Builder backpack after the wand built one block from it");

        // --- Master Builder wand, plain backpack ---
        resetWandSite(helper);
        ItemStack plainBackpack = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.GLASS, 16)));
        ItemStack builderWand = singleBlockWand();
        builderWand.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        armWand(player, builderWand, plainBackpack);
        InteractionResult refused = useWand(helper, player, builderWand);
        helper.assertTrue(refused == InteractionResult.FAIL && helper.getBlockState(target).isAir(),
                "a backpack without Master Builder supplied the wand - Master Builder on the wand must not "
                        + "open the backpack, it returned " + refused);
        helper.assertValueEqual(countIn(plainBackpack, Items.GLASS), 16, "glass left in the plain backpack");

        // --- a spare backpack in the hotbar is no building material ---
        resetWandSite(helper);
        ItemStack lonelyWand = singleBlockWand();
        armWand(player, lonelyWand, ItemStack.EMPTY);
        player.getInventory().setItem(1, new ItemStack(ModItems.BACKPACK));
        InteractionResult noMaterial = useWand(helper, player, lonelyWand);
        helper.assertTrue(noMaterial == InteractionResult.FAIL && helper.getBlockState(target).isAir(),
                "the wand treated a backpack in the hotbar as building material, it returned " + noMaterial
                        + " and placed " + helper.getBlockState(target));

        // --- the pick takes one normal stack from the worn Master Builder backpack ---
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(2);
        ItemStack pickBackpack = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.COBBLESTONE, 256)));
        pickBackpack.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        pickBackpack.enchant(enchantment(helper, ModEnchantments.DEEP_POCKETS), 2);
        player.setItemSlot(EquipmentSlot.CHEST, pickBackpack);
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.COBBLESTONE)), player);
        helper.assertTrue(player.getMainHandItem().is(Items.COBBLESTONE),
                "the Master Builder pick did not take cobblestone out of the worn Master Builder backpack");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 64, "cobblestone the pick put into the hand");
        helper.assertValueEqual(countIn(pickBackpack, Items.COBBLESTONE), 192, "cobblestone left in the backpack after the pick");

        player.getInventory().clearContent();
        ItemStack noBuilder = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.COBBLESTONE, 64)));
        player.setItemSlot(EquipmentSlot.CHEST, noBuilder);
        ModMessageHandlers.handleMasterBuilderPick(new MasterBuilderPickPayload(new ItemStack(Items.COBBLESTONE)), player);
        helper.assertTrue(player.getMainHandItem().isEmpty() && countIn(noBuilder, Items.COBBLESTONE) == 64,
                "the Master Builder pick took from a backpack without Master Builder");

        helper.succeed();
    }

    /**
     * Constructor's Touch on the worn backpack refills the hand when placing a block used up the last
     * one, with one normal stack of the same block from the backpack.
     *
     * <ul>
     *   <li><b>Constructor's Touch:</b> the last stone in hand is placed, and the hand holds the 40 stone
     *       from the backpack straight afterwards; the backpack is empty.</li>
     *   <li><b>No Constructor's Touch:</b> the hand stays empty and the backpack keeps its stone.</li>
     *   <li><b>A hand that is not empty yet</b> is not refilled.</li>
     * </ul>
     *
     * <p>What breaks this test: dropping the refill from {@code BlockItemMixin}, refilling without the
     * enchantment, and refilling a hand that still holds blocks.
     */
    public static void constructorsTouchRefillsTheEmptyHandFromTheWornBackpack(GameTestHelper helper) {
        Player player = survivalPlayer(helper);
        BlockPos[] supports = {new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1)};
        for (BlockPos support : supports) {
            helper.setBlock(support, Blocks.STONE);
            helper.setBlock(support.above(), Blocks.AIR);
        }

        // --- Constructor's Touch refills ---
        ItemStack touch = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.STONE, 40)));
        touch.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        player.setItemSlot(EquipmentSlot.CHEST, touch);
        ItemStack last = new ItemStack(Items.STONE, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, last);
        last.useOn(clickTop(helper, player, supports[0]));
        helper.assertBlockPresent(Blocks.STONE, supports[0].above());
        helper.assertTrue(player.getMainHandItem().is(Items.STONE) && player.getMainHandItem().getCount() == 40,
                "Constructor's Touch did not refill the empty hand from the worn backpack, the hand holds "
                        + player.getMainHandItem());
        helper.assertTrue(BackpackItem.contents(touch).isEmpty(),
                "the stone that refilled the hand is still in the backpack as well");

        // --- no Constructor's Touch ---
        ItemStack plain = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.STONE, 40)));
        player.setItemSlot(EquipmentSlot.CHEST, plain);
        ItemStack another = new ItemStack(Items.STONE, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, another);
        another.useOn(clickTop(helper, player, supports[1]));
        helper.assertBlockPresent(Blocks.STONE, supports[1].above());
        helper.assertTrue(player.getMainHandItem().isEmpty() && countIn(plain, Items.STONE) == 40,
                "a backpack without Constructor's Touch refilled the hand");

        // --- the hand still holds some ---
        ItemStack touchAgain = backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.STONE, 40)));
        touchAgain.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        player.setItemSlot(EquipmentSlot.CHEST, touchAgain);
        ItemStack two = new ItemStack(Items.STONE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, two);
        two.useOn(clickTop(helper, player, supports[2]));
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && countIn(touchAgain, Items.STONE) == 40,
                "Constructor's Touch refilled a hand that still held a stone");

        helper.succeed();
    }

    /**
     * Four enchantments reach the backpacks and two deliberately do not. The anvil's question
     * ({@code Enchantment#canEnchant}) is asked for all four tiers:
     * <ul>
     *   <li><b>Deep Pockets, Funnel, Master Builder, Constructor's Touch:</b> accepted.</li>
     *   <li><b>Drawer and Colour Palette:</b> refused. They share their tags with Deep Pockets and Master
     *       Builder, which is why the backpacks come in through tags of their own
     *       ({@code deep_pockets_enchantable}, {@code funnel_enchantable},
     *       {@code master_builder_enchantable}) instead of joining {@code bundle_enchantable} or
     *       {@code extra_inventory_items}.</li>
     * </ul>
     * Controls: Drawer still goes on a reinforced bundle and Colour Palette on a building wand, so the
     * refusals are about the backpacks.
     *
     * <p>What breaks this test: a backpack missing from one of the four tags, backpacks added to
     * {@code bundle_enchantable} or {@code extra_inventory_items}, and one tier left out.
     */
    public static void backpacksTakeTheirFourEnchantmentsButNeitherDrawerNorColorPalette(GameTestHelper helper) {
        List<ResourceKey<Enchantment>> accepted = List.of(ModEnchantments.DEEP_POCKETS, ModEnchantments.FUNNEL,
                ModEnchantments.MASTER_BUILDER, ModEnchantments.CONSTRUCTORS_TOUCH);
        List<ResourceKey<Enchantment>> refused = List.of(ModEnchantments.DRAWER, ModEnchantments.COLOR_PALETTE);

        for (Item item : List.of(ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK,
                ModItems.ENDERITE_BACKPACK)) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(stack.is(ModTags.Items.BACKPACKS), item + " is missing from simplebuilding:backpacks");
            for (ResourceKey<Enchantment> key : accepted) {
                helper.assertTrue(enchantment(helper, key).value().canEnchant(stack),
                        key.identifier() + " cannot be put on " + item);
            }
            for (ResourceKey<Enchantment> key : refused) {
                helper.assertTrue(!enchantment(helper, key).value().canEnchant(stack),
                        key.identifier() + " can be put on " + item + ", but it means nothing on a backpack");
            }
        }

        helper.assertTrue(enchantment(helper, ModEnchantments.DRAWER).value().canEnchant(new ItemStack(ModItems.REINFORCED_BUNDLE)),
                "Drawer cannot go on a reinforced bundle any more, so its refusal on the backpacks proves nothing");
        helper.assertTrue(enchantment(helper, ModEnchantments.COLOR_PALETTE).value().canEnchant(new ItemStack(ModItems.DIAMOND_BUILDING_WAND)),
                "Colour Palette cannot go on a building wand any more, so its refusal on the backpacks proves nothing");

        helper.succeed();
    }

    // =====================================================================================
    // FIRE AND EXPLOSIONS
    // =====================================================================================

    /**
     * The netherite and enderite backpacks survive fire and explosions as dropped items, like the
     * matching bundles; the basic and reinforced ones do not - and a destroyed backpack spills what it
     * carried instead of taking it along.
     *
     * <ul>
     *   <li><b>Fire:</b> all four drops are set on fire. The lower two burn, the upper two carry the
     *       netherite ingot's {@code DAMAGE_RESISTANT} and stay.</li>
     *   <li><b>Explosion:</b> four drops stand at the same distance around a small blast. The lower two
     *       are destroyed, the upper two ignore it ({@code ItemEntityMixin#ignoreExplosion}).</li>
     *   <li><b>Spill:</b> the basic backpack in the blast held three diamonds; they lie in the room
     *       afterwards.</li>
     * </ul>
     *
     * <p>What breaks this test: dropping {@code fireResistant()} from an upper tier, removing an upper
     * backpack from {@code ignoreExplosion} or adding a lower one, and a destroyed backpack that does not
     * spill its contents.
     */
    public static void upperTiersSurviveFireAndExplosionsAndLowerOnesSpillTheirContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }

        // --- fire ---
        ItemEntity basicInFire = drop(helper, new ItemStack(ModItems.BACKPACK), new Vec3(0.5, 1.2, 0.5));
        ItemEntity reinforcedInFire = drop(helper, new ItemStack(ModItems.REINFORCED_BACKPACK), new Vec3(1.5, 1.2, 0.5));
        ItemEntity netheriteInFire = drop(helper, new ItemStack(ModItems.NETHERITE_BACKPACK), new Vec3(0.5, 1.2, 1.5));
        ItemEntity enderiteInFire = drop(helper, new ItemStack(ModItems.ENDERITE_BACKPACK), new Vec3(1.5, 1.2, 1.5));
        for (ItemEntity entity : List.of(basicInFire, reinforcedInFire, netheriteInFire, enderiteInFire)) {
            entity.hurtServer(level, level.damageSources().onFire(), LETHAL_DAMAGE);
        }
        helper.assertTrue(basicInFire.isRemoved() && reinforcedInFire.isRemoved(),
                "a basic or reinforced backpack survived being set on fire, so the fire cannot tell the tiers apart");
        helper.assertTrue(!netheriteInFire.isRemoved(), "the netherite backpack burned up; it is registered fireResistant()");
        helper.assertTrue(!enderiteInFire.isRemoved(), "the enderite backpack burned up; it is registered fireResistant()");

        // --- explosion ---
        ItemEntity basicInBlast = drop(helper, backpackWith(ModItems.BACKPACK, contentsOf(new ItemStack(Items.DIAMOND, 3))),
                new Vec3(4.8, 1.2, 5.0));
        ItemEntity reinforcedInBlast = drop(helper, new ItemStack(ModItems.REINFORCED_BACKPACK), new Vec3(5.2, 1.2, 5.0));
        ItemEntity netheriteInBlast = drop(helper, new ItemStack(ModItems.NETHERITE_BACKPACK), new Vec3(5.0, 1.2, 5.2));
        ItemEntity enderiteInBlast = drop(helper, new ItemStack(ModItems.ENDERITE_BACKPACK), new Vec3(5.0, 1.2, 4.8));
        Vec3 blast = helper.absoluteVec(new Vec3(5.0, 1.2, 5.0));
        level.explode(null, blast.x, blast.y, blast.z, 2.0F, Level.ExplosionInteraction.NONE);
        helper.assertTrue(basicInBlast.isRemoved() && reinforcedInBlast.isRemoved(),
                "a basic or reinforced backpack survived the blast it stood in, so the blast is too weak to "
                        + "say anything about the upper tiers");
        helper.assertTrue(!netheriteInBlast.isRemoved(),
                "the netherite backpack was destroyed by the explosion it is supposed to ignore");
        helper.assertTrue(!enderiteInBlast.isRemoved(),
                "the enderite backpack was destroyed by the explosion it is supposed to ignore");

        // --- the destroyed basic backpack spilled its diamonds ---
        int diamonds = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (entity.getItem().is(Items.DIAMOND)) {
                diamonds += entity.getItem().getCount();
                entity.discard();
            }
        }
        helper.assertValueEqual(diamonds, 3, "diamonds the destroyed backpack spilled");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS - PLAYERS AND MENUS
    // =====================================================================================

    /** A survival player that exists on both lines and is never put into the player list. */
    private static Player survivalPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 6.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    /**
     * The in-level mock player, for code that insists on a {@code ServerPlayer}. It is creative, but
     * {@code instabuild} is switched off so blocks and materials are really spent. Handed back to the
     * player list when the test ends; a leaked mock player stalls the gametest server's shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 6.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        helper.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }

    /** Right click with the hand's item, applying the transformed hand stack the way the game mode does. */
    private static void rightClick(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        InteractionResult result = held.use(player.level(), player, hand);
        if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null) {
            player.setItemInHand(hand, success.heldItemTransformedTo());
        }
    }

    /** The worn backpack's menu, built through the same provider the backpack key uses. */
    private static BackpackMenu openWorn(ServerPlayer player) {
        return (BackpackMenu) BackpackMenuProviders.worn(player).provider().createMenu(1, player.getInventory(), player);
    }

    /** A worn-mode menu for {@code tier} on an empty container. */
    private static BackpackMenu menuFor(Player player, BackpackTier tier) {
        return new BackpackMenu(1, player.getInventory(), new BackpackContainer(tier, 1), BackpackOpenData.worn(tier, 1));
    }

    /** Menu slots 0 to 45 read what the player's own inventory menu reads there. */
    private static void assertVanillaPart(GameTestHelper helper, Player player, BackpackMenu menu, boolean samePlace) {
        for (int i = 0; i < BackpackMenu.BACKPACK_SLOT_START; i++) {
            Slot ours = menu.getSlot(i);
            Slot vanilla = player.inventoryMenu.getSlot(i);
            if (i >= BackpackMenu.ARMOR_SLOT_START) {
                helper.assertTrue(ours.container == vanilla.container && ours.getContainerSlot() == vanilla.getContainerSlot(),
                        "backpack menu slot " + i + " reads inventory index " + ours.getContainerSlot()
                                + " where the vanilla inventory menu reads " + vanilla.getContainerSlot());
            }
            if (samePlace && (i < BackpackMenu.INV_SLOT_START || i == BackpackMenu.SHIELD_SLOT)) {
                helper.assertTrue(ours.x == vanilla.x && ours.y == vanilla.y,
                        "backpack menu slot " + i + " sits at " + ours.x + "/" + ours.y + " instead of vanilla's "
                                + vanilla.x + "/" + vanilla.y);
            }
        }
    }

    /** Shift-click; the click type is called ContainerInput on this line. */
    private static void shiftClick(AbstractContainerMenu menu, int slot, Player player) {
        menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, player);
    }

    /** Left click (pick up); the click type is called ContainerInput on this line. */
    private static void leftClick(AbstractContainerMenu menu, int slot, Player player) {
        menu.clicked(slot, 0, ContainerInput.PICKUP, player);
    }

    // =====================================================================================
    // HELPERS - STACKS
    // =====================================================================================

    /** Contents with the given stacks in component slots 0, 1, 2, ... */
    private static BackpackContents contentsOf(ItemStack... stacks) {
        List<BackpackContents.Entry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            entries.add(BackpackContents.Entry.of(i, stacks[i]));
        }
        return new BackpackContents(entries);
    }

    private static ItemStack backpackWith(Item item, BackpackContents contents) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponentTypes.BACKPACK_CONTENTS, contents);
        return stack;
    }

    private static int countIn(ItemStack backpack, Item item) {
        int total = 0;
        for (ItemStack stack : BackpackItem.entryStacks(backpack)) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Everything of {@code item} in the player's inventory, hands included. */
    private static int carried(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static ItemEntity drop(GameTestHelper helper, ItemStack stack, Vec3 relative) {
        Vec3 pos = helper.absoluteVec(relative);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(entity);
        helper.runBeforeTestEnd(entity::discard);
        return entity;
    }

    /** A click on the middle of the top face of {@code support}. */
    private static UseOnContext clickTop(GameTestHelper helper, Player player, BlockPos support) {
        BlockPos absolute = helper.absolutePos(support);
        Vec3 hit = new Vec3(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, absolute, false));
    }

    private static void assertKeptNameAndFunnel(GameTestHelper helper, ItemStack stack, String name, String what) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        helper.assertTrue(customName != null && name.equals(customName.getString()),
                what + " lost its name, it is called " + customName);
        helper.assertValueEqual(stack.getEnchantments().getLevel(enchantment(helper, ModEnchantments.FUNNEL)), 1,
                "Funnel level " + what + " kept");
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Crafts through the live recipe manager; the one place a crafting result is assembled. */
    private static ItemStack craft(GameTestHelper helper, ServerLevel level, CraftingInput grid, String what, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(), what + " crafts nothing at all");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched by " + what);
        return match.get().value().assemble(grid);
    }

    /** Smiths through the live recipe manager; the one place a smithing result is assembled. */
    private static ItemStack smith(GameTestHelper helper, ServerLevel level, SmithingRecipeInput input, String what) {
        Optional<RecipeHolder<SmithingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(match.isPresent(), what + " matches no smithing recipe at all");
        return match.get().value().assemble(input);
    }

    // =====================================================================================
    // HELPERS - THE BUILDING WAND
    // =====================================================================================

    /** A diamond wand set to radius 0, so one click builds exactly the block in front of the face. */
    private static ItemStack singleBlockWand() {
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        CompoundTag settings = new CompoundTag();
        settings.putInt("SettingsRadius", 0);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    /** Wand in the selected hotbar slot, the backpack on the chest, nothing else. */
    private static void armWand(ServerPlayer player, ItemStack wand, ItemStack worn) {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.setItemSlot(EquipmentSlot.CHEST, worn);
    }

    private static InteractionResult useWand(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        return wand.getItem().useOn(clickTop(helper, player, WAND_ANCHOR));
    }

    private static void driveUntilIdle(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_TICK_CAP) {
            wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < WAND_TICK_CAP, "the wand was still building after " + WAND_TICK_CAP + " ticks");
    }

    private static boolean wandIsActive(ItemStack wand) {
        return wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("Active", false);
    }

    /** An obsidian anchor with air around it. */
    private static void resetWandSite(GameTestHelper helper) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.setBlock(WAND_ANCHOR.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }
        helper.setBlock(WAND_ANCHOR, Blocks.OBSIDIAN);
    }
}
