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

import art.arcane.archon.Archon;
import art.arcane.archon.configuration.ArchonSQLConfiguration;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.Quill;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.RoundRobin;
import art.arcane.quill.logging.L;
import art.arcane.quill.service.QuillService;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ArchonService extends QuillService {
    private KList<ArchonSQLConfiguration> connections = KList.from(new ArchonSQLConfiguration());
    private transient RoundRobin<ArchonSQLConnection> readOnlySQLConnections;
    private transient RoundRobin<ArchonSQLConnection> writeSQLConnections;
    private transient Edict edict;

    public static void fixClass(Class<?> v) {

    }

    @Override
    public void onEnable() {
        try {
            fixClass(com.mysql.jdbc.Driver.class);
            fixClass(com.mysql.cj.conf.url.SingleConnectionUrl.class);
        } catch (Throwable e) {
            L.ex(e);
            Quill.crashStack("Failed to load sql driver...");
        }
        try {
            i("Initializing SQL Connections");
            initSQLConnections();
            edict = new Edict(this);
            i("Online (" + readOnlySQLConnections.list().size() + " Read Only, " + writeSQLConnections.list().size() + " Writable)");
            L.flush();
            Archon.defaultService = this;
        } catch (Throwable e) {
            L.ex(e);
            Quill.crashStack("Failed to start Archon");
        }
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    public ArchonResult query(String query) {
        return access().query(query);
    }

    public int update(String query) {
        return access().update(query);
    }

    public void shutdown() {
        writeSQLConnections.list().forEach(ArchonConnection::disconnect);
        readOnlySQLConnections.list().forEach(ArchonConnection::disconnect);
        L.i("Archon has Shutdown");
    }

    public Edict access() {
        if (edict == null) {
            Quill.crashStack("Non-initialized Service worker! Something with startup is wrong!");
        }

        return edict;
    }

    public ArchonSQLConnection getReadSQLConnection() {
        ArchonSQLConnection v = readOnlySQLConnections.next();
        return v == null ? getWriteSQLConnection() : v;
    }

    public ArchonSQLConnection getWriteSQLConnection() {
        return writeSQLConnections.next();
    }

    private void initSQLConnections() {
        if (Archon.forceConnection != null) {
            connections.clear();
            w("I hope you know what you're doing by forcing SQL Connections through Archon...");
            readOnlySQLConnections = new KList<ArchonSQLConnection>().roundRobin();
            writeSQLConnections = KList.from(ArchonSQLConnection.wrap(Archon.forceConnection)).roundRobin();
            return;
        }

        KList<ArchonSQLConfiguration> conf = getConnections();
        KList<ArchonSQLConnection> readOnly = new KList<>();
        KList<ArchonSQLConnection> write = new KList<>();
        for (ArchonSQLConfiguration i : conf) {
            ArchonSQLConnection c = new ArchonSQLConnection(i);
            c.connect();

            if (c.isConnected()) {
                i("Connected to DB " + c.getTag());
                if (i.isReadOnly()) {
                    readOnly.add(c);
                } else {
                    write.add(c);
                }
            } else {
                Quill.crashStack(c.getTag() + " is somehow not connected after invoking connect(). This should not happen as we should have already crashed. See the stack trace below.");
            }
        }

        writeSQLConnections = write.roundRobin();
        readOnlySQLConnections = readOnly.roundRobin();
    }
}
