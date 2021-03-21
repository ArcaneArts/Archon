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

package art.arcane.archon.table;

import art.arcane.archon.element.Element;
import art.arcane.archon.element.Identity;
import art.arcane.archon.element.Type;
import art.arcane.quill.collections.ID;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AlterOne extends Element {
    @Builder.Default
    @Identity
    private ID id = new ID();

    @Type("VARCHAR(64)")
    @Builder.Default
    private String value = "default";

    @Override
    public String getTableName() {
        return "altertest";
    }
}
