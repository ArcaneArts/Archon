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
