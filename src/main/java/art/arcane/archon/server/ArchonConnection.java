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

public interface ArchonConnection {
    boolean isConnected();

    void disconnect();

    String getName();

    void connect();

    default void ensureConnected() {
        if (!isConnected()) {
            connect();
        }
    }

    default void ensureDisconnected() {
        if (isConnected()) {
            disconnect();
        }
    }
}
