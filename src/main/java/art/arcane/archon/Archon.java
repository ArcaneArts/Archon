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

package art.arcane.archon;

import art.arcane.archon.server.ArchonService;
import art.arcane.quill.logging.L;

import java.sql.Connection;

public class Archon {
    public static ArchonService defaultService = null;
    public static Connection forceConnection = null;

    public static void main(String[] a) {
        new ArchonService().shutdown();
        L.flush();
        System.exit(0);
    }
}
