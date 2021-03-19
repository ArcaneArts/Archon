package art.arcane.archon;

import art.arcane.archon.server.ArchonService;
import art.arcane.quill.Quill;
import art.arcane.quill.service.QuillService;
import art.arcane.quill.service.Service;
import lombok.Getter;

public class ArchonTestService extends QuillService {
    @Service
    @Getter
    private ArchonService database = new ArchonService();

    public static void startTestService()
    {
        Quill.start(new String[0]);
    }

    @Override
    public void onEnable() {
        i("Archon Test Service Started");
    }

    @Override
    public void onDisable() {
        i("Archon Test Service Stopped");
    }
}
