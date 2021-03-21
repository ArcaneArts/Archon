/*
 * This file is part of Archon by Arcane Arts.
 *
 * Archon by Arcane Arts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Archon by Arcane Arts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License in this package for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Archon.  If not, see <https://www.gnu.org/licenses/>.
 */

package art.arcane.archon.server;

import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.Quill;
import art.arcane.quill.execution.J;
import art.arcane.quill.logging.L;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ArchonSQLConnection implements ArchonConnection {
    private static int creates = 1;
    private final String name;
    private transient final ArchonSQLConfiguration config;
    private boolean connected;
    private Connection sql;
    private Connection forceConnection;

    public ArchonSQLConnection(ArchonSQLConfiguration config) {
        this.config = config;
        name = "sql:" + config.hashCode() + ":" + creates++;
        connected = false;
    }

    public static ArchonSQLConnection wrap(Connection forceConnection) {
        ArchonSQLConnection a = new ArchonSQLConnection(new ArchonSQLConfiguration());
        a.force(forceConnection);
        a.connect();
        return a;
    }

    private void force(Connection forceConnection) {
        this.forceConnection = forceConnection;
    }

    @Override
    public void disconnect() {
        if (sql != null) {
            J.attempt(() -> sql.close());
            sql = null;
        }

        connected = false;
    }

    @Override
    public void connect() {
        if (forceConnection != null) {
            sql = forceConnection;

            try {
                if (sql.isValid(5)) {
                    connected = true;
                    return;
                }
            } catch (SQLException e) {
                L.f("[" + getName() + "]: Database Connection Failure!");
                L.ex(e);
                Quill.crash("Failed to connect to database: (TEST EMBEDDED DB)" + " (See error above)");
            }
            return;
        }

        J.attempt(() -> Class.forName(config.getDriver()).newInstance());
        Properties p = new Properties();
        p.setProperty("user", config.getUsername());

        if (config.isUsesPassword()) {
            p.setProperty("password", config.getPassword());
        }

        String url = "jdbc:mysql://" + config.getAddress() + (":" + config.getPort()) + "/" + config.getDatabase();

        try {

            sql = DriverManager.getConnection(url, p);

            if (sql.isValid(5)) {
                connected = true;
                return;
            }
        } catch (Throwable e) {
            L.f("[" + getName() + "]: Database Connection Failure!");
            L.ex(e);
            Quill.crash("Failed to connect to database: " + url + " (See error above)");
        }

        disconnect();
    }

    public ArchonResult query(String query) {
        try {
            ArchonResult res = new ArchonResult(sql.prepareStatement(query).executeQuery());
            L.v("[SQL Query]: " + query + " -> " + res.toJSON());
            return res;
        } catch (SQLException throwables) {
            L.ex(throwables);
            Quill.crash("Failed to query with: " + query);
        }

        return null;
    }

    public int update(String query) {
        try {
            int v = sql.prepareStatement(query).executeUpdate();
            L.v("[SQL Update]: " + query + " -> " + v + " rows affected");
            return v;
        } catch (SQLException throwables) {
            L.ex(throwables);
            Quill.crash("Failed to update with: " + query);
        }

        return -1;
    }

    public String getName() {
        return this.name;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public String getTag() {
        return getName() + "@" + config.getAddress() + ":" + config.getPort() + "/" + config.getDatabase();
    }
}
