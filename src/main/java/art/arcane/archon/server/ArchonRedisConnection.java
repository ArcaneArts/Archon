package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonRedisConfiguration;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.execution.J;
import art.arcane.quill.io.IO;
import art.arcane.quill.logging.L;
import lombok.Getter;
import redis.clients.jedis.Jedis;

public class ArchonRedisConnection implements ArchonConnection {
    private static int creates = 1;

    @Getter
    private final String name;

    @Getter
    private boolean connected;

    @Getter
    private Jedis jedis;
    private final ArchonRedisConfiguration config;

    public ArchonRedisConnection(ArchonRedisConfiguration config)
    {
        this.config = config;
        name = "redis:" + config.hashCode() + ":" + creates++;
        connected = false;
    }

    public int invalidate(String key) {
        if(key.contains("*"))
        {
           return jedis.del(jedis.keys(key + "*").toArray(new String[0])).intValue();
        }

        return jedis.del(key).intValue();
    }

    public void invalidateCachedQuery(String key)
    {
        jedis.del(key);
    }

    public void invalidateCachedQueriesPrefixedWith(String key)
    {
        jedis.del(jedis.keys(key + "*").toArray(new String[0]));
    }

    public void cacheQuery(String key, ArchonResult result)
    {
        jedis.set(key, result.toJSON());
    }

    public ArchonResult getCachedQuery(String key)
    {
        try
        {
            String v = jedis.get(key);

            if(v == null || v.length() < 2)
            {
                return null;
            }

            return ArchonResult.fromJson(v);
        }

        catch(Throwable e)
        {

        }

        return null;
    }

    @Override
    public void disconnect() {
        if(jedis != null)
        {
            J.attempt(() -> jedis.disconnect());
            jedis = null;
        }

        connected = false;
    }

    @Override
    public void connect() {
        jedis = new Jedis(config.getAddress(), config.getPort());
        L.i("[" + getName() + "]: Connecting to " + config.getAddress() + ":" + config.getPort());
        jedis.connect();
        if(config.isUsePassword())
        {
            L.i("[" + getName() + "]: Authenticating with password...");
            jedis.auth(config.getPassword());
        }

        if(jedis.isConnected())
        {
            L.i("[" + getName() + "]: Redis Connection Active!");
            connected = true;
        }

        else
        {
            L.f("[" + getName() + "]: Redis Connection Failure!");
            disconnect();
        }
    }
}
