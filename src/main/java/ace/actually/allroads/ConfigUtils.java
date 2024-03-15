package ace.actually.allroads;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConfigUtils {

    public static Map<String,String> config = new HashMap<>();

    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toString() + "/VillageRoads/config.acfg");

    public static Map<String,String> loadConfigs()
    {

        try {
            List<String> lines = FileUtils.readLines(CONFIG_FILE,"utf-8");
            lines.forEach(line->
            {
                if(line.charAt(0)!='#')
                {
                    String noSpace = line.replace(" ","");
                    String[] entry = noSpace.split("=");
                    config.put(entry[0],entry[1]);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static void generateConfigs(List<String> input)
    {
        try {
            FileUtils.writeLines(CONFIG_FILE,input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkConfigs()
    {
        if(CONFIG_FILE.exists())
        {
            loadConfigs();
            return;
        }
        generateConfigs(makeDefaults());
        loadConfigs();
    }

    private static List<String> makeDefaults()
    {
        List<String> defaults = new ArrayList<>();


        defaults.add("#tick skipping for road generation, might make the game more playable on bigger packs");
        defaults.add("tickskip=1");
        defaults.add("#amount of villages to find for plotting roads between");
        defaults.add("villagecount=10");
        defaults.add("#amount of roads to generate");
        defaults.add("roadcount=5");
        defaults.add("#structure search radius");
        defaults.add("searchradius=1000");
        return defaults;
    }

}
