package com.simplebuilding.modules.simpledimensions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
public final class ModuleClientSmoke implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context){
  context.runOnClient(client->{if(!(client.gui.screen() instanceof TitleScreen)||!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpledimension"))throw new AssertionError("Module title boot");});
  context.takeScreenshot("simpledimensions-title");
  var language=context.computeOnClient(client->client.getLanguageManager().getSelected());
  var optionLanguage=context.computeOnClient(client->client.options.languageCode);
  // This instance is isolated from normal clients. The world builder creates a disposable save.
  var settingsFile=context.computeOnClient(client->{
   try{
   var instance=client.gameDirectory.toPath().toRealPath();
   require(instance.endsWith(Path.of("integration/run-fabric-263")),"Use only the integration test instance");
   var root=configRoot().toAbsolutePath().normalize();
   require(root.equals(instance.resolve("config/simpledimension")),"Settings must belong to the test instance");
   require(!Files.isSymbolicLink(root.getParent())&&!Files.isSymbolicLink(root),"Test settings directory must not redirect elsewhere");
   require(!Files.exists(root)||root.toRealPath().equals(root),"Test settings directory must remain inside the instance");
   require(!Files.isSymbolicLink(root.resolve("server.json")),"Test settings must not be a symlink");
   return root.resolve("server.json");
   }catch(IOException e){throw new UncheckedIOException(e);}
  });
  try(var configBackup=FileBackup.capture(settingsFile)){
  try(var world=context.worldBuilder().create()){
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
   world.getServer().runOnServer(server->{
    for(String name:java.util.List.of("skyblock","mining","travel"))if(server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.fromNamespaceAndPath("simpledimension",name)))==null)throw new AssertionError("Normal world dimension "+name);
    var p=server.getPlayerList().getPlayers().getFirst();var l=p.level();var base=p.blockPosition().offset(-2,0,-5);
    for(int x=0;x<4;x++)for(int y=0;y<4;y++){
     var q=base.offset(x,y,0);boolean inside=x>0&&x<3&&y<3;
     l.setBlock(q,inside?net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("simpledimension:sky_portal")).defaultBlockState():((y<3||x==1||x==2)?Blocks.GLOWSTONE:Blocks.AIR).defaultBlockState(),3);
     if(inside&&l.getBlockEntity(q)!=null){try{var b=l.getBlockEntity(q);b.getClass().getMethod("configure",int.class,String.class).invoke(b,0x66D9FF,"simpledimension:skyblock");}catch(Exception e){throw new AssertionError(e);}l.sendBlockUpdated(q,l.getBlockState(q),l.getBlockState(q),3);}
    }
    l.setBlock(base.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);
    p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,net.minecraft.world.phys.Vec3.atCenterOf(base.offset(2,1,0)));
   });
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();context.waitTicks(20);
   context.runOnClient(client->{if(client.level==null||client.player==null)throw new AssertionError("Title to world");});
   context.takeScreenshot("simpledimensions-portal");
   var server=world.getServer();
   var original=server.computeOnServer(s->copy(runtimeSettings(s)));
   try{
    verifyDimensions(context,server,"en_us");
    verifyDimensions(context,server,"de_de");
   }finally{
    try{context.runOnClient(client->client.setScreenAndShow(null));}
    finally{server.runOnServer(s->{
     restoreSettings(runtimeSettings(s),original);
     assertSettings(runtimeSettings(s),original,"Runtime restoration");
    });}
   }
  }
  }finally{
   try{setLanguage(context,language);}
   finally{context.runOnClient(client->client.options.languageCode=optionLanguage);}
  }
 }

 private static void verifyDimensions(ClientGameTestContext context,TestServerContext server,String language){
  setLanguage(context,language);
  var baseline=loadSettings();
  var defaults=defaultSettings();
  for(String field:List.of("skyblockEnabled","miningEnabled","travelEnabled")){
   require(Boolean.TRUE.equals(publicField(defaults,field)),field+" must default on");
   setSetting(baseline,field,true);
  }
  call(baseline,"save",Path.class,configRoot());
  server.runOnServer(s->restoreSettings(runtimeSettings(s),baseline));
  boolean german=language.equals("de_de");
  String tab=german?"Dimensionen":"Dimensions";
  List<String> labels=german?List.of("Skyblock erlauben","Abbau erlauben","Reise erlauben")
    :List.of("Enable Skyblock","Enable Mining","Enable Travel");
  openConfig(context);
  context.runOnClient(client->{
   require(client.hasSingleplayerServer(),"Settings test requires its integrated server");
   require(client.gui.screen().getTitle().getString().equals(german?"Simple Dimensions: Servereinstellungen":"Simple Dimensions: Server Settings"),"Localized settings title");
   require(((Component)call(client.gui.screen(),"getSelectedCategory")).getString().equals(german?"Zugang":"Access"),"Initial Access category");
  });
  if(!german)context.takeScreenshot("simpledimensions-config");
  selectDimensions(context,tab);
  context.runOnClient(client->assertRows(client,tab,labels,true));
  if(german)context.takeScreenshot("simpledimensions-dimensions-de");
  else context.takeScreenshot("simpledimensions-dimensions-en");

  // Resolve the Mining toggle from its row, never from a screen coordinate or field mutation.
  clickWidget(context,client->{
   var entry=dimensionRows(client.gui.screen(),tab).get(1);
   return button((ContainerEventHandler)entry,yesNoText(entry,true));
  });
  context.runOnClient(client->assertRows(client,tab,labels,false));
  assertSettings(loadSettings(),baseline,"Unsaved draft must not reach disk");
  server.runOnServer(s->assertSettings(runtimeSettings(s),baseline,"Unsaved draft must not reach server"));
  clickWidget(context,client->button(client.gui.screen(),Component.translatable("text.cloth-config.save_and_done").getString()));
  context.waitFor(client->client.gui.screen()==null);
  var expected=copy(baseline);setSetting(expected,"miningEnabled",false);
  assertSettings(loadSettings(),expected,"Saved settings file");
  server.waitFor(s->Boolean.FALSE.equals(publicField(runtimeSettings(s),"miningEnabled")));
  server.runOnServer(s->assertSettings(runtimeSettings(s),expected,"Saved integrated-server settings"));
  openConfig(context);
  selectDimensions(context,tab);
  context.runOnClient(client->assertRows(client,tab,labels,false));
  if(german)context.takeScreenshot("simpledimensions-dimensions-saved-de");
  else context.takeScreenshot("simpledimensions-dimensions-saved-en");
  context.runOnClient(client->client.setScreenAndShow(null));
 }

 private static void openConfig(ClientGameTestContext context){
  context.runOnClient(client->{
   try{
    var type=Class.forName("com.simplebuilding.modules.simpledimensions.client.DimensionConfigScreen");
    client.setScreenAndShow((Screen)type.getMethod("create",Screen.class).invoke(null,(Screen)null));
   }catch(ReflectiveOperationException e){throw new AssertionError("Config opens",e);}
  });
  context.waitTicks(5);
 }

 private static void selectDimensions(ClientGameTestContext context,String label){
  // Cloth tabs are public children, but are not in Fabric clickScreenButton's renderables list.
  clickWidget(context,client->button(client.gui.screen(),label));
  context.waitFor(client->((Component)call(client.gui.screen(),"getSelectedCategory")).getString().equals(label));
 }

 private static void assertRows(Minecraft client,String tab,List<String> labels,boolean mining){
  var screen=client.gui.screen();
  require(((Component)call(screen,"getSelectedCategory")).getString().equals(tab),"Dimensions tab must be selected");
  var entries=dimensionRows(screen,tab);
  require(entries.size()==3,"Exactly three Dimensions settings, got "+entries.size());
  var list=(ContainerEventHandler)publicField(screen,"listWidget");
  var visible=(List<?>)call(list,"visibleChildren");
  var booleanType=booleanEntryType();
  require(visible.containsAll(entries),"All Dimensions rows must be visible");
  require(visible.stream().filter(booleanType::isInstance).toList().equals(entries),"Rendered toggles must exactly match the Dimensions rows");
  int lastBottom=-1;
  for(int i=0;i<entries.size();i++){
   var entry=entries.get(i);
   String label=labels.get(i);
   require(booleanType.isInstance(entry),label+" must be a boolean toggle");
   require(((Component)call(entry,"getFieldName")).getString().equals(label),"Localized row "+i+": "+label);
   require(call(entry,"getDefaultValue").equals(Optional.of(true)),label+" reset default must be on");
   require(call(entry,"getValue").equals(i!=1||mining),label+" value");
   require(Boolean.TRUE.equals(call(entry,"isEditable")),label+" must be editable");
   var toggle=button((ContainerEventHandler)entry,yesNoText(entry,i!=1||mining));
   require(toggle.active&&toggle.visible&&list.getRectangle().encompasses(toggle.getRectangle()),label+" toggle must fit inside the list");
   require(toggle.getY()>=lastBottom,"Dimension toggles must not overlap");
   lastBottom=toggle.getY()+toggle.getHeight();
   require(client.font.width(label)<toggle.getX()-list.getRectangle().left(),label+" must fit before its toggle");
  }
 }

 private static final String BOOLEAN_ENTRY="me.shedaniel.clothconfig2.gui.entries.BooleanListEntry";

 private static Class<?> booleanEntryType(){
  try{return Class.forName(BOOLEAN_ENTRY);}
  catch(ClassNotFoundException e){throw new AssertionError("Cloth boolean entry API",e);}
 }
 private static String yesNoText(Object entry,boolean value){
  // Cloth's builder returns a non-public subclass; invoke its public base API.
  try{return ((Component)booleanEntryType().getMethod("getYesNoText",boolean.class).invoke(entry,value)).getString();}
  catch(ReflectiveOperationException e){throw new AssertionError("Cloth localized toggle text",e);}
 }

 private static List<?> dimensionRows(Screen screen,String label){
  var categories=(Map<?,?>)call(screen,"getCategorizedEntries");
  var matches=categories.entrySet().stream().filter(e->((Component)e.getKey()).getString().equals(label)).toList();
  require(matches.size()==1,"Exactly one localized Dimensions category: "+label);
  return (List<?>)matches.getFirst().getValue();
 }

 private static AbstractWidget button(ContainerEventHandler parent,String label){
  var matches=parent.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
    .filter(widget->widget.getMessage().getString().equals(label)).toList();
  require(matches.size()==1,"Exactly one widget labeled '"+label+"', got "+matches.size());
  return matches.getFirst();
 }

 private static void clickWidget(ClientGameTestContext context,Function<Minecraft,AbstractWidget> find){
  var point=context.computeOnClient(client->{
   var widget=find.apply(client);var screen=client.gui.screen();var window=client.getWindow();
   require(widget.active&&widget.visible,"Clicked widget must be enabled and visible");
   require(screen.getRectangle().encompasses(widget.getRectangle()),"Clicked widget must fit on screen");
   double x=widget.getX()+widget.getWidth()/2.0,y=widget.getY()+widget.getHeight()/2.0;
   require(widget.isMouseOver(x,y),"Widget center must be clickable");
   return new double[]{x*window.getScreenWidth()/window.getGuiScaledWidth(),y*window.getScreenHeight()/window.getGuiScaledHeight()};
  });
  context.getInput().setCursorPos(point[0],point[1]);
  context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
  context.waitTicks(5);
 }

 private static void setLanguage(ClientGameTestContext context,String language){
  var reload=context.computeOnClient(client->{
   client.setScreenAndShow(null);
   if(client.getLanguageManager().getSelected().equals(language))return java.util.concurrent.CompletableFuture.completedFuture(null);
   client.getLanguageManager().setSelected(language);client.options.languageCode=language;
   return client.reloadResourcePacks();
  });
  context.waitFor(client->reload.isDone());reload.join();
  context.waitFor(client->client.gui.overlay()==null);
 }

 // These implementation classes are runtime jars, not on the smoke compile classpath.
 // Use only public APIs/fields; no accessibility overrides or new build dependencies.
 private static Object call(Object target,String method){
  try{return target.getClass().getMethod(method).invoke(target);}
  catch(ReflectiveOperationException e){throw new AssertionError("Public UI API: "+method,e);}
 }
 private static Object call(Object target,String method,Class<?> argumentType,Object argument){
  try{return (target instanceof Class<?> type?type:target.getClass()).getMethod(method,argumentType).invoke(target,argument);}
  catch(ReflectiveOperationException e){throw new AssertionError("Public UI API: "+method,e);}
 }
 private static Object publicField(Object target,String name){
  try{return (target instanceof Class<?> type?type:target.getClass()).getField(name).get(target);}
  catch(ReflectiveOperationException e){throw new AssertionError("Public UI field: "+name,e);}
 }

 private static Class<?> moduleType(String name){
  try{return Class.forName("com.simplebuilding.modules.simpledimensions."+name);}
  catch(ClassNotFoundException e){throw new AssertionError("Module runtime class: "+name,e);}
 }
 private static Path configRoot(){return (Path)publicField(moduleType("DimensionRuntime"),"CONFIG_ROOT");}
 private static Object runtimeSettings(MinecraftServer server){
  return publicField(call(moduleType("DimensionRuntime"),"get",MinecraftServer.class,server),"settings");
 }
 private static Object loadSettings(){return call(moduleType("DimensionSettings"),"load",Path.class,configRoot());}
 private static Object defaultSettings(){
  try{return moduleType("DimensionSettings").getConstructor().newInstance();}
  catch(ReflectiveOperationException e){throw new AssertionError("Settings defaults",e);}
 }
 private static Object copy(Object settings){
  var gson=new Gson();return gson.fromJson(gson.toJson(settings),settings.getClass());
 }
 private static void setSetting(Object settings,String name,boolean value){
  try{settings.getClass().getField(name).setBoolean(settings,value);}
  catch(ReflectiveOperationException e){throw new AssertionError("Test fixture setting: "+name,e);}
 }
 private static void restoreSettings(Object target,Object source){
  try{
   for(var field:target.getClass().getFields())
    if(!java.lang.reflect.Modifier.isStatic(field.getModifiers()))field.set(target,field.get(source));
  }catch(ReflectiveOperationException e){throw new AssertionError("Restore all runtime settings",e);}
 }
 private static void assertSettings(Object actual,Object expected,String message){
  var gson=new Gson();require(gson.toJsonTree(actual).equals(gson.toJsonTree(expected)),message);
 }
 private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}

 private record FileBackup(Path file,byte[] contents) implements AutoCloseable{
  static FileBackup capture(Path file){
   try{return new FileBackup(file,Files.exists(file)?Files.readAllBytes(file):null);}
   catch(IOException e){throw new UncheckedIOException(e);}
  }
  public void close(){
   try{
    if(contents==null){Files.deleteIfExists(file);require(!Files.exists(file),"Restore absent settings file");}
    else{Files.write(file,contents);require(Arrays.equals(contents,Files.readAllBytes(file)),"Restore settings byte for byte");}
    }catch(IOException e){throw new UncheckedIOException(e);}
   }
 }
}
