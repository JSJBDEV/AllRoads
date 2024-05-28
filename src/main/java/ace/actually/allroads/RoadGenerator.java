package ace.actually.allroads;

import brightspark.asynclocator.AsyncLocator;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * We actually don't want any persistent data in this class - this means that we can skip trying to tick the generator
 * if there are no roads
 */
public class RoadGenerator {

    private List<BlockPos> foundStructures = new ArrayList<>();
    private List<BlockPos[]> plannedRoads = new ArrayList<>();

    private BlockPos pos1c;

    private int xdistance;
    private int zdistance;

    public boolean roadLatch = false;

    private static final  Heightmap.Type HEIGHTMAP = Heightmap.Type.MOTION_BLOCKING_NO_LEAVES;

    /**
     * loads a "road plan" to be used by makeRoadTick
     * generally, a road will start diagonal and then finish straight
     * @param server the server with the dataCommandStorage
     * @param pos1 a starting position (anywhere in the world)
     * @param pos2 a finishing position (anywhere in the world)
     */
    public void planRoad(MinecraftServer server, BlockPos pos1, BlockPos pos2)
    {

        pos1c = server.getOverworld().getChunk(pos1).getPos().getCenterAtY(server.getOverworld().getSeaLevel());
        BlockPos pos2c = server.getOverworld().getChunk(pos2).getPos().getCenterAtY(server.getOverworld().getSeaLevel());

        xdistance = pos2c.getX()-pos1c.getX();
        zdistance = pos2c.getZ()-pos1c.getZ();

        AllRoads.LOGGER.debug(pos1.toShortString());
        AllRoads.LOGGER.debug(pos2.toShortString());

    }

    public void planNextRoad(MinecraftServer server)
    {
        BlockPos[] next = plannedRoads.remove(0);
        planRoad(server,next[0],next[1]);
    }

