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
        TESTS.put("claims_tools_bed_head_footprint",ToolClaimTests::bedHeadFootprint);
        TESTS.put("claims_followup_crafter",Stage4ClaimTests::crafter);
        TESTS.put("claims_followup_copper_golem",Stage4ClaimTests::copperGolem);
        TESTS.put("claims_followup_lightning",Stage4ClaimTests::lightning);
        TESTS.put("claims_environment_natural_damage",Stage4ClaimTests::naturalDamage);
        TESTS.put("claims_environment_explosion_multipart",Stage4ClaimTests::explosionMultipart);
        TESTS.put("claims_environment_indirect_cloud",Stage4ClaimTests::indirectCloud);
        TESTS.put("claims_environment_connected_pistons",Stage4ClaimTests::connectedPistons);
        TESTS.put("claims_environment_custom_pistons",Stage4ClaimTests::customPistons);
        TESTS.put("claims_environment_custom_hoppers",Stage4ClaimTests::customHoppers);
        TESTS.put("claims_environment_attractor",Stage4ClaimTests::attractor);
        TESTS.put("claims_environment_disabled",EnvironmentClaimTests::disabled);
        TESTS.put("claims_environment_pickup",EnvironmentClaimTests::pickup);
        TESTS.put("claims_environment_explosion_fire",EnvironmentClaimTests::explosionFire);
        TESTS.put("claims_environment_fluid_piston",EnvironmentClaimTests::fluidPiston);
        TESTS.put("claims_environment_hopper",EnvironmentClaimTests::hopper);
        TESTS.put("claims_environment_projectiles",EnvironmentClaimTests::projectiles);
        TESTS.put("claims_environment_dispenser",EnvironmentClaimTests::dispenser);

        TESTS.put("claims_portal_flow",ClaimPortalTests::flow);
        TESTS.put("claims_access_commands",ClaimAccessTests::commandsAndAccess);
        TESTS.put("claims_access_caps_failures",ClaimAccessTests::capsAndFailures);
        TESTS.put("claims_access_admin",ClaimAccessTests::adminPolicy);
        TESTS.put("claims_tools_bed_hammer",ToolClaimTests::bedHammer);
        TESTS.put("claims_tools_wand_hammer",ToolClaimTests::wandHammer);
        TESTS.put("claims_tools_beam",ToolClaimTests::beam);
        TESTS.put("claims_tools_echo",ToolClaimTests::echo);
        TESTS.put("claims_tools_pad",ToolClaimTests::pad);

        TESTS.put("claims_config_bounds",ClaimTests::config);
        TESTS.put("claims_vanilla_border",ClaimTests::vanilla);
        TESTS.put("claims_bucket_entity_hooks",ClaimTests::bucketsAndEntities);
        TESTS.put("claims_disabled_hooks",ClaimTests::disabledHooks);
        TESTS.put("claims_bed_footprint",ClaimTests::bed);
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
        var p=player(h,h.absolutePos(new BlockPos(1,3,1)));
        yes(h,!c.trust(p,UUID.randomUUID(),true)&&!c.unclaim(p,false)&&!c.dataLoaded(),"Disabled access changes perform no IO");
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
        with(h,on(),test);
    }
    static void with(GameTestHelper h, ClaimConfig config, Consumer<Claims> test) {
        var server=h.getLevel().getServer();var old=Claims.get(server);var claims=new Claims(config,temp(),Map.of());Claims.install(server,claims);
        try {test.accept(claims);}finally {Claims.install(server,old);}
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
    static void vanilla(GameTestHelper h) {
        with(h,c->{
            var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,3,2));
            var inside=new BlockPos((base.getX()>>4)*16+16,base.getY(),(base.getZ()>>4)*16+8);
            var outside=inside.west();var owner=player(h,inside.east(2));var stranger=player(h,outside.west(2));
            yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Claim boundary fixture");
            level.setBlock(inside,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            yes(h,!stranger.gameMode.destroyBlock(inside)&&level.getBlockState(inside).is(net.minecraft.world.level.block.Blocks.STONE),"Actual break hook denies stranger");
            yes(h,owner.gameMode.destroyBlock(inside)&&level.getBlockState(inside).isAir(),"Actual break hook permits owner");
            level.setBlock(outside,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(outside),net.minecraft.core.Direction.EAST,outside,false);
            stranger.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,2));
            yes(h,stranger.gameMode.useItemOn(stranger,level,stranger.getMainHandItem(),InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL,"Clicked unclaimed, adjacent target claimed");
            yes(h,level.getBlockState(inside).isAir()&&stranger.getMainHandItem().getCount()==2,"No placement or cost across claim boundary");
            owner.setPos(stranger.position());owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,2));
            yes(h,owner.gameMode.useItemOn(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction()&&level.getBlockState(inside).is(net.minecraft.world.level.block.Blocks.DIRT),"Owner actual placement succeeds");
            level.setBlock(inside.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            level.setBlock(inside,net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.LeverBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR),3);
            var click=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(inside),net.minecraft.core.Direction.UP,inside,false);
            stranger.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            yes(h,stranger.gameMode.useItemOn(stranger,level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,click)==InteractionResult.FAIL&&!level.getBlockState(inside).getValue(net.minecraft.world.level.block.LeverBlock.POWERED),"Actual block interaction denied");
        });h.succeed();
    }
    static void bucketsAndEntities(GameTestHelper h) {
        with(h,c->{
            var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,3,2));var inside=new BlockPos((base.getX()>>4)*16+16,base.getY(),(base.getZ()>>4)*16+8);
            var owner=player(h,inside.east(3));var stranger=player(h,inside.west(3));
            yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Claim fixture");
            level.setBlock(inside,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            var bucket=(BucketItem)Items.WATER_BUCKET;
            yes(h,!bucket.emptyContents(stranger,level,inside,null)&&level.getBlockState(inside).isAir(),"Real bucket placement refuses stranger");
            yes(h,bucket.emptyContents(owner,level,inside,null)&&level.getFluidState(inside).isSource(),"Real bucket placement permits owner");
            stranger.setPos(inside.getX()+.5,inside.getY()+2,inside.getZ()+.5);stranger.setXRot(90);
            stranger.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            yes(h,Items.BUCKET.use(level,stranger,InteractionHand.MAIN_HAND)==InteractionResult.FAIL&&level.getFluidState(inside).isSource(),"Real bucket pickup refuses stranger");
            level.setBlock(inside,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            var cow=net.minecraft.world.entity.EntityTypes.COW.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);cow.setPos(inside.getX()+.5,inside.getY(),inside.getZ()+.5);level.addFreshEntity(cow);
            stranger.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            yes(h,stranger.interactOn(cow,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO)==InteractionResult.FAIL&&stranger.getMainHandItem().is(Items.BUCKET),"Actual entity interaction refused");
            float health=cow.getHealth();stranger.attack(cow);yes(h,cow.getHealth()==health,"Actual melee attack refused");
            yes(h,!stranger.stabAttack(net.minecraft.world.entity.EquipmentSlot.MAINHAND,cow,5,false,false,false)&&cow.getHealth()==health,"Piercing attack refuses stranger");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            yes(h,owner.interactOn(cow,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO).consumesAction()&&owner.getMainHandItem().is(Items.MILK_BUCKET),"Owner entity interaction allowed");
            cow.discard();
        });h.succeed();
    }

    private ClaimTests() {}
    static void disabledHooks(GameTestHelper h) {
        with(h,ClaimConfig.DEFAULT,c->{
            var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,3,1));var p=player(h,pos);
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            yes(h,p.gameMode.destroyBlock(pos)&&level.getBlockState(pos).isAir(),"Disabled break hook preserves Vanilla");
            yes(h,((BucketItem)Items.WATER_BUCKET).emptyContents(p,level,pos,null),"Disabled bucket hook preserves Vanilla");
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            yes(h,!c.dataLoaded(),"Actual disabled hooks perform no ledger reads");
        });h.succeed();
    }
    static void bed(GameTestHelper h) {
        with(h,c->{
            var level=h.getLevel();var base=h.absolutePos(new BlockPos(1,3,1));
            var head=new BlockPos((base.getX()>>4)*16+16,base.getY(),(base.getZ()>>4)*16+8);var foot=head.west();
            var owner=player(h,foot.west(3));var other=player(h,foot.west(3));other.setYRot(270);owner.setYRot(270);
            yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(head)),owner.getUUID(),100),"Bed boundary fixture");
            for(var pos:List.of(foot,head)){level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);}
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(foot.below()),net.minecraft.core.Direction.UP,foot.below(),false);
            other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BED.red()));
            var context=new net.minecraft.world.item.context.BlockPlaceContext(other,InteractionHand.MAIN_HAND,other.getMainHandItem(),hit);
            yes(h,((BlockItem)Items.BED.red()).place(context)==InteractionResult.FAIL&&level.getBlockState(foot).isAir()&&level.getBlockState(head).isAir(),"Actual bed placement checks head across border before placing foot");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BED.red()));
            yes(h,((BlockItem)Items.BED.red()).place(new net.minecraft.world.item.context.BlockPlaceContext(owner,InteractionHand.MAIN_HAND,owner.getMainHandItem(),hit)).consumesAction()&&level.getBlockState(head).is(net.minecraft.world.level.block.Blocks.BED.red()),"Owner can place complete bed");
            yes(h,!other.gameMode.destroyBlock(foot)&&level.getBlockState(head).is(net.minecraft.world.level.block.Blocks.BED.red()),"Breaking unclaimed foot cannot remove protected head");
        });h.succeed();
    }
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
