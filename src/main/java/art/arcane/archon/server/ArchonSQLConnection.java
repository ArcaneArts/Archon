package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.execution.J;
import art.arcane.quill.logging.L;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ArchonSQLConnection implements ArchonConnection {
    private static int creates = 1;

    @Getter
    private final String name;

    @Getter
    private boolean connected;

    private Connection sql;
    private final ArchonSQLConfiguration config;

    public ArchonSQLConnection(ArchonSQLConfiguration config)
    {
        this.config = config;
        name = "sql:" + config.hashCode() + ":" + creates++;
        connected = false;
    }

    @Override
    public void disconnect() {
        if(sql != null)
        {
            J.attempt(() -> sql.close());
            sql = null;
        }

        connected = false;
    }

    @Override
    public void connect() {
        J.attempt(() -> Class.forName(config.getDriver()).newInstance());
        Properties p = new Properties();
        p.setProperty("user", config.getUsername());

        if(config.isUsesPassword())
        {
            p.setProperty("password", config.getPassword());
        }

        try {

            String url = "jdbc:mysql://" + config.getAddress() + (config.getPort() != 3306 ? (":" + config.getPort()) : "") + "/" + config.getDatabase();
            L.i("[" + getName() + "]: Connecting to " + url);
            sql = DriverManager.getConnection(url, p);

            if(sql.isValid(5))
            {
                L.i("[" + getName() + "]: Database Connection Active!");
                connected = true;
                return;
            }
        } catch (Throwable e) {
            L.f("[" + getName() + "]: Database Connection Failure!");
            L.ex(e);
        }

        disconnect();
    }

    public ArchonResult query(String query)
    {
        try {
            ArchonResult res = new ArchonResult(sql.prepareStatement(query).executeQuery());
            L.v("[SQL Query]: " + query + " -> " + res.toJSON());
            return res;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            L.v("[REQ?]: " + query);
        }

        return null;
    }

    public int update(String query)
    {
        try {
            int v = sql.prepareStatement(query).executeUpdate();
            L.v("[SQL Update]: " + query + " -> " + v + " rows affected");
            return v;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            L.v("[REQ?]: " + query);
        }

        return -1;
    }
}
