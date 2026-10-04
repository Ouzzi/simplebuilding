package com.simplebuilding.modules.simpletweaks.claims;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Cross-mod proof uses public registry IDs, Vanilla commands/interactions and real server ticks only. */
public final class ClaimPortalTests {
    private static void stand(ServerPlayer p,BlockPos at){p.setPos(Vec3.atBottomCenterOf(at));}
    private static void move(ServerPlayer p,ServerLevel level,BlockPos at){
        p.teleport(new TeleportTransition(level,Vec3.atBottomCenterOf(at),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
    }
    private static InteractionResult ignite(ServerPlayer p,BlockPos cell){
        return p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(cell.below()),Direction.UP,cell.below(),false));
    }
    private static Runnable guarded(boolean[] closed,Runnable cleanup,Runnable step){
        return ()->{
            if(closed[0])return;
            try{step.run();}
            catch(RuntimeException|Error failure){
                try{cleanup.run();}catch(RuntimeException|Error cleanupFailure){failure.addSuppressed(cleanupFailure);}
                throw failure;
            }
        };
    }
    public static void flow(GameTestHelper h){
        // Principle 8: Simple Dimensions is an optional partner; the standalone run (Tweaks + SimpleBuilding) passes with a note.
        if(!BuiltInRegistries.BLOCK.containsKey(Identifier.parse("simpledimension:sky_portal"))){com.mojang.logging.LogUtils.getLogger().info("[standalone] simpledimension not loaded - skipping claim portal flow");h.succeed();return;}
        var source=h.getLevel();var server=source.getServer();var original=Claims.get(server);
        var base=h.absolutePos(new BlockPos(2,80,2));var cell=new BlockPos((base.getX()>>4)*16+6,base.getY(),(base.getZ()>>4)*16+6);
        source.getChunkAt(cell);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=4;y++)source.setBlock(cell.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int y=0;y<2;y++){source.setBlock(cell.offset(-1,y,0),Blocks.GLOWSTONE.defaultBlockState(),3);source.setBlock(cell.offset(1,y,0),Blocks.GLOWSTONE.defaultBlockState(),3);}
        source.setBlock(cell.above(2),Blocks.GLOWSTONE.defaultBlockState(),3);source.setBlock(cell.south(2),Blocks.SEA_LANTERN.defaultBlockState(),3);
        var owner=h.makeMockServerPlayerInLevel();var guest=h.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);guest.setGameMode(GameType.SURVIVAL);stand(owner,cell.north(2));stand(guest,cell.north(2));
        var closed=new boolean[1];
        Runnable cleanup=()->{if(closed[0])return;closed[0]=true;Claims.install(server,original);server.getPlayerList().remove(owner);server.getPlayerList().remove(guest);};
        // Timeout fallback only; successful and failed steps clean up explicitly below.
        h.runBeforeTestEnd(cleanup);
        java.util.function.BiConsumer<Integer,Runnable> later=(ticks,step)->h.runAfterDelay(ticks,guarded(closed,cleanup,step));
        guarded(closed,cleanup,()->{
        var config=new ClaimConfig(true,8,8,16,20,false,0,List.of("minecraft:overworld","simpledimension:skyblock"));
        var root=temp();ClaimAccessTests.seed(root,ClaimAccessTests.key(owner),owner.getUUID(),Set.of());
        var claims=new Claims(config,root,Map.of());Claims.install(server,claims);var commands=ClaimAccessTests.dispatcher(config);
        var portal=BuiltInRegistries.BLOCK.getValue(Identifier.parse("simpledimension:sky_portal"));
        yes(h,portal!=Blocks.AIR,"Real Dimensions module is loaded");
        guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FIRE_CHARGE,2));
        yes(h,ignite(guest,cell)==InteractionResult.FAIL&&guest.getMainHandItem().getCount()==2&&source.getBlockState(cell).isAir(),"Untrusted real loader activation denied without mutation/cost");
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FIRE_CHARGE,2));
        yes(h,ignite(owner,cell).consumesAction()&&source.getBlockState(cell).is(portal)&&owner.getMainHandItem().getCount()==1,"Owner actual loader activation succeeds");
        stand(guest,cell);
        final ServerLevel[] destination={null};final BlockPos[] exit={null},exactReturn={null};
        later.accept(20,()->{yes(h,guest.level()==source,"Untrusted actual portal tick cannot travel");stand(owner,cell);});
        later.accept(30,()->{
            yes(h,owner.level()!=source,"Owner travels through actual server tick hook");destination[0]=owner.level();exit[0]=owner.blockPosition();
            yes(h,destination[0].getBlockState(exit[0]).is(portal),"Actual generated return exists");
            exactReturn[0]=BlockPos.of(destination[0].getBlockEntity(exit[0]).saveWithoutMetadata(destination[0].registryAccess()).getLongOr("Link",0));
            move(owner,source,cell.north(2));
        });
        later.accept(35,()->yes(h,ClaimAccessTests.execute(commands,owner,"claim trust "+guest.getUUID())==1,"Actual owner command grants source access"));
        later.accept(60,()->yes(h,guest.level()==destination[0],"Trusted second player travels through real tick hook"));
        later.accept(70,()->yes(h,ClaimAccessTests.execute(commands,owner,"claim untrust "+guest.getUUID())==1,"Owner revokes while traveler is away"));
        later.accept(90,()->yes(h,claims.create(new ClaimStore.Key(destination[0].dimension().identifier().toString(),ChunkPos.pack(exit[0])),owner.getUUID(),server.overworld().getGameTime()),"Generated return chunk can become claimed"));
        later.accept(95,()->stand(guest,exit[0].east(2)));
        later.accept(120,()->stand(guest,exit[0]));
        later.accept(130,()->{
            yes(h,guest.level()==source,"Revoked traveler escapes newly claimed generated exit");
            yes(h,!guest.chunkPosition().equals(new ChunkPos(cell.getX()>>4,cell.getZ()>>4)),"Return never enters revoked original claim");
            yes(h,Claims.allow(guest,source,guest.blockPosition()),"Emergency landing is authorized");
        });
        later.accept(140,()->{yes(h,ClaimAccessTests.execute(commands,owner,"claim trust "+guest.getUUID())==1,"Source trust restored");stand(guest,cell);});
        later.accept(165,()->{
            yes(h,guest.level()==source,"Real linked destination claim still denies trusted source traveler");
            move(owner,destination[0],exit[0].east(2));
            yes(h,ClaimAccessTests.execute(commands,owner,"claim trust "+guest.getUUID())==1,"Destination owner grants destination access");
        });
        later.accept(190,()->{yes(h,guest.level()==destination[0],"Both endpoint grants allow actual travel");move(owner,source,cell.north(2));});
        later.accept(200,()->stand(guest,exit[0].east(2)));
        later.accept(255,()->stand(guest,exit[0]));
        final Claims[] disabled={null};
        later.accept(275,()->{
            yes(h,guest.level()==source&&guest.blockPosition().equals(exactReturn[0]),"Trusted exact generated return succeeds");
            yes(h,ClaimAccessTests.execute(commands,owner,"claim untrust "+guest.getUUID())==1,"Restore denied source before disabled control");
            var archived=temp();try {Files.writeString(archived.resolve("simpletweaks-claims.json"),"unreadable archive");}catch(java.io.IOException e){throw new IllegalStateException(e);}
            disabled[0]=new Claims(ClaimConfig.DEFAULT,archived,Map.of());Claims.install(server,disabled[0]);stand(guest,cell.north(2));
        });
        later.accept(320,()->stand(guest,cell));
        later.accept(345,()->{yes(h,guest.level()==destination[0]&&!disabled[0].dataLoaded(),"Disabled Claims permits actual travel without ledger IO");cleanup.run();h.succeed();});
        }).run();
    }
}
