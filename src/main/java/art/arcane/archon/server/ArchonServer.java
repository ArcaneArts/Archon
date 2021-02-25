package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonConfiguration;
import art.arcane.archon.configuration.ArchonRedisConfiguration;
import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.element.ExampleTable;
import art.arcane.archon.element.ID;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.RoundRobin;
import art.arcane.quill.execution.J;
import art.arcane.quill.execution.parallel.BurstExecutor;
import art.arcane.quill.execution.parallel.MultiBurst;
import art.arcane.quill.logging.L;

public class ArchonServer {
    private static ArchonServer instance;
    private RoundRobin<ArchonSQLConnection> readOnlySQLConnections;
    private RoundRobin<ArchonSQLConnection> writeSQLConnections;
    private RoundRobin<ArchonRedisConnection> redisConnections;
    private final Edict edict;

    public ArchonServer()
    {
        instance = this;
        L.i("Starting Archon Server");
        initSQLConnections();
        initRedisConnections();
        edict = new Edict(this);
        L.flush();
        L.i("============== Archon ==============");
        L.i("SQL: ");
        L.i("  Writers: ");
        writeSQLConnections.list().forEach((i) -> L.i("    " + i.getName()));
        L.i("  Readers: ");
        readOnlySQLConnections.list().forEach((i) -> L.i("    " + i.getName()));
        L.i("  Caches: ");
        redisConnections.list().forEach((i) -> L.i("    " + i.getName()));
        L.flush();
        L.i("====================================");
        L.flush();
        test();
    }

    private void test() {
        new ExampleTable().sync();

        ExampleTable t = new ExampleTable();
        t.setId(new ID());
        t.setShortName("TestName");
        t.setLongerName("A Longer Test Name.");
        t.push();
    }

    public static ArchonServer get()
    {
        if(instance == null)
        {
            instance = new ArchonServer();
        }

        return instance;
    }

    public void shutdown()
    {
        ArchonServer.get().access().shutdown();
        redisConnections.list().forEach(ArchonConnection::disconnect);
        writeSQLConnections.list().forEach(ArchonConnection::disconnect);
        readOnlySQLConnections.list().forEach(ArchonConnection::disconnect);
        L.i("Archon has Shutdown");
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
        KList<ArchonSQLConnection> readOnly = new KList<>();
        KList<ArchonSQLConnection> write = new KList<>();
        for(ArchonSQLConfiguration i : conf)
        {
            if(i.isReadOnly())
            {
                ArchonSQLConnection c = new ArchonSQLConnection(i);
                c.connect();
                readOnly.add(c);
            }
            
            else
            {
                ArchonSQLConnection c = new ArchonSQLConnection(i);
                c.connect();
                write.add(c);
            }
        }
        
        writeSQLConnections = write.roundRobin();
        readOnlySQLConnections = readOnly.roundRobin();
    }

    private void initRedisConnections()
    {
        KList<ArchonRedisConfiguration> conf = ArchonConfiguration.get().getRedisConnections();
        KList<ArchonRedisConnection> redis = new KList<>();
        for(ArchonRedisConfiguration i : conf)
        {
            ArchonRedisConnection c = new ArchonRedisConnection(i);
            c.connect();
            redis.add(c);
        }

        redisConnections = redis.roundRobin();
    }
}
