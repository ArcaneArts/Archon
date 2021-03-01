package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonConfiguration;
import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.element.*;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.RoundRobin;
import art.arcane.quill.format.Form;
import art.arcane.quill.logging.L;
import art.arcane.quill.math.Profiler;

public class ArchonServer {
    private static ArchonServer instance;
    private RoundRobin<ArchonSQLConnection> readOnlySQLConnections;
    private RoundRobin<ArchonSQLConnection> writeSQLConnections;
    private final Edict edict;

    public ArchonServer()
    {
        instance = this;
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
        test();
    }

    private void test() {
        L.v(">- === TESTS === -<");
        L.v("References!");
        L.v(">- ===  ===  === -<");
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
}
