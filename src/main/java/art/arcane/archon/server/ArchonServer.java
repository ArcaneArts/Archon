package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonConfiguration;
import art.arcane.archon.configuration.ArchonRedisConfiguration;
import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.element.ExampleTable;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.RoundRobin;
import art.arcane.quill.execution.parallel.BurstExecutor;
import art.arcane.quill.execution.parallel.MultiBurst;
import art.arcane.quill.logging.L;

public class ArchonServer {
    private RoundRobin<ArchonSQLConnection> readOnlySQLConnections;
    private RoundRobin<ArchonSQLConnection> writeSQLConnections;
    private RoundRobin<ArchonRedisConnection> redisConnections;
    private final Edict edict;

    public ArchonServer()
    {
        L.i("Starting Archon Server");
        MultiBurst.burst.burst(
            this::initSQLConnections,
            this::initRedisConnections
        );
        edict = new Edict(this);
        L.flush();
        L.i("====================================");
        L.i("SQL: ");
        L.i("  Writers: ");
        writeSQLConnections.list().forEach((i) -> L.i("    " + i.getName()));
        L.i("  Readers: ");
        readOnlySQLConnections.list().forEach((i) -> L.i("    " + i.getName()));
        L.i("Redis: ");
        redisConnections.list().forEach((i) -> L.i("  " + i.getName()));
        L.flush();
        L.i("====================================");
        L.flush();
    }

    public Edict access()
    {
        return edict;
    }

    public ArchonRedisConnection getRedisConnection()
    {
        return redisConnections.next();
    }

    public ArchonSQLConnection getReadSQLConnection()
    {
        ArchonSQLConnection v = readOnlySQLConnections.next();
        return v == null ? getWriteSQLConnection() : v;
    }

    public ArchonSQLConnection getWriteSQLConnection()
    {
        return writeSQLConnections.next();
    }

    private void initSQLConnections()
    {
        KList<ArchonSQLConfiguration> conf = ArchonConfiguration.get().getSqlConnections();
        BurstExecutor e = MultiBurst.burst.burst(conf.size());
        KList<ArchonSQLConnection> readOnly = new KList<>();
        KList<ArchonSQLConnection> write = new KList<>();
        for(ArchonSQLConfiguration i : conf)
        {
            if(i.isReadOnly())
            {
                e.queue(() -> {
                    ArchonSQLConnection c = new ArchonSQLConnection(i);
                    c.connect();
                    readOnly.add(c);
                });
            }
            
            else
            {
                e.queue(() -> {
                    ArchonSQLConnection c = new ArchonSQLConnection(i);
                    c.connect();
                    write.add(c);
                });
            }
        }
        
        writeSQLConnections = write.roundRobin();
        readOnlySQLConnections = readOnly.roundRobin();
        e.complete();
    }

    private void initRedisConnections()
    {
        KList<ArchonRedisConfiguration> conf = ArchonConfiguration.get().getRedisConnections();
        BurstExecutor e = MultiBurst.burst.burst(conf.size());
        KList<ArchonRedisConnection> redis = new KList<>();
        for(ArchonRedisConfiguration i : conf)
        {
            e.queue(() -> {
                ArchonRedisConnection c = new ArchonRedisConnection(i);
                c.connect();
                redis.add(c);
            });
        }

        redisConnections = redis.roundRobin();
        e.complete();
    }
}