    public void findStructures(ServerWorld world, BlockPos about, int amount, TagKey<Structure> structureTagKey)
    {
        foundStructures =new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            AsyncLocator.locate(world,structureTagKey,about,AllRoads.SEARCH_RADIUS,true)
                    .thenOnServerThread(a-> foundStructures.add(a));
        }
    }



    /**
     * finds villages about a point within 1000 blocks
     * @param world
     * @param about
     * @param amount
     */
    public void findVillages(ServerWorld world, BlockPos about,int amount)
    {
        findStructures(world,about,amount,StructureTags.VILLAGE);
    }



    /**
     * assumes you have used findVillages, then creates random routes between them
     * this starts the generator and once one road is finished the next will start
     * @param server
     * @param roadsToPlan
     */
    public void planRoadsUseRandom(MinecraftServer server, int roadsToPlan)
    {
        server.getOverworld().getPlayers().stream().filter(a->a.hasPermissionLevel(2)).forEach(a->a.sendMessage(Text.of("[AllRoads] planning roads, brace for a bit of lag!")));

        plannedRoads=new ArrayList<>();
        for (int i = 0; i < roadsToPlan; i++) {
            int rand1 = server.getOverworld().random.nextInt(foundStructures.size());
            int rand2 = server.getOverworld().random.nextInt(foundStructures.size());
            while (rand1==rand2)
            {
                rand2=server.getOverworld().random.nextInt(foundStructures.size());
            }
            plannedRoads.add(new BlockPos[]{foundStructures.get(rand1), foundStructures.get(rand2)});
        }
        BlockPos[] first = plannedRoads.remove(0);
        AllRoads.LOGGER.info("Next road from: "+first[0]);
        planRoad(server,first[0],first[1]);

    }

    public void makeRoadTick(MinecraftServer server)
    {
        if(!plannedRoads.isEmpty())
        {
            boolean movedZ = false;

            if(xdistance!=0 || zdistance!=0)
            {
                if(xdistance>0)
                {
                    pos1c=pos1c.add(1,0,0);
                    xdistance--;

                }
                else
                {
                    pos1c=pos1c.add(-1,0,0);
                    xdistance++;
                }

                if(zdistance>0)
                {
                    pos1c=pos1c.add(0,0,1);
                    zdistance--;
                    movedZ=true;
                }
                else if(zdistance<0)
                {
                    pos1c=pos1c.add(0,0,-1);
                    zdistance++;
                    movedZ=true;
                }

                //fun fact, world.getTopY(Heightmap.Type, int, int) will just return 0 if the chunk isn't loaded/created
                //((hence why we use this slighltly more complicated line here, which will do it anyway))
                int h1 = server.getOverworld().getChunk(pos1c).sampleHeightmap(HEIGHTMAP, pos1c.getX() & 15, pos1c.getZ() & 15);
                pos1c=new BlockPos(pos1c.getX(),h1,pos1c.getZ());

                //TODO: work out why blocks also just kinda spawn at 127 all the time for some reason...
                if(h1!=127)
                {
                    for (int i = -2; i < 3; i++) {
                        for (int j = -2; j < 3; j++) {

                            if(!server.getOverworld().getBiome(pos1c.add(i,0,j)).isIn(BiomeTags.IS_OCEAN))
                            {
                                if(server.getOverworld().random.nextInt(7)==0)
                                {
                                    if(!server.getOverworld().getBlockState(pos1c.add(i,0,j)).isAir())
                                    {
                                        BlockState state = server.getOverworld().getBlockState(pos1c.add(i,0,j));
                                        if(server.getOverworld().getBlockState(pos1c.add(i,1,j)).isOf(Blocks.SNOW))
                                        {
                                            server.getOverworld().setBlockState(pos1c.add(i,1,j),Blocks.AIR.getDefaultState());
                                        }

                                        if(state.isOf(Blocks.SNOW))
                                        {
                                            server.getOverworld().setBlockState(pos1c.add(i,0,j),Blocks.AIR.getDefaultState());
                                            server.getOverworld().setBlockState(pos1c.add(i,-1,j),Blocks.PACKED_ICE.getDefaultState());
                                        }
                                        if(state.isIn(BlockTags.DIRT))
                                        {
                                            server.getOverworld().setBlockState(pos1c.add(i,0,j),Blocks.DIRT_PATH.getDefaultState());
                                        }
                                        if(state.isIn(BlockTags.SAND) || state.isIn(BlockTags.TERRACOTTA))
                                        {
                                            server.getOverworld().setBlockState(pos1c.add(i,0,j),Blocks.PACKED_MUD.getDefaultState());
                                        }
                                        if(state.isOf(Blocks.GRAVEL))
                                        {
                                            server.getOverworld().setBlockState(pos1c.add(i,0,j),Blocks.COBBLESTONE.getDefaultState());
                                        }
                                    }
                                }
                            }

                        }
                    }
                    switch (server.getOverworld().random.nextInt(100))
                    {
                        //TODO: make this use an API
                        //add some flair, lamps and camps
                        case 7 ->
                        {
                            BlockPos lamp = getRoadsideLocation(server,pos1c,movedZ);
                            if(server.getOverworld().getBlockState(lamp).isOf(Blocks.WATER))
                            {
                                server.getOverworld().setBlockState(lamp,Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.EAST));
                                server.getOverworld().setBlockState(lamp.up(),Blocks.SPRUCE_FENCE.getDefaultState());
                                break;
                            }

                            server.getOverworld().setBlockState(lamp.up(1),Blocks.OAK_FENCE.getDefaultState());
                            server.getOverworld().setBlockState(lamp.up(2),Blocks.OAK_FENCE.getDefaultState());
                            server.getOverworld().setBlockState(lamp.up(3),Blocks.OAK_FENCE.getDefaultState());
                            server.getOverworld().setBlockState(lamp.up(4),Blocks.LANTERN.getDefaultState());

                            AllRoads.LOGGER.debug("lamp at "+lamp.toShortString() +"["+xdistance+","+zdistance+"]");
                        }
                        case 11 ->
                        {
                            BlockPos lamp = getRoadsideLocation(server,pos1c,movedZ);
                            if(server.getOverworld().getBlockState(lamp).isOf(Blocks.WATER))
                            {
                                break;
                            }

                            server.getOverworld().setBlockState(lamp.up(1),Blocks.CAMPFIRE.getDefaultState());
                            server.getOverworld().setBlockState(lamp.east(1).south(2),Blocks.WHITE_WOOL.getDefaultState());
                            server.getOverworld().setBlockState(lamp.east(2).north(1),Blocks.WHITE_WOOL.getDefaultState());
                        }

                    }

                }

            }
            if(xdistance==0 && zdistance==0)
            {
                server.getOverworld().getPlayers().stream().filter(a->a.hasPermissionLevel(2)).forEach(a->a.sendMessage(Text.of("[VillageRoads] finished a road, "+plannedRoads.size()+" left!")));

                if(!plannedRoads.isEmpty())
                {
                   planNextRoad(server);
                }
            }
        }
    }

    /**
     * work out where to put extra roadside features based on how the road construction is moving
     * @param server
     * @param in
     * @param movedZ
     * @return
     */
    private BlockPos getRoadsideLocation(MinecraftServer server,BlockPos in, boolean movedZ)
    {
        int h2;
        BlockPos lamp;
        if(movedZ)
        {
            lamp = new BlockPos(in.getX()+4,0,in.getZ());
        }
        else
        {
            lamp = new BlockPos(in.getX(),0,in.getZ()+4);
        }
        h2 = server.getOverworld().getChunk(lamp).sampleHeightmap(HEIGHTMAP, lamp.getX() & 15, lamp.getZ() & 15);
        lamp=lamp.up(h2);
        return lamp;
    }

    public int countVillages() {
        return foundStructures.size();
    }

    public void addRoadPlan(BlockPos[] poss)
    {
        if(plannedRoads!=null)
        {
            plannedRoads.add(poss);
        }
    }
}