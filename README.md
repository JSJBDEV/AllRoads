# All Roads
Generate roads between structures.



## What?
by default, AllRoads will find 10 villages and generate 4 roads.
The roads are obvious over land, but still have waypoints over water

## Other Structures?
In general, any structure that is in a structure tag can have a
road generated between it

To invoke a road generation, you must first add a "road plan" using `AllRoads.ROAD_GENERATOR.addRoadPlan(BlockPos[] plan)`

a road plan is simply a 2-length array of BlockPos, being the start and end positions (of structures)

after adding all your road plans, you can use the method `AllRoads.ROAD_GENERATOR.planNextRoad(MinecraftServer server)`

This will then continue to generate roads until there are none left.

This mod requires [Async Locator](https://modrinth.com/mod/async-locator). if your going to use your own structures,
I'd recommend using it as well