package art.arcane.archon;

import art.arcane.archon.element.ElementList;
import art.arcane.archon.table.*;
import art.arcane.quill.Quill;
import art.arcane.quill.collections.ID;
import art.arcane.quill.collections.KList;
import art.arcane.quill.logging.L;
import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.DriverManager;

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
            Quill.testMode = true;
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
    public void testQueryLists()
    {
        ExampleUser2 ee = ExampleUser2.builder().build();
        ee.sync();
        ee.dropTable(ee.getDropRequest());
        ee.sync();

        for(int i = 0; i < 256; i++)
        {
            ExampleUser2.builder()
                    .firstName("First" + i)
                    .lastName(i + "lastt")
                    .someId(i*4)
                    .email((i*i) + "@email" + i + ".com")
                    .build().push();
        }

        ElementList<ExampleUser2> e = ElementList.where(Archon.defaultService, ExampleUser2.class, "1", "someId", true);
        assertEquals(e.size(), 256, "Invalid Element List Size?");
        assertEquals(e.get(3).getEmail(), "9@email3.com");
        assertEquals(ElementList.whereField(Archon.defaultService, ExampleUser2.class, "email", "'16@email4.com'").size(), 1, "Invalid list size should be one");
        assertEquals(ElementList.where(Archon.defaultService, ExampleUser2.class, "(`someId` / 4) % 2 = 0").size(), 128, "Invalid size");
        assertEquals(new ExampleUser2().allWhere("1").size(), 256, "Invalid count");
    }

    @Test
    public void testReferences()
    {
        for(int i = 0; i < 3; i++)
        {
            ParentRefTest r = new ParentRefTest();
            r.setEmail(i + "@email.com");
            r.setFirstName("First"+i);
            r.setLastName("Last"+i);
            r.getChildRef().commit(ChildRefTest.builder()
                    .pepper("pepper" + i)
                    .salt("salt" + i)
                    .password("" + i)
                    .build());
            r.push();
        }

        ParentRefTest p = new ParentRefTest();
        assertTrue(p.where("email", "0@email.com"), "Cannot get 0email on reference test!");
        assertEquals(p.getChildRef().get().getPepper(), "pepper0", "Pepper does not equal!");

        for(int i = 3; i < 6; i++)
        {
            ChildRefTest c = ChildRefTest.builder()
                    .pepper("pepper" + i)
                    .salt("salt" + i)
                    .password("" + i)
                    .build().push();
            ParentRefTest r = new ParentRefTest();
            r.setEmail(i + "@email.com");
            r.setFirstName("First"+i);
            r.setLastName("Last"+i);
            r.getChildRef().set(c);
            r.push();
        }

        ParentRefTest p2 = new ParentRefTest();
        assertTrue(p2.where("email", "5@email.com"), "Cannot get 5email on reference test!");
        assertEquals(p2.getChildRef().get().getPepper(), "pepper5", "Pepper does not equal!");

    }

    @Test
    public void testSelectIdWhere() {
        ExampleUser ee = ExampleUser.builder().build();
        ee.sync();
        ee.dropTable(ee.getDropRequest());
        ee.sync();

        KList<ID> ids = new KList<>();
        for(int i = 0; i < 32; i++)
        {
            ids.add(new ID());
            ExampleUser.builder()
                    .id(ids.get(i))
                    .email(i + "@email.com")
                    .firstName("First"+i)
                    .lastName("Last"+i)
                    .build()
                    .push();
        }

        ID realId = ids.get(7);
        String email = "7@email.com";
        String firsTName = "First7";

        ID test7 = ExampleUser.builder().build().getIdentityWhere("email", email);
        assertEquals(realId, test7, "Wrong ID!");
    }

    @Test
    public void testSelectWhere() {
        KList<ID> ids = new KList<>();
        for(int i = 0; i < 32; i++)
        {
            ids.add(new ID());
            ExampleUser.builder()
                    .id(ids.get(i))
                    .email(i + "@email2.com")
                    .firstName("First"+i)
                    .lastName("Last"+i)
                    .build()
                    .push();
        }

        ID realId = ids.get(9);
        String email = "9@email2.com";
        String firsTName = "First9";
        ExampleUser u = ExampleUser.builder().build();
        L.v("EMAIL is " + email + " Data is ", u);
        assertTrue(u.where("email", email));

        assertEquals(firsTName, "First9", "Bad Where Get");
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

    @Test
    public void testGetWhere() {

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
