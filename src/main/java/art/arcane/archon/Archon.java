package art.arcane.archon;

import art.arcane.archon.data.ArchonResult;
import art.arcane.archon.server.ArchonServer;
import art.arcane.quill.logging.L;

public class Archon
{

    public static ArchonResult query(String query)
    {
        return ArchonServer.get().access().query(query);
    }

    public static int update(String query)
    {
        return ArchonServer.get().access().update(query);
    }

    public static void main(String[] a)
    {
        ArchonServer.get().shutdown();
        L.flush();
        System.exit(0);
    }
}
