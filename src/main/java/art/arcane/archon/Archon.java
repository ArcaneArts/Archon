package art.arcane.archon;

import art.arcane.archon.data.ArchonResult;
import art.arcane.archon.server.ArchonServer;
import art.arcane.quill.logging.L;

public class Archon
{
    public static ArchonResult query(String cacheKey, String query)
    {
        return ArchonServer.get().access().query(cacheKey, query);
    }

    public static ArchonResult query(String query)
    {
        return ArchonServer.get().access().query(query);
    }

    public static int update(String cacheKey, String query)
    {
        return ArchonServer.get().access().update(cacheKey, query);
    }

    public static int update(String query)
    {
        return ArchonServer.get().access().update(query);
    }

    public static void main(String[] a)
    {
        new ArchonServer();
        L.flush();
        System.exit(0);
    }
}
