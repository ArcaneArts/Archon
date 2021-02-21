package art.arcane.archon.server;

import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.execution.ChronoLatch;
import art.arcane.quill.execution.parallel.MultiBurst;
import lombok.Data;
import redis.clients.jedis.Jedis;

@Data
public class Edict
{
    private final ChronoLatch cl = new ChronoLatch(60000);
    private long _cacheHit = 0;
    private long _invalidations = 0;
    private long _cacheWrites = 0;
    private long _dbHit = 0;
    private long cacheHit = 0;
    private long invalidations = 0;
    private long cacheWrites = 0;
    private long dbHit = 0;
    private final ArchonServer server;

    public Edict(ArchonServer server)
    {
        this.server = server;
    }

    public ArchonResult query(String q)
    {
        return server.getReadSQLConnection().query(q);
    }

    public ArchonResult query(String cacheKey, String q)
    {
        if(cl.flip())
        {
            MultiBurst.burst.lazy(this::updateMetrics);
        }

        ArchonResult r = getServer().getRedisConnection().getCachedQuery(cacheKey);

        if(r != null)
        {
            _cacheHit++;
            return r;
        }

        _dbHit++;
        ArchonResult rr = query(q);

        if(rr != null)
        {
            MultiBurst.burst.lazy(() -> getServer().getRedisConnection().cacheQuery(cacheKey, rr));
            _cacheWrites++;
        }

        return rr;
    }

    private long increment(String v, long amt)
    {Jedis r = getServer().getRedisConnection().getJedis();
        long val = 0;

        try
        {
            val = Long.parseLong(r.get("archon:metrics:" + v));
        }

        catch(Throwable ignored)
        {

        }

        r.set("archon:metrics:" + v, "" + (val + amt));
        return (val + amt);
    }

    public int update(String q)
    {
        return server.getWriteSQLConnection().update(q);
    }

    public int update(String cacheKey, String q)
    {
        if(cl.flip())
        {
            MultiBurst.burst.lazy(this::updateMetrics);
        }

        MultiBurst.burst.lazy(() -> _invalidations += getServer().getRedisConnection().invalidate(cacheKey));
        return update(q);
    }

    private void updateMetrics() {
        MultiBurst.burst.lazy(() -> {
            cacheHit = increment("cachehit", _cacheHit);
            _cacheHit = 0;
        });
        MultiBurst.burst.lazy(() -> {
            cacheWrites = increment("cachewrite", _cacheWrites);
            _cacheWrites = 0;
        });
        MultiBurst.burst.lazy(() -> {
            dbHit = increment("dbhit", _dbHit);
            _dbHit = 0;
        });
        MultiBurst.burst.lazy(() -> {
            invalidations = increment("invalidations", _invalidations);
            _invalidations = 0;
        });
    }
}
