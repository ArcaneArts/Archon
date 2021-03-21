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

import art.arcane.archon.element.*;
import art.arcane.quill.collections.ID;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ParentRefTest extends Element {
    @Identity
    @Builder.Default
    private ID id = new ID();

    @Type("VARCHAR(128)")
    @Builder.Default
    private String email = "someone@something.at";

    @Type("VARCHAR(64)")
    @Builder.Default
    private String firstName = "Mills";

    @Type("VARCHAR(64)")
    @Builder.Default
    private String lastName = "Mills";

    @ReferenceType(ChildRefTest.class)
    @Builder.Default
    private Reference<ChildRefTest> childRef = new Reference<>(ChildRefTest.builder()
            .password("1337")
            .salt("sdfsdfsdf")
            .pepper("sdfhjhjjj")
            .build());

    @Override
    public String getTableName() {
        return "parentreftest";
    }
}
