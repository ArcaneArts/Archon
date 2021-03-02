package art.arcane.archon.server;

import art.arcane.archon.data.ArchonResult;
import lombok.Data;

@Data
public class Edict
{
    private final ArchonServiceWorker server;

    public Edict(ArchonServiceWorker server)
    {
        this.server = server;
    }

    public ArchonResult query(String q)
    {
        return server.getReadSQLConnection().query(q);
    }

    public int update(String q)
    {
        return server.getWriteSQLConnection().update(q);
    }
}
