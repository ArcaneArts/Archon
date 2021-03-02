package art.arcane.archon;

import art.arcane.archon.server.ArchonServiceWorker;
import art.arcane.quill.logging.L;

public class Archon
{
    public static void main(String[] a)
    {
        new ArchonServiceWorker().shutdown();
        L.flush();
        System.exit(0);
    }
}
