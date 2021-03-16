package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.Quill;
import art.arcane.quill.collections.ID;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.RoundRobin;
import art.arcane.quill.logging.L;
import art.arcane.quill.service.QuillServiceWorker;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ArchonServiceWorker extends QuillServiceWorker {
    private KList<ArchonSQLConfiguration> sqlConnections = KList.from(new ArchonSQLConfiguration());
    private transient RoundRobin<ArchonSQLConnection> readOnlySQLConnections;
    private transient RoundRobin<ArchonSQLConnection> writeSQLConnections;
    private transient Edict edict;

    public ArchonServiceWorker()
    {

    }

    public static void fixClass(Class<?> v)
    {

    }

    @Override
    public void onEnable() {
        L.f("STARTING ARCHON ID " + new ID().toString() + "...");

        try
        {
            fixClass(com.mysql.jdbc.Driver.class);
            fixClass(com.mysql.cj.conf.url.SingleConnectionUrl.class);
        }

        catch(Throwable e)
        {
            L.ex(e);
            Quill.crashStack("Failed to load sql driver...");
        }
        try
        {
            L.i("Starting Archon Server");
            initSQLConnections();
            edict = new Edict(this);
            L.flush();
            L.i("============== Archon ==============");
            L.i("SQL: ");
            L.i("  Writers: ");
            writeSQLConnections.list().forEach((i) -> L.i("    " + i.getName()));
            L.i("  Readers: ");
            readOnlySQLConnections.list().forEach((i) -> L.i("    " + i.getName()));
            L.flush();
            L.i("====================================");
            L.flush();
        }

        catch(Throwable e)
        {
            L.ex(e);
            Quill.crashStack("Failed to start Archon");
        }

        L.i("EDICT " + edict != null );
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    public ArchonResult query(String query)
    {
        return access().query(query);
    }

    public int update(String query)
    {
        return access().update(query);
    }

    public void shutdown()
    {
        writeSQLConnections.list().forEach(ArchonConnection::disconnect);
        readOnlySQLConnections.list().forEach(ArchonConnection::disconnect);
        L.i("Archon has Shutdown");
    }

    public Edict access()
    {
        return edict;
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
        KList<ArchonSQLConfiguration> conf = getSqlConnections();
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
}
