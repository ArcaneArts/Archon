package art.arcane.archon;

import art.arcane.quill.Quill;
import art.arcane.quill.execution.J;
import art.arcane.quill.logging.L;
import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.junit.MariaDB4jRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArchonTests {
    private ArchonTestService testService;
    private DB db;

    @BeforeAll
    public void setupArchonService()
    {
        L.i("Configuring Archon Services for Testing...");
        L.i("Starting Embedded MariaDB on 3309");
        try {
            db = DB.newEmbeddedDB(3309);
            db.start();
            //Archon.forceConnection = DriverManager.getConnection(db.getConfiguration().getURL("test"), "root", "");
            L.i("Starting Archon Test Service");
            ArchonTestService.startTestService();
            testService = (ArchonTestService) Quill.delegate;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNewObjectSetWithoutExsistingTable() {
        RandomTableObject rt = RandomTableObject.builder()
                .value("SomeNewObject")
                .build();
        rt.push();
        assertTrue(rt.exists(), "The table was not created during push when it diddnt exist before. This should have been done automatically.");
        assertTrue(rt.delete(), "The table was not properly deleted for some reason.");
        assertTrue(rt.dropTable(rt.getDropRequest()), "Failed to drop table");
    }

    @Test
    public void testTableAlterAddColumn() {
        AlterOne rt = AlterOne.builder()
                .value("SomeNewObject")
                .build();
        rt.push();
        assertTrue(rt.exists(), "The table was not created during push when it diddnt exist before. This should have been done automatically.");
        rt.bruteForceUnregister();

        AlterTwo rt2 = AlterTwo.builder()
                .id(rt.getId())
                .build();
        rt2.pull();
        assertEquals(rt2.getValue2(), 34, "Not 34");
    }

    @Test
    public void testTableAlterDropColumn() {
        AlterTwo rt = AlterTwo.builder()
                .value("SomeNewObject")
                .build();
        rt.push();
        assertTrue(rt.exists(), "The table was not created during push when it diddnt exist before. This should have been done automatically.");
        rt.bruteForceUnregister();

        AlterOne rt2 = AlterOne.builder()
                .id(rt.getId())
                .build();
        rt2.pull();
    }

    @AfterAll
    public void shutdownArchonService() {
        L.v("Shutting down test service");
        Quill.shutdown();

        L.i("Shuttind down Embedded MariaDB Process...");
        try {
            db.stop();
        } catch (ManagedProcessException e) {
            e.printStackTrace();
        }
    }
}
