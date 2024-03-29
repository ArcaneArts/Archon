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

import art.arcane.archon.data.ArchonResult;
import lombok.Data;

@Data
public class Edict {
    private final ArchonService server;

    public Edict(ArchonService server) {
        this.server = server;
    }

    public String toString() {
        return "Edict";
    }

    public ArchonResult query(String q) {
        return server.getReadSQLConnection().query(q);
    }

    public int update(String q) {
        return server.getWriteSQLConnection().update(q);
    }
}
