package ace.actually.allroads;


import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class AllRoads implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("AllRoads");

	public static int TICK_SKIP = 1;
	public static int VILLAGE_COUNT = 10;
	public static int ROAD_COUNT = 5;
	public static int SEARCH_RADIUS = 1000;
	public static final RoadGenerator ROAD_GENERATOR = new RoadGenerator();
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ConfigUtils.checkConfigs();
		TICK_SKIP = Integer.parseInt(ConfigUtils.config.get("tickskip"));
		VILLAGE_COUNT = Integer.parseInt(ConfigUtils.config.get("villagecount"));
		ROAD_COUNT = Integer.parseInt(ConfigUtils.config.get("roadcount"));
		SEARCH_RADIUS = Integer.parseInt(ConfigUtils.config.get("searchradius"));

		CommandRegistrationCallback.EVENT.register((dispatcher, phase, registrationEnvironment) -> dispatcher.register(

				literal("allroads")
						.requires(source -> source.hasPermissionLevel(2))
						.then(literal("villages")

								.then(argument("villagesToFind", IntegerArgumentType.integer())
										.executes(context -> {
											AllRoads.ROAD_GENERATOR.findVillages(context.getSource().getWorld(),context.getSource().getEntity().getBlockPos(), IntegerArgumentType.getInteger(context,"villagesToFind"));
											return 1;
										})))
						.then(literal("roads")

								.then(argument("roadsToPlan", IntegerArgumentType.integer())
										.executes(context -> {
											AllRoads.ROAD_GENERATOR.planRoadsUseRandom(context.getSource().getServer(), IntegerArgumentType.getInteger(context,"roadsToPlan"));
											return 1;
										})))
		));

		LOGGER.debug("May all your roads lead to rome!");
	}
}