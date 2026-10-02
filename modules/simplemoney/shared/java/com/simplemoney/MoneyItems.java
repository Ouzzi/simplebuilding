package com.simplemoney;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import java.util.*;
import java.util.function.Consumer;
public final class MoneyItems {
 public static final List<String> IDS=List.of("special_paper","special_fiber","resin_fiber","blank_note","refined_blank_note","raw_bill","money_bill");
 public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
 /** Creative-tab order = crafting order: paper, the resin fiber that makes it a blank note, the special fiber that refines it, raw bill, bill. */
 public static final List<String> TAB_ORDER=List.of("special_paper","resin_fiber","blank_note","special_fiber","refined_blank_note","raw_bill","money_bill");
 /** The materials also stand right after paper in the vanilla Ingredients tab, so the search tab lists them there, not at its end. */
 public static final Item SEARCH_ANCHOR=Items.PAPER;
 public static List<ItemStack> tabStacks(){return TAB_ORDER.stream().map(n->new ItemStack(ITEMS.get(n))).toList();}
 public static void register() {
  for(String name:IDS) {
   var id=Identifier.fromNamespaceAndPath("simplemoney",name);
   var props=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(name.endsWith("fiber")?16:64);
   props.rarity(name.equals("money_bill")?Rarity.EPIC:name.equals("raw_bill")?Rarity.RARE:name.endsWith("note")?Rarity.UNCOMMON:Rarity.COMMON);
   if(name.equals("money_bill")) props.fireResistant();
   ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,id,new Item(props) {
    @Override public boolean isFoil(ItemStack stack) { return name.equals("money_bill") || super.isFoil(stack); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> consumer,TooltipFlag flag) {
     consumer.accept(Component.translatable("tooltip.simplemoney."+name+".tooltip"));
     if(name.equals("special_fiber")) consumer.accept(Component.translatable("tooltip.simplemoney.special_fiber.tooltip.2"));
     if(name.equals("money_bill")) {
      consumer.accept(Component.translatable("tooltip.simplemoney.money_bill.tooltip.2"));
      consumer.accept(Component.translatable("tooltip.simplemoney.money_bill.tooltip.3"));
      consumer.accept(Component.translatable("tooltip.simplemoney.money_bill.tooltip.4"));
      consumer.accept(Component.translatable("tooltip.simplemoney.money_bill.tooltip.5"));
      consumer.accept(Component.translatable("tooltip.simplemoney.money_bill.tooltip.6"));
      consumer.accept(Component.translatable("tooltip.simplemoney.money_bill.tooltip.7"));
     }
     super.appendHoverText(stack,context,display,consumer,flag);
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
     if(!name.equals("money_bill")) return super.use(level,player,hand);
     if(!level.isClientSide()) level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.BOOK_PAGE_TURN,SoundSource.PLAYERS,0.6f,1.8f);
     if(level instanceof net.minecraft.server.level.ServerLevel server) server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,player.getX(),player.getY()+1,player.getZ(),1,0,0,0,0.1);
     return InteractionResult.SUCCESS;
    }
   }));
  }
 }
 public static void registerTab(CreativeModeTab.Builder builder) {
  Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,Identifier.fromNamespaceAndPath("simplemoney","money_items"),builder.icon(()->new ItemStack(ITEMS.get("money_bill"))).title(Component.translatable("itemgroup.simplemoney.money_items")).displayItems((p,o)->tabStacks().forEach(o::accept)).build());
 }
}
