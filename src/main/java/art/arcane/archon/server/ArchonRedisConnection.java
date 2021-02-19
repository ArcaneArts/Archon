package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonRedisConfiguration;
import art.arcane.quill.execution.J;
import lombok.Getter;
import redis.clients.jedis.Jedis;

public class ArchonRedisConnection implements ArchonConnection {
    private static int creates = 1;

    @Getter
    private final String name;

    @Getter
    private boolean connected;

    private Jedis jedis;
    private final ArchonRedisConfiguration config;

    public ArchonRedisConnection(ArchonRedisConfiguration config)
    {
        this.config = config;
        name = "redis:" + config.hashCode() + ":" + creates++;
        connected = false;
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
        jedis.connect();
        if(config.isUsePassword())
        {
            jedis.auth(config.getPassword());
        }

        if(jedis.isConnected())
        {
            connected = true;
        }

        else
        {
            disconnect();
        }
    }
}
