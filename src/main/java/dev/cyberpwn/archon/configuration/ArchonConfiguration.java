package dev.cyberpwn.archon.configuration;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import ninja.bytecode.shuriken.collections.KList;
import ninja.bytecode.shuriken.io.IO;
import ninja.bytecode.shuriken.json.JSONObject;
import ninja.bytecode.shuriken.logging.L;

import java.io.File;

public class ArchonConfiguration
{
    @Getter
    private int socketPort = 28435;

    @Getter
    private KList<ArchonSQLConfiguration> sqlConnections = KList.from(new ArchonSQLConfiguration());

    @Getter
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
