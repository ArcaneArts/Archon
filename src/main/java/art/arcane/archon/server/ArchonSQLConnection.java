package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.quill.execution.J;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
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
            sql = DriverManager.getConnection("jdbc:mysql://" + config.getAddress() + (config.getPort() != 3306 ? (":" + config.getPort()) : "") + "/" + config.getDatabase(), p);

            if(sql.isValid(5))
            {
                connected = true;
                return;
            }
        } catch (Throwable ignored) {

        }

        disconnect();
    }
}
