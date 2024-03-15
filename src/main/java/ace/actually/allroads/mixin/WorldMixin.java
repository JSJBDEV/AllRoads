package ace.actually.allroads.mixin;

import ace.actually.allroads.AllRoads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class WorldMixin {

	@Shadow @Final private MinecraftServer server;

	@Inject(at = @At("HEAD"), method = "tickTime")
	private void time(CallbackInfo ci) {

		//if we have some villages found, then we can assume we should probably be generating roads
		//the found villages ARE NOT PERSISTENT, so this only triggers at 10 ticks or if the command is called
		//if the server closes down, the generation just stops
		if(AllRoads.ROAD_GENERATOR.countVillages()!=0)
		{
			if(AllRoads.TICK_SKIP==1)
			{
				AllRoads.ROAD_GENERATOR.makeRoadTick(server);
			}
			else if (server.getOverworld().getTimeOfDay()% AllRoads.TICK_SKIP==0)
			{
				AllRoads.ROAD_GENERATOR.makeRoadTick(server);
			}

		}
		//give the server 10 ticks to do ***things***, then find VILLAGE_COUNT villages
		if(server.getOverworld().getTime()==10)
		{
			AllRoads.ROAD_GENERATOR.findVillages(server.getOverworld(),server.getOverworld().getSpawnPos(), AllRoads.VILLAGE_COUNT);
			AllRoads.ROAD_GENERATOR.roadLatch=true;
		}
		//when we have found all the villages (at some point) plan some roads
		if(AllRoads.ROAD_GENERATOR.countVillages()== AllRoads.VILLAGE_COUNT && AllRoads.ROAD_GENERATOR.roadLatch)
		{
			AllRoads.ROAD_GENERATOR.roadLatch=false;
			AllRoads.ROAD_GENERATOR.planRoadsUseRandom(server, AllRoads.ROAD_COUNT);
		}


	}




}
