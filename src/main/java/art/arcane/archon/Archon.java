package art.arcane.archon;

import art.arcane.archon.server.ArchonServer;
import art.arcane.quill.logging.L;

public class Archon
{
    public static void main(String[] a)
    {
        new ArchonServer();
        L.flush();
        System.exit(0);
    }
}
