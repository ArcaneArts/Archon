package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonRedisConfiguration;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.execution.J;
import art.arcane.quill.execution.parallel.MultiBurst;
import art.arcane.quill.io.IO;
import art.arcane.quill.logging.L;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        synchronized (this)
        {
            AtomicInteger i = new AtomicInteger();
            L.v("[Redis DEL]: " + key );
            if(key.contains("*"))
            {
                return jedis.del(jedis.keys(key + "*").toArray(new String[0])).intValue();
            }

            return jedis.del(key).intValue();
        }
    }

    public void invalidateCachedQuery(String key)
    {
       synchronized (this) {
           L.v("[Redis DEL]: " + key);
           jedis.del(key);
       }
    }

    public void invalidateCachedQueriesPrefixedWith(String key)
    {
        synchronized (this)
        {
            L.v("[Redis DEL]: " + key );
            jedis.del(jedis.keys(key + "*").toArray(new String[0]));
        }
    }

    public void cacheQuery(String key, ArchonResult result)
    {
        set(key, result.toJSON());
    }

    public void set(String key, String value)
    {
       synchronized (this)
       {
           L.v("[Redis SET]: " + key + " -> " + value);
           jedis.set(key, value);
       }
    }

    public String get(String key)
    {
        synchronized (this)
        {
            AtomicReference<String> r = new AtomicReference<>();

            L.v("[Redis GET]: " + key);
            r.set(jedis.get(key));
            return r.get();
        }
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
        synchronized (this)
        {
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

                String v = jedis.get("Derp");

                connected = true;
            }

            else
            {
                L.f("[" + getName() + "]: Redis Connection Failure!");
                disconnect();
            }
        }
    }
}
