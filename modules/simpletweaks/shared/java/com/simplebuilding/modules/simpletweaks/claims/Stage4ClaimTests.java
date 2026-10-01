package com.simplebuilding.modules.simpletweaks.claims;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimPistonInvoker;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Real server entrypoints; the only custom-mod lookup is through public registry IDs. */
final class Stage4ClaimTests {
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:"+id));}
    private static void modes(GameTestHelper h,java.util.function.BiConsumer<Fixture,Boolean> test){
        for(int mode:new int[]{1,0,-1})with(h,mode==1?on():ClaimConfig.DEFAULT,c->{
            boolean enabled=mode==1;
            if(mode==-1)Claims.install(h.getLevel().getServer(),null);
            try(var f=new Fixture(h,c,enabled)){test.accept(f,enabled);if(!enabled)yes(h,!c.dataLoaded(),"Disabled automation never opens the ledger");}
        });
        h.succeed();
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel l;final BlockPos edge;final Claims claims;
        final net.minecraft.server.level.ServerPlayer owner;
        final List<Entity> entities=new ArrayList<>();final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        Fixture(GameTestHelper h,Claims c,boolean enabled){
            this.h=h;this.l=h.getLevel();this.claims=c;
            // High, isolated workspace. Each synchronous test restores every cell before returning.
            edge=ToolClaimTests.boundary(h).above(90);
            owner=player(h,edge.east(12).above(6));entities.add(owner);
            if(enabled)yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(edge)),owner.getUUID(),100),"Stage 4 owner fixture");
            for(int x=-10;x<=12;x++)for(int y=-1;y<=4;y++)for(int z=-5;z<=5;z++){
                var p=edge.offset(x,y,z);saved.put(p,l.getBlockState(p));l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            }
        }
        void set(BlockPos p,BlockState s){saved.putIfAbsent(p,l.getBlockState(p));l.setBlock(p,s,2);}
        void set(BlockPos p,Block b){set(p,b.defaultBlockState());}
        <T extends Entity> T add(T entity,BlockPos p){entity.setPos(Vec3.atBottomCenterOf(p));l.addFreshEntity(entity);entities.add(entity);return entity;}
        @Override public void close(){
            entities.forEach(Entity::discard);
            l.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(edge.offset(-12,-2,-7)),Vec3.atLowerCornerOf(edge.offset(15,8,8)))).forEach(Entity::discard);
            // Clear containers without dropping fixture inventory.
            for(var p:saved.keySet())if(l.getBlockEntity(p) instanceof Container container)container.clearContent();
            saved.forEach((p,s)->l.setBlock(p,s,2));
            l.getEntitiesOfClass(ItemEntity.class,new AABB(edge).inflate(16)).forEach(Entity::discard);
        }
    }
    static void naturalDamage(GameTestHelper h){modes(h,(f,enabled)->{
        for(var p:List.of(f.edge.west(3),f.edge.east(3))){
            var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),p);float before=cow.getHealth();
            yes(h,cow.causeFallDamage(8,1,f.l.damageSources().fall())&&cow.getHealth()<before,"Real fall damage remains on either side of a claim");cow.discard();
            var swimmer=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),p);swimmer.setNoAi(true);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)f.set(p.offset(x,y,z),Blocks.WATER);
            swimmer.tick();swimmer.setAirSupply(-19);before=swimmer.getHealth();swimmer.tick();
            yes(h,swimmer.getHealth()<before,"Actual submerged entity tick still causes drowning");swimmer.discard();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)f.set(p.offset(x,y,z),Blocks.AIR);
        }
        for(var source:List.of(f.l.damageSources().starve(),f.l.damageSources().inWall(),f.l.damageSources().freeze(),f.l.damageSources().magic())){
            var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),f.edge.east(3));float before=cow.getHealth();
            yes(h,cow.hurtServer(f.l,source,2)&&cow.getHealth()<before,"Owner-independent natural damage: "+source);cow.discard();
        }
        var stranger=player(h,f.edge.east(4));f.entities.add(stranger);float before=stranger.getHealth();
        stranger.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        yes(h,stranger.hurtServer(f.l,f.l.damageSources().starve(),2)&&stranger.getHealth()<before,"Claim does not make a visiting player immune to starvation");
    });}
    static void explosionMultipart(GameTestHelper h){modes(h,(f,enabled)->{
        for(var bed:List.of(((BlockItem)Items.BED.red()).getBlock(),((BlockItem)Items.STRAW_BED).getBlock())){
            for(boolean border:new boolean[]{true,false}){
                var head=border?f.edge:f.edge.west(6);var foot=head.west();
                f.set(foot.below(),Blocks.OBSIDIAN);f.set(head.below(),Blocks.OBSIDIAN);
                f.set(foot,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.EAST).setValue(BedBlock.PART,BedPart.FOOT));
                f.set(head,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.EAST).setValue(BedBlock.PART,BedPart.HEAD));
                yes(h,f.l.getBlockState(head).is(bed)&&f.l.getBlockState(foot).is(bed),"Complete bed fixture");
                new ServerExplosion(f.l,null,null,null,Vec3.atCenterOf(foot),3,false,Explosion.BlockInteraction.DESTROY).explode();
                yes(h,enabled&&border?f.l.getBlockState(head).is(bed)&&f.l.getBlockState(foot).is(bed):f.l.getBlockState(head).isAir()&&f.l.getBlockState(foot).isAir(),"Real explosion checks both bed halves; border="+border+", enabled="+enabled);
                f.set(head,Blocks.AIR);f.set(foot,Blocks.AIR);
            }
        }
    });}
    static void indirectCloud(GameTestHelper h){modes(h,(f,enabled)->{
        var stranger=player(h,f.edge.west(5));f.entities.add(stranger);
        for(var actor:List.of(stranger,f.owner)){
            var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),f.edge.east(3));cow.setNoAi(true);
            var cloud=f.add(EntityTypes.AREA_EFFECT_CLOUD.create(f.l,EntitySpawnReason.COMMAND),f.edge.east(3));
            cloud.setOwner(actor);cloud.setWaitTime(0);cloud.setRadius(2);cloud.setRadiusOnUse(0);
            yes(h,cloud.getOwner()==actor&&f.l.getEntitiesOfClass(LivingEntity.class,cloud.getBoundingBox()).contains(cow),"Cloud fixture has a resolved owner and a visible target");
            cloud.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
            for(int i=0;i<10;i++)cloud.tick();
            yes(h,cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON)==(!enabled||actor==f.owner),"Actual cloud ticks retain player attribution; enabled="+enabled+", owner="+(actor==f.owner));
            float before=cow.getHealth();
            boolean hit=cow.hurtServer(f.l,f.l.damageSources().indirectMagic(cloud,actor),3);
            yes(h,hit==(!enabled||actor==f.owner)&&(hit?cow.getHealth()<before:cow.getHealth()==before),"Actual indirect damage retains player attribution");
            var item=f.add(new ItemEntity(f.l,0,0,0,new ItemStack(Items.APPLE),0,0,0),f.edge.east(2));
            yes(h,item.hurtServer(f.l,f.l.damageSources().indirectMagic(cloud,actor),10)==(!enabled||actor==f.owner)
                    &&item.isRemoved()==(!enabled||actor==f.owner),"Final ItemEntity damage override retains indirect authority");item.discard();
            cow.discard();cloud.discard();
        }
    });}
    static void connectedPistons(GameTestHelper h){modes(h,(f,enabled)->{
        for(var sticky:List.of(Blocks.SLIME_BLOCK,Blocks.HONEY_BLOCK)){
            var p=f.edge.west().north(2);var s=Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.SOUTH);
            f.set(p,s);f.set(p.south(),sticky);f.set(p.south().east(),Blocks.STONE);
            boolean moved=((ClaimPistonInvoker)Blocks.PISTON).claims$move(f.l,p,Direction.SOUTH,true);
            yes(h,moved==!enabled,"Connected "+sticky+" branch cannot pull claimed block");
            if(enabled)yes(h,f.l.getBlockState(p.south()).is(sticky)&&f.l.getBlockState(p.south().east()).is(Blocks.STONE),"Denied connected movement is atomic");
            for(int x=0;x<=1;x++)for(int z=0;z<=3;z++)f.set(p.offset(x,0,z),Blocks.AIR);
            var own=f.edge.east(3).north(2);f.set(own,s);f.set(own.south(),sticky);f.set(own.south().east(),Blocks.STONE);
            yes(h,((ClaimPistonInvoker)Blocks.PISTON).claims$move(f.l,own,Direction.SOUTH,true),"Same-owner connected movement works");
            for(int x=0;x<=1;x++)for(int z=0;z<=3;z++)f.set(own.offset(x,0,z),Blocks.AIR);
            // The initial block is unclaimed; only a connected side branch crosses the border.
            f.set(p,Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.SOUTH).setValue(PistonBaseBlock.EXTENDED,true));
            f.set(p.south(),Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING,Direction.SOUTH).setValue(PistonHeadBlock.TYPE,PistonType.STICKY));
            f.set(p.south(2),sticky);f.set(p.south(2).east(),Blocks.STONE);
            yes(h,((ClaimPistonInvoker)Blocks.STICKY_PISTON).claims$move(f.l,p,Direction.SOUTH,false)==!enabled,"Retraction previews connected "+sticky+" branch without removing the head");
            if(enabled)yes(h,f.l.getBlockState(p.south()).is(Blocks.PISTON_HEAD)&&f.l.getBlockState(p.south(2).east()).is(Blocks.STONE),"Denied connected retraction preserves both head and foreign block");
            for(int x=0;x<=1;x++)for(int z=0;z<=3;z++)f.set(p.offset(x,0,z),Blocks.AIR);
        }
        for(boolean own:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west(2);var s=Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST).setValue(PistonBaseBlock.EXTENDED,true);
            f.set(p,s);f.set(p.east(),Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING,Direction.EAST).setValue(PistonHeadBlock.TYPE,PistonType.STICKY));f.set(p.east(2),Blocks.STONE);
            yes(h,((ClaimPistonInvoker)Blocks.STICKY_PISTON).claims$move(f.l,p,Direction.EAST,false)==(!enabled||own),"Actual sticky retraction checks source ownership; enabled="+enabled+", own="+own);
        }
    });}
    static void customPistons(GameTestHelper h){modes(h,(f,enabled)->{
        for(String id:List.of("reinforced_piston","netherite_piston","enderite_piston"))for(boolean own:new boolean[]{false,true})for(boolean breach:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west();var machine=block(id);var state=machine.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST);
            for(int x=-1;x<=4;x++){f.set(p.east(x),Blocks.AIR);f.set(p.east(x).below(),Blocks.AIR);}
            f.set(p,state);f.set(p.east(),breach?Blocks.BEDROCK:Blocks.STONE);f.set(p.below(),Blocks.REDSTONE_BLOCK);
            state.triggerEvent(f.l,p,0,Direction.EAST.get3DDataValue());
            if(enabled&&!own)yes(h,f.l.getBlockState(p).equals(state)&&f.l.getBlockState(p.east()).is(breach?Blocks.BEDROCK:Blocks.STONE)&&f.l.getBlockState(p.below()).is(Blocks.REDSTONE_BLOCK),"Denied custom piston preserves target, wear and fuel: "+id);
            else yes(h,!f.l.getBlockState(p.east()).is(breach?Blocks.BEDROCK:Blocks.STONE),"Real custom piston positive/off control: "+id+", breach="+breach);
        }
        // Reinforced piston fuel can be across a different boundary from its movement.
        var p=f.edge;var machine=block("reinforced_piston");var state=machine.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST);
        f.set(p,state);f.set(p.east(),Blocks.BEDROCK);f.set(p.east(2),Blocks.AIR);f.set(p.west(),Blocks.REDSTONE_BLOCK);
        state.triggerEvent(f.l,p,0,Direction.EAST.get3DDataValue());
        yes(h,enabled?f.l.getBlockState(p.west()).is(Blocks.REDSTONE_BLOCK)&&f.l.getBlockState(p).equals(state):f.l.getBlockState(p.west()).isAir(),"Reinforced breach checks fuel before movement");
    });}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void tick(ServerLevel l,BlockPos p){
        var state=l.getBlockState(p);var be=l.getBlockEntity(p);
        var ticker=(BlockEntityTicker<BlockEntity>)(BlockEntityTicker)((EntityBlock)state.getBlock()).getTicker(l,state,be.getType());ticker.tick(l,p,state,be);
    }
    static void customHoppers(GameTestHelper h){modes(h,(f,enabled)->{
        if(enabled)yes(h,f.claims.create(new ClaimStore.Key(f.l.dimension().identifier().toString(),ChunkPos.pack(f.edge.west())),UUID.randomUUID(),100),"Distinct neighboring machine owner");
        for(String id:List.of("reinforced_hopper","netherite_hopper","enderite_hopper"))for(boolean own:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west();var target=p.east();
            f.set(p,Blocks.AIR);f.set(target,Blocks.AIR);f.set(p,block(id).defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST));f.set(target,Blocks.CHEST);
            var from=(Container)f.l.getBlockEntity(p);var into=(Container)f.l.getBlockEntity(target);from.setItem(0,new ItemStack(Items.APPLE,3));
            for(int t=0;t<16;t++)tick(f.l,p);
            yes(h,enabled&&!own?into.isEmpty()&&from.getItem(0).getCount()==3:!into.isEmpty(),"Actual custom hopper transfer: "+id+", own="+own);
            from.clearContent();into.clearContent();f.set(p,Blocks.AIR);f.set(target,Blocks.AIR);
        }
        // Dropper has its own dispenseFrom override, including an ejection path without a container.
        for(boolean own:new boolean[]{false,true})for(boolean container:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west();f.set(p,Blocks.AIR);f.set(p.east(),container?Blocks.CHEST:Blocks.AIR);
            var state=Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);f.set(p,state);
            var from=(Container)f.l.getBlockEntity(p);from.setItem(0,new ItemStack(Items.APPLE,3));state.tick(f.l,p,RandomSource.create(0));
            yes(h,from.getItem(0).getCount()==(enabled&&!own?3:2),"Real dropper source/destination check, container="+container);
        }
        var p=f.edge.west();f.set(p,Blocks.AIR);f.set(p.east(),Blocks.AIR);
        var dispenser=Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);f.set(p,dispenser);
        var source=(Container)f.l.getBlockEntity(p);source.setItem(0,new ItemStack(Items.WATER_BUCKET));dispenser.tick(f.l,p,RandomSource.create(0));
        yes(h,enabled?source.getItem(0).is(Items.WATER_BUCKET)&&f.l.getFluidState(p.east()).isEmpty():source.getItem(0).is(Items.BUCKET)&&!f.l.getFluidState(p.east()).isEmpty(),"Dispenser denial preserves its item; disabled and absent providers retain bucket behavior");
    });}
    static void attractor(GameTestHelper h){modes(h,(f,enabled)->{
        var p=f.edge.west(2);
        if(Math.floorMod(f.l.getGameTime()+p.asLong(),2)!=0)p=p.above();
        f.set(p.below(),Blocks.STONE);f.owner.setShiftKeyDown(true);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ToolClaimTests.item("magnet")));
        var hit=new BlockHitResult(Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false);
        yes(h,f.owner.getMainHandItem().getItem().useOn(new UseOnContext(f.owner,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Place attractor through real item use");
        var foreign=f.add(new ItemEntity(f.l,0,0,0,new ItemStack(Items.APPLE),0,0,0),new BlockPos(f.edge.getX()+1,p.getY(),p.getZ()));
        var local=f.add(new ItemEntity(f.l,0,0,0,new ItemStack(Items.APPLE),0,0,0),p.west(3));
        tick(f.l,p);
        yes(h,foreign.getDeltaMovement().equals(Vec3.ZERO)==enabled&&!local.getDeltaMovement().equals(Vec3.ZERO),"Actual placed-attractor ticker preserves foreign items and pulls local items");
        if(enabled){
            yes(h,f.claims.create(new ClaimStore.Key(f.l.dimension().identifier().toString(),ChunkPos.pack(p)),f.owner.getUUID(),120),"Adjacent same-owner chunk fixture");
            tick(f.l,p);yes(h,!foreign.getDeltaMovement().equals(Vec3.ZERO),"Attractor works across adjacent same-owner chunks");
        }
    });}
}
