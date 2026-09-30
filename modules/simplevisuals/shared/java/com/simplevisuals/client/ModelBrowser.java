package com.simplevisuals.client;
import net.minecraft.client.gui.screens.Screen;import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;import net.minecraft.world.item.Item;import net.minecraft.core.component.DataComponents;
import java.util.*;
/** Bounded pages, five-column rows; preview instructions never send a custom gameplay packet. */
public final class ModelBrowser extends Screen {
 private final Screen parent;private final Item item;private EditBox search;private final List<Button> grid=new ArrayList<>();private final List<RenamedModels.Entry> shown=new ArrayList<>();private int page;private RenamedModels.Entry selected;
 public ModelBrowser(Screen parent,Item item){super(Component.translatable("simplevisuals.models"));this.parent=parent;this.item=item;}
 protected void init(){
  search=addRenderableWidget(new EditBox(font,width/2-100,32,200,20,Component.translatable("simplevisuals.search")));search.setMaxLength(64);search.setResponder(s->{page=0;refresh();});
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-45,height-24,90,20).build());
  addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);refresh();}).bounds(width/2-100,height-24,40,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;refresh();}).bounds(width/2+60,height-24,40,20).build());refresh();
 }
 private void refresh(){for(var b:grid)removeWidget(b);grid.clear();shown.clear();String query=search.getValue().toLowerCase(Locale.ROOT);var matches=RenamedModels.entries(item).stream().filter(e->e.name().toLowerCase(Locale.ROOT).contains(query)||e.tags().stream().anyMatch(t->t.toLowerCase(Locale.ROOT).contains(query))).toList();page=Math.min(page,Math.max(0,(matches.size()-1)/15));
  for(int i=page*15;i<Math.min(matches.size(),page*15+15);i++){var entry=matches.get(i);int j=i-page*15;var b=Button.builder(Component.empty(),button->selected=entry).bounds(width/2-90+(j%5)*36,62+(j/5)*36,34,34).tooltip(Tooltip.create(Component.literal(entry.name()+" ["+entry.id()+"] "+String.join(", ",entry.tags())))).build();addRenderableWidget(b);grid.add(b);shown.add(entry);}
 }
 public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);g.text(font,title,(width-font.width(title))/2,15,0xffffffff);
  g.nextStratum();for(int i=0;i<shown.size();i++){var stack=new net.minecraft.world.item.ItemStack(item);stack.set(DataComponents.ITEM_MODEL,shown.get(i).renderedModel());g.item(stack,grid.get(i).getX()+9,grid.get(i).getY()+9);}
  if(selected!=null){var stack=new net.minecraft.world.item.ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(selected.name()));g.item(stack,width/2-8,height-70);var text=Component.translatable("simplevisuals.rename_to",selected.name());g.text(font,text,(width-font.width(text))/2,height-48,0xffffff00);}
 }
 public void onClose(){minecraft.setScreenAndShow(parent);}
}
