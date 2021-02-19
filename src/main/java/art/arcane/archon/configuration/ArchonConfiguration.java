package art.arcane.archon.configuration;

import art.arcane.quill.collections.KList;
import art.arcane.quill.io.IO;
import art.arcane.quill.json.JSONObject;
import art.arcane.quill.logging.L;
import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;

import java.io.File;

@Data
public class ArchonConfiguration
{
    private int socketPort = 28435;
    private KList<ArchonSQLConfiguration> sqlConnections = KList.from(new ArchonSQLConfiguration());
    private KList<ArchonRedisConfiguration> redisConnections = KList.from(new ArchonRedisConfiguration());
    private static ArchonConfiguration config;

    public static ArchonConfiguration get()
    {
        if(config != null)
        {
            return config;
        }

        config = new ArchonConfiguration();

        try
        {
            File f = new File("config.json");

            if(f.exists())
            {
                config = new Gson().fromJson(IO.readAll(f), ArchonConfiguration.class);
            }

            else
            {
                IO.writeAll(f, new JSONObject(new Gson().toJson(config)).toString(4));
            }
        }

        catch(Throwable e)
        {
            L.f("There was a problem loading the config.json. Assuming defaults (which wont really work)");
            L.ex(e);
        }

        return config;
    }
}
