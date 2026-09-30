package com.simplebuilding.modules.simpletweaks.claims;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;

public final class ClaimTests {
    public static final Map<String,Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("claims_config_bounds",ClaimTests::config);
        TESTS.put("claims_legacy_atomic_roundtrip",ClaimTests::persistence);
        TESTS.put("claims_malformed_preserved",ClaimTests::malformed);
        TESTS.put("claims_disabled_no_io",ClaimTests::disabled);
        TESTS.put("claims_caps_and_cooldown",ClaimTests::caps);
        TESTS.put("claims_deed_two_players",ClaimTests::deed);
        TESTS.put("claims_policy_boundaries",ClaimTests::policy);
        TESTS.put("claims_command_collision",ClaimTests::commands);
    }
    static void yes(GameTestHelper h,boolean condition,String why) { h.assertTrue(condition,why); }
    static Path temp() { try { return Files.createTempDirectory("simpletweaks-claims-test-"); } catch(java.io.IOException e) { throw new AssertionError(e); } }
    static ClaimConfig on() { return new ClaimConfig(true,2,2,3,20,false,0,ClaimConfig.DEFAULT.dimensions()); }
    static void config(GameTestHelper h) {
        yes(h,!ClaimConfig.DEFAULT.enabled()&&!ClaimConfig.DEFAULT.opBypass(),"Default off, no implicit OP bypass");
        var c=new ClaimConfig(true,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,true,Integer.MAX_VALUE,List.of());
        yes(h,c.maxClaimsPerPlayer()==256&&c.maxTrustedPlayers()==64&&c.globalCap()==10000&&c.cooldownTicks()==72000&&c.spawnBuffer()==256,"All upper bounds");
        c=new ClaimConfig(true,-1,-1,-1,-1,false,-1,List.of());
        yes(h,c.maxClaimsPerPlayer()==1&&c.maxTrustedPlayers()==0&&c.globalCap()==1&&c.cooldownTicks()==20&&c.spawnBuffer()==0,"All lower bounds");
        try { new ClaimConfig(true,1,1,1,20,false,0,List.of("../invalid")); throw new AssertionError("Invalid dimension accepted"); } catch(IllegalArgumentException expected) {}
        try {
            Path p=temp().resolve("config.json"); var d=ClaimConfig.load(p); yes(h,d.equals(ClaimConfig.DEFAULT),"First config defaults");
            byte[] original=Files.readAllBytes(p); yes(h,ClaimConfig.load(p).equals(d)&&Arrays.equals(original,Files.readAllBytes(p)),"Read does not rewrite config");
            Files.writeString(p,"{\"enabled\":\"true\"}");
            try { ClaimConfig.load(p); throw new AssertionError("Nonboolean switch accepted"); } catch(IllegalStateException expected) {}
        } catch(java.io.IOException e) {throw new AssertionError(e);}
        h.succeed();
    }
    static void persistence(GameTestHelper h) {
        try {
            Path dir=temp(), legacy=dir.resolve("legacy.dat"), target=dir.resolve("claims.json");
            UUID owner=UUID.randomUUID(), guest=UUID.randomUUID();
            var data=new CompoundTag(); var row=new CompoundTag();row.putString("Owner",owner.toString());
            var list=new ListTag();list.add(StringTag.valueOf(guest.toString()));row.put("Whitelist",list);data.put("-1",row);
            var root=new CompoundTag();root.put("data",data);root.putInt("DataVersion",1);NbtIo.writeCompressed(root,legacy);
            var unloaded=dir.resolve("dimensions/archive/unloaded/data/simpletweaks_claims.dat");Files.createDirectories(unloaded.getParent());NbtIo.writeCompressed(root,unloaded);
            byte[] before=Files.readAllBytes(legacy);
            var store=new ClaimStore(target,Map.of("minecraft:overworld",legacy));var key=new ClaimStore.Key("minecraft:overworld",-1);
            yes(h,store.view().get(key).permits(guest)&&!Files.exists(target),"Legacy parsed without writing migration");
            yes(h,store.view().containsKey(new ClaimStore.Key("archive:unloaded",-1)),"Unloaded dimension claims imported before global ledger publication");
            try {store.view().clear();throw new AssertionError("Mutable view");}catch(UnsupportedOperationException expected){}
            try {store.view().get(key).whitelist().clear();throw new AssertionError("Mutable whitelist");}catch(UnsupportedOperationException expected){}
            store.replace(store.view());var again=new ClaimStore(target,Map.of());
            yes(h,again.view().equals(store.view())&&Arrays.equals(before,Files.readAllBytes(legacy)),"Round trip, original legacy bytes retained");
            Path failureDir=temp();
            var unwritable=new ClaimStore(failureDir.resolve("future.json"),Map.of());
            Files.createDirectory(failureDir.resolve("future.json"));
            try {unwritable.replace(store.view());throw new AssertionError("Atomic move should fail");}catch(java.io.IOException expected){}
            yes(h,unwritable.view().isEmpty(),"Failed persistence cannot publish new authority");
        }catch(java.io.IOException e){throw new AssertionError(e);}h.succeed();
    }
    static void malformed(GameTestHelper h) {
        try {
            Path dir=temp(), old=dir.resolve("legacy.dat"); Files.writeString(old,"malformed legacy");byte[] bytes=Files.readAllBytes(old);
            var c=new Claims(on(),dir,Map.of("minecraft:overworld",old));
            yes(h,!c.allowed("minecraft:overworld",UUID.randomUUID(),0)&&c.locked(),"Malformed data locks enabled protection");
            yes(h,Arrays.equals(bytes,Files.readAllBytes(old))&&!Files.exists(dir.resolve("simpletweaks-claims.json")),"Malformed original never overwritten");
        }catch(java.io.IOException e){throw new AssertionError(e);}h.succeed();
    }
    static void disabled(GameTestHelper h) {
        Path dir=temp();var c=new Claims(ClaimConfig.DEFAULT,dir,Map.of("minecraft:overworld",dir.resolve("unreadable.dat")));
        try {Files.writeString(dir.resolve("simpletweaks-claims.json"),"malformed archived data");}catch(java.io.IOException e){throw new AssertionError(e);}
        yes(h,c.allowed("minecraft:overworld",null,0)&&c.view().isEmpty()&&!c.dataLoaded(),"Disabled lookup never opens data");
        yes(h,!c.create(new ClaimStore.Key("minecraft:overworld",0),UUID.randomUUID(),100)&&!c.dataLoaded(),"Disabled mutations do nothing");
        try {yes(h,Files.readString(dir.resolve("simpletweaks-claims.json")).equals("malformed archived data"),"No claim writes when disabled");}catch(java.io.IOException e){throw new AssertionError(e);}h.succeed();
    }
    static void caps(GameTestHelper h) {
        var c=new Claims(on(),temp(),Map.of());UUID owner=UUID.randomUUID(),other=UUID.randomUUID();
        yes(h,c.create(new ClaimStore.Key("minecraft:overworld",1),owner,100),"First claim");
        yes(h,!c.create(new ClaimStore.Key("minecraft:overworld",2),owner,119),"Cooldown");
        yes(h,c.create(new ClaimStore.Key("minecraft:the_nether",2),owner,120),"Cooldown boundary across dimensions");
        yes(h,!c.create(new ClaimStore.Key("minecraft:overworld",3),owner,140),"Per-owner global cap");
        yes(h,c.create(new ClaimStore.Key("minecraft:overworld",3),other,140),"Other player budget");
        yes(h,!c.create(new ClaimStore.Key("minecraft:overworld",4),UUID.randomUUID(),160),"Global cap");
        yes(h,!c.create(new ClaimStore.Key("other:dimension",5),other,160),"Dimension allowlist");h.succeed();
    }
    static ServerPlayer player(GameTestHelper h,BlockPos pos) {
        var p=h.makeMockServerPlayerInLevel();p.setUUID(UUID.randomUUID());p.setGameMode(GameType.SURVIVAL);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;
    }
    static void with(GameTestHelper h, Consumer<Claims> test) {
        var server=h.getLevel().getServer();var old=Claims.get(server);var claims=new Claims(on(),temp(),Map.of());Claims.SERVERS.put(server,claims);
        try {test.accept(claims);}finally {if(old==null)Claims.SERVERS.remove(server);else Claims.SERVERS.put(server,old);}
    }
    static void deed(GameTestHelper h) {
        with(h,c->{
            var pos=h.absolutePos(new BlockPos(1,2,1)).offset(1024,0,1024);var owner=player(h,pos);var stranger=player(h,pos);
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse("simpletweaks:claim_deed"));
            var stack=new ItemStack(item);var data=new CompoundTag();data.putString("OwnerName","forged");data.putString("Unknown","retained");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            yes(h,item.use(h.getLevel(),owner,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"Real deed creates server-owned claim");
            stranger.setItemInHand(InteractionHand.MAIN_HAND,stack.copy());
            yes(h,item.use(h.getLevel(),stranger,InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"Forged deed cannot steal claim");
            yes(h,stack.get(DataComponents.CUSTOM_DATA).copyTag().equals(data),"All legacy deed custom data retained");
            var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();ClaimCommands.register(dispatcher,on());
            var root=dispatcher.getRoot().getChild("claim");
            yes(h,root.canUse(owner.createCommandSourceStack())&&!root.getChild("admin").canUse(owner.createCommandSourceStack()),"Owner command and separate OP4 administration");
        });h.succeed();
    }
    static void policy(GameTestHelper h) {
        with(h,c->{
            var level=h.getLevel();var server=level.getServer();var p=player(h,server.getRespawnData().pos());
            yes(h,!c.claim(p)&&c.view().isEmpty(),"Spawn chunk cannot be claimed");
            var border=level.getWorldBorder();double oldSize=border.getSize(),oldX=border.getCenterX(),oldZ=border.getCenterZ();
            try {
                border.setCenter(1008,1008);border.setSize(20);p.setPos(1016,80,1008);
                yes(h,!c.claim(p),"A chunk crossing the world border is refused even when player is inside");
                border.setSize(128);p.setPos(1008,80,1008);p.setGameMode(GameType.SPECTATOR);
                yes(h,!c.claim(p),"Spectators cannot claim");p.setGameMode(GameType.ADVENTURE);
                yes(h,!c.claim(p),"Adventure mode cannot claim");p.setGameMode(GameType.SURVIVAL);
                yes(h,c.claim(p),"Allowed survival claim inside the complete border");
            }finally {border.setCenter(oldX,oldZ);border.setSize(oldSize);}
        });h.succeed();
    }
    private ClaimTests() {}
    static void commands(GameTestHelper h) {
        var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        var foreign=dispatcher.register(net.minecraft.commands.Commands.literal("claim").executes(c->7));
        var handler=foreign.getCommand();
        ClaimCommands.register(dispatcher,ClaimConfig.DEFAULT);
        yes(h,dispatcher.getRoot().getChild("claim")==foreign&&foreign.getCommand()==handler&&foreign.getChildren().isEmpty(),"Disabled registration preserves foreign node, handler and children");
        try {yes(h,dispatcher.execute("claim",h.getLevel().getServer().createCommandSourceStack())==7,"Foreign command still executes");}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new AssertionError(e);}
        try {ClaimCommands.register(dispatcher,on());throw new AssertionError("Enabled collision accepted");}catch(IllegalStateException expected){}
        yes(h,foreign.getCommand()==handler&&foreign.getChildren().isEmpty(),"Enabled collision cannot replace foreign handler");
        var empty=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();ClaimCommands.register(empty,ClaimConfig.DEFAULT);
        yes(h,empty.getRoot().getChildren().isEmpty(),"Disabled registration adds no command");h.succeed();
    }
}
