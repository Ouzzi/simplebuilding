package com.simplemoney.testing;
import com.simplemoney.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import java.util.*;
import java.io.*;
public final class MoneyTests {
 public static final Map<String,java.util.function.Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("money_game_test_launch_smoke",MoneyTests::launchSmoke);
  ALL.put("money_game_test_bill_use",MoneyTests::billUse);
  ALL.put("money_game_test_creative_tabs",MoneyTests::creativeTabs);
  ALL.put("money_game_test_recipes",MoneyTests::recipes);
  ALL.put("money_game_test_recipe_outputs",MoneyTests::recipeOutputs);
  ALL.put("money_game_test_config_world",MoneyTests::configWorld);
  ALL.put("money_game_test_trades",MoneyTests::trades);
  ALL.put("money_game_test_loot",MoneyTests::loot);
  ALL.put("money_game_test_config",MoneyTests::config);
  ALL.put("money_game_test_languages_and_assets",MoneyTests::languagesAndAssets);
  ALL.put("money_game_test_simplebuilding_storage",MoneyTests::storage);
  ALL.put("money_game_test_link_conditions",LinkTests::conditions);
  ALL.put("money_game_test_link_offers",LinkTests::offers);
  ALL.put("money_game_test_link_bounds",LinkTests::bounds);
  ALL.put("money_game_test_link_rarity",LinkTests::rarity);
  ALL.put("money_game_test_link_budgets",LinkTests::budgets);
  ALL.put("money_game_test_link_menu",LinkTests::menu);
  ALL.put("money_game_test_link_no_arbitrage",LinkTests::noArbitrage);
 }
 private static Identifier id(String path) {return Identifier.fromNamespaceAndPath("simplemoney",path);}
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 public static void launchSmoke(GameTestHelper h) {
  if(isModLoaded("simplebuilding"))h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:iron_chisel")),"SimpleBuilding content visible when loaded");
  h.assertValueEqual(MoneyItems.ITEMS.size(),7,"all original items registered");
  for(String name:MoneyItems.IDS) {
   var item=BuiltInRegistries.ITEM.getValue(id(name));h.assertTrue(item!=Items.AIR,"registered "+name);
   h.assertValueEqual(item.getDefaultMaxStackSize(),name.endsWith("fiber")?16:64,"stack size "+name);
   h.assertValueEqual(item.isFoil(item.getDefaultInstance()),name.equals("money_bill"),"glint "+name);
  }
  h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id("money_items")),"original creative tab");
  h.assertTrue(MoneyItems.ITEMS.get("money_bill").getDefaultInstance().has(DataComponents.DAMAGE_RESISTANT),"bill fire resistance");h.succeed();
 }
 /** Own tab in crafting order; in vanilla Ingredients (and so in the search tab) the same run right after paper. */
 public static void creativeTabs(GameTestHelper h) {
  h.assertTrue(new HashSet<>(MoneyItems.TAB_ORDER).equals(new HashSet<>(MoneyItems.IDS))&&MoneyItems.TAB_ORDER.size()==MoneyItems.IDS.size(),"tab order lists every item once");
  CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess());
  var expected=MoneyItems.TAB_ORDER.stream().map(MoneyItems.ITEMS::get).toList();
  var own=BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ResourceKey.create(Registries.CREATIVE_MODE_TAB,id("money_items"))).getDisplayItems().stream().map(ItemStack::getItem).toList();
  h.assertValueEqual(own,expected,"own tab in crafting order");
  for(var tab:List.of(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INGREDIENTS).getDisplayItems(),CreativeModeTabs.searchTab().getDisplayItems())) {
   var items=new ArrayList<ItemStack>(tab).stream().map(ItemStack::getItem).toList();
   int at=items.indexOf(MoneyItems.SEARCH_ANCHOR);
   h.assertTrue(at>=0&&at+expected.size()<items.size()&&items.subList(at+1,at+1+expected.size()).equals(expected),"money items follow paper: "+(at<0?"no paper":items.subList(at+1,Math.min(items.size(),at+1+expected.size()))));
   h.assertValueEqual(items.stream().filter(MoneyItems.ITEMS.values()::contains).count(),(long)expected.size(),"each money item once");
  }
  h.succeed();
 }
 public static void billUse(GameTestHelper h) {
  var player=h.makeMockServerPlayerInLevel();var item=MoneyItems.ITEMS.get("money_bill");var stack=new ItemStack(item,3);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
  var result=item.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(result==net.minecraft.world.InteractionResult.SUCCESS,"bill use succeeds");h.assertValueEqual(player.getMainHandItem().getCount(),3,"using money does not spend it");h.succeed();
 }
 public static void recipes(GameTestHelper h) {
  for(String name:List.of("special_paper_from_crafting_table","special_fiber_from_crafting_table","resin_fiber_from_crafting_table","blank_note_smithing","refined_bank_note_blank_smithing","raw_bill_from_crafting_table","money_bill_from_blasting","rocket_from_paper")) {
   var recipe=h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,id(name)));
   h.assertTrue(recipe.isPresent(),"loaded recipe "+name);
   if(name.equals("money_bill_from_blasting")) {var cooking=(AbstractCookingRecipe)recipe.get().value();h.assertValueEqual(cooking.cookingTime(),24000,"curing time: one day in a vanilla blast furnace");h.assertValueEqual(cooking.experience(),20f,"source curing xp");}
  }h.succeed();
 }
 private static ItemStack stack(String name) {return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(name)));}
 public static void recipeOutputs(GameTestHelper h) {
  String[][] rows={
   {"special_paper_from_crafting_table","minecraft:paper,minecraft:honeycomb,minecraft:paper","simplemoney:special_paper","1","3","1"},
   {"special_fiber_from_crafting_table","minecraft:copper_nugget,minecraft:gold_nugget,minecraft:amethyst_shard,minecraft:diamond,minecraft:amethyst_shard,minecraft:gold_nugget,minecraft:gold_nugget,minecraft:diamond,minecraft:copper_nugget","simplemoney:special_fiber","1","3","3"},
   {"resin_fiber_from_crafting_table","minecraft:iron_nugget,minecraft:resin_clump,minecraft:honeycomb,minecraft:resin_clump,minecraft:bone_meal,minecraft:resin_clump,minecraft:honeycomb,minecraft:resin_clump,minecraft:iron_nugget","simplemoney:resin_fiber","1","3","3"},
   {"raw_bill_from_crafting_table","minecraft:green_dye,minecraft:ink_sac,minecraft:green_dye,simplemoney:refined_blank_note,simplemoney:refined_blank_note,simplemoney:refined_blank_note,minecraft:ink_sac,minecraft:green_dye,minecraft:ink_sac","simplemoney:raw_bill","3","3","3"},
   {"rocket_from_paper","minecraft:paper,minecraft:paper,minecraft:paper,minecraft:paper,minecraft:gunpowder,minecraft:paper,minecraft:paper,minecraft:gunpowder,minecraft:paper","minecraft:firework_rocket","1","3","3"}
  };
  for(var row:rows) {
   var stacks=new ArrayList<ItemStack>();for(String name:row[1].split(","))stacks.add(stack(name));
   var grid=CraftingInput.of(Integer.parseInt(row[4]),Integer.parseInt(row[5]),stacks);
   var match=h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel());
   h.assertTrue(match.isPresent(),"source ingredients craft "+row[0]);h.assertValueEqual(match.get().id().identifier(),id(row[0]),"correct recipe");
   var result=match.get().value().assemble(grid);h.assertTrue(result.is(stack(row[2]).getItem()),"output item "+row[0]);h.assertValueEqual(result.getCount(),Integer.parseInt(row[3]),"output amount "+row[0]);
   stacks.set(0,ItemStack.EMPTY);h.assertTrue(!match.get().value().matches(CraftingInput.of(Integer.parseInt(row[4]),Integer.parseInt(row[5]),stacks),h.getLevel()),"missing ingredient rejected "+row[0]);
  }
  for(var row:new String[][]{{"minecraft:iron_ingot","simplemoney:special_paper","simplemoney:resin_fiber","simplemoney:blank_note"},{"minecraft:gold_ingot","simplemoney:blank_note","simplemoney:special_fiber","simplemoney:refined_blank_note"}}) {
   var input=new SmithingRecipeInput(stack(row[0]),stack(row[1]),stack(row[2]));var match=h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,input,h.getLevel());h.assertTrue(match.isPresent(),"source smithing ingredients");h.assertTrue(match.get().value().assemble(input).is(stack(row[3]).getItem()),"source smithing output");h.assertTrue(!match.get().value().matches(new SmithingRecipeInput(ItemStack.EMPTY,input.base(),input.addition()),h.getLevel()),"smithing needs template");
  }
  var match=h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.BLASTING,new SingleRecipeInput(stack("simplemoney:raw_bill")),h.getLevel());h.assertTrue(match.isPresent()&&match.get().value().assemble(new SingleRecipeInput(stack("simplemoney:raw_bill"))).is(stack("simplemoney:money_bill").getItem()),"curing output");h.succeed();
 }
 public static void configWorld(GameTestHelper h) {
  var registry=h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
  for(var entry:sourceTrades()) {var row=entry.getAsJsonObject();String name=row.get("id").getAsString();boolean expected=SimpleMoney.enabled(name.contains("wandering_trader")?"enableWanderingTrades":"enableVillagerTrades");h.assertValueEqual(registry.containsKey(Identifier.parse(name)),expected,"loader obeyed server config for "+name);}
  if(isModLoaded("simplebuilding"))h.assertTrue(registry.containsKey(Identifier.parse("simplebuilding:librarian/3/emerald_building_book")),"SimpleBuilding trades survive Money config");h.succeed();
 }
 private static JsonArray sourceTrades() {
  try(var reader=new InputStreamReader(Objects.requireNonNull(MoneyTests.class.getResourceAsStream("/data/simplemoney/testing/source-trades.json")),java.nio.charset.StandardCharsets.UTF_8)) {return JsonParser.parseReader(reader).getAsJsonArray();}catch(IOException e){throw new IllegalStateException(e);}
 }
 public static void trades(GameTestHelper h) {
  var level=h.getLevel();var villager=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);
  var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,villager.position()).withParameter(LootContextParams.THIS_ENTITY,villager).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,Unit.INSTANCE).create(LootContextParamSets.VILLAGER_TRADE);
  var context=new LootContext.Builder(params).withOptionalRandomSeed(314159L).create(Optional.empty());
  var registry=level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
  int count=0;
  for(var entry:sourceTrades()) {
   var row=entry.getAsJsonObject();var name=row.get("id").getAsString();var trade=registry.getValue(Identifier.parse(name));h.assertTrue(trade!=null,"registered trade "+name);count++;
   for(int roll=0;roll<128;roll++) {
    var offer=trade.getOffer(context);h.assertTrue(offer!=null,"offer "+name);
    var wants=row.getAsJsonObject("wants");var gives=row.getAsJsonObject("gives");
    h.assertTrue(offer.getBaseCostA().is(BuiltInRegistries.ITEM.getValue(Identifier.parse(wants.get("id").getAsString()))),"currency "+name);
    var cost=wants.get("count");int min=cost.isJsonObject()?cost.getAsJsonObject().get("min").getAsInt():cost.getAsInt();int max=cost.isJsonObject()?cost.getAsJsonObject().get("max").getAsInt():min;
    h.assertTrue(offer.getBaseCostA().getCount()>=min && offer.getBaseCostA().getCount()<=max,"price range "+name);
    h.assertTrue(offer.getResult().is(BuiltInRegistries.ITEM.getValue(Identifier.parse(gives.get("id").getAsString()))),"result "+name);
    int giveMin=gives.has("count")?gives.get("count").getAsInt():1;int giveMax=giveMin;
    if(row.has("given_item_modifier"))for(var modifier:row.getAsJsonArray("given_item_modifier")){var mod=modifier.getAsJsonObject();if(mod.get("type").getAsString().equals("minecraft:set_count")){giveMin=mod.getAsJsonObject("count").get("min").getAsInt();giveMax=mod.getAsJsonObject("count").get("max").getAsInt();}}
    h.assertTrue(offer.getResult().getCount()>=giveMin&&offer.getResult().getCount()<=giveMax,"source result count "+name);
    h.assertValueEqual(offer.getMaxUses(),row.get("max_uses").getAsInt(),"uses "+name);h.assertValueEqual(offer.getXp(),row.get("xp").getAsInt(),"xp "+name);h.assertValueEqual(offer.getPriceMultiplier(),row.get("reputation_discount").getAsFloat(),"discount "+name);
    if(row.has("given_item_modifier")) for(var modifier:row.getAsJsonArray("given_item_modifier")) {
     var mod=modifier.getAsJsonObject();if(mod.get("type").getAsString().equals("simplemoney:weighted_enchant")) {
      var ench=offer.getResult().getOrDefault(offer.getResult().is(Items.ENCHANTED_BOOK)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
      h.assertTrue(!ench.isEmpty(),"weighted enchantment applied "+name);
      for(var en:ench.entrySet()) {boolean found=false;for(var choice:mod.getAsJsonArray("pool")){var c=choice.getAsJsonObject();if(en.getKey().unwrapKey().orElseThrow().identifier().toString().equals(c.get("enchantment").getAsString()) && en.getIntValue()==c.get("level").getAsInt()) found=true;}h.assertTrue(found,"enchantment belongs to source pool "+name);}
     }
    }
   }
  }h.assertValueEqual(count,47,"source offer count");h.succeed();
 }
 public static void loot(GameTestHelper h) {
  var level=h.getLevel();
  for(var entry:MoneyLoot.ENTRIES) {
   var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:chests/"+entry.table()));var table=level.getServer().reloadableRegistries().getLootTable(key);
   int found=0;var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(1,1,1)))).create(LootContextParamSets.CHEST);
   for(int roll=0;roll<256;roll++) for(var stack:table.getRandomItems(params,roll+100)) if(stack.is(MoneyItems.ITEMS.get("money_bill"))) {found++;h.assertTrue(stack.getCount()>=entry.min()&&stack.getCount()<=entry.max(),"loot amount "+entry.table());}
   h.assertTrue(found>0,"injected currency "+entry.table());
  }h.succeed();
 }
 public static void config(GameTestHelper h) {
  h.assertTrue(new SimpleMoney.Config().trades.enableVillagerTrades && new SimpleMoney.Config().trades.enableWanderingTrades,"source defaults");
  var original=SimpleMoney.config;try {SimpleMoney.config=new SimpleMoney.Config();SimpleMoney.config.trades.enableVillagerTrades=false;h.assertTrue(!SimpleMoney.enabled("enableVillagerTrades") && SimpleMoney.enabled("enableWanderingTrades"),"independent villager switch");SimpleMoney.config.trades.enableWanderingTrades=false;h.assertTrue(!SimpleMoney.enabled("enableWanderingTrades"),"wandering switch");h.assertTrue(!SimpleMoney.enabled("unknown"),"unknown flag denied");}finally{SimpleMoney.config=original;}h.succeed();
 }
 public static void languagesAndAssets(GameTestHelper h) {
  try {
   JsonObject en, de;
   try(var r=new InputStreamReader(MoneyTests.class.getResourceAsStream("/assets/simplemoney/lang/en_us.json"),java.nio.charset.StandardCharsets.UTF_8)){en=JsonParser.parseReader(r).getAsJsonObject();}
   try(var r=new InputStreamReader(MoneyTests.class.getResourceAsStream("/assets/simplemoney/lang/de_de.json"),java.nio.charset.StandardCharsets.UTF_8)){de=JsonParser.parseReader(r).getAsJsonObject();}
   h.assertValueEqual(en.keySet(),de.keySet(),"bilingual completeness");
   for(String name:MoneyItems.IDS) {
    h.assertTrue(en.has("item.simplemoney."+name)&&en.has("tooltip.simplemoney."+name+".tooltip"),"name and tooltip "+name);
    var stack=new ItemStack(MoneyItems.ITEMS.get(name));
    var lines=new ArrayList<net.minecraft.network.chat.Component>();
    stack.getItem().appendHoverText(stack,Item.TooltipContext.of(h.getLevel()),net.minecraft.world.item.component.TooltipDisplay.DEFAULT,lines::add,TooltipFlag.NORMAL);
    var keys=lines.stream().map(net.minecraft.network.chat.Component::getContents)
      .filter(c->c instanceof net.minecraft.network.chat.contents.TranslatableContents)
      .map(c->((net.minecraft.network.chat.contents.TranslatableContents)c).getKey())
      .filter(k->k.startsWith("tooltip.simplemoney.")).toList();
    h.assertValueEqual(keys.size(),name.equals("money_bill")?7:name.equals("special_fiber")?2:1,"complete tooltip lines: "+name);
    for(String key:keys)for(var lang:List.of(en,de))h.assertTrue(lang.has(key)&&lang.get(key).getAsString().length()<=48,"short translated tooltip line: "+key);
    for(String path:List.of("items/"+name+".json","models/item/"+name+".json","textures/item/"+name+".png"))h.assertTrue(MoneyTests.class.getResource("/assets/simplemoney/"+path)!=null,"asset "+path);
   }
   for(String flag:List.of("enableVillagerTrades","enableWanderingTrades"))for(var lang:List.of(en,de)) {h.assertTrue(lang.has("text.autoconfig.simplemoney.category.default"),"config tab");var prefix="text.autoconfig.simplemoney.option.trades."+flag;h.assertTrue(lang.has(prefix)&&lang.get(prefix+".tooltip").getAsString().contains("true"),"config name and default tooltip "+flag);}
  }catch(IOException e){throw new IllegalStateException(e);}h.succeed();
 }
 public static void storage(GameTestHelper h) {
  if(partnerMissing(h,"simplebuilding","SimpleBuilding storage round trip"))return;
  var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:reinforced_hopper"));var pos=new BlockPos(2,2,2);h.setBlock(pos,block);var container=(Container)h.getLevel().getBlockEntity(h.absolutePos(pos));
  for(var item:MoneyItems.ITEMS.values()) {container.setItem(0,new ItemStack(item,3));h.assertTrue(container.removeItem(0,2).is(item)&&container.getItem(0).getCount()==1,"foreign item round trip");}h.succeed();
 }
}
