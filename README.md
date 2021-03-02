# Archon
Enhanced Database server with efficient object types for java

[![Release](https://jitpack.io/v/ArcaneArts/Archon.svg)](https://jitpack.io/#ArcaneArts/Archon)

```groovy
maven { url 'https://jitpack.io' }
```

```groovy
implementation 'com.github.ArcaneArts:Archon:latest.release'
```

## Usage

You need to use a [Quill Service](https://github.com/ArcaneArts/Quill#standalone-server-bootstrap) to use Archon.

```java
import art.arcane.quill.Quill;
import art.arcane.quill.logging.L;
import art.arcane.archon.server.ArchonServiceWorker;

public class TestService extends QuillService
{
    public static void main(String[] a)
    {
        Quill.start(a);
    }

    @ServiceWorker
    private ArchonServiceWorker archon;

    public TestService() {
        super("TestSVC");
    }

    @Override
    public void onEnable() {
        L.v("Enabled! (Archon is already running at this point)");
    }

    @Override
    public void onDisable() {
        L.v("Disabled!");
    }
}
```

Configure Archon in the service worker json file for your service.
