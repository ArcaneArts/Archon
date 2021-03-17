package art.arcane.archon;

import art.arcane.archon.server.ArchonService;
import art.arcane.quill.logging.L;

public class Archon
{
    public static ArchonService defaultService = null;

    public static void main(String[] a)
    {
        new ArchonService().shutdown();
        L.flush();
        System.exit(0);
    }
}
