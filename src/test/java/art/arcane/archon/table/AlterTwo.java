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
public class AlterTwo extends Element {
    @Builder.Default
    @Identity
    private ID id = new ID();

    @Type("VARCHAR(64)")
    @Builder.Default
    private String value = "default";

    @Builder.Default
    private int value2 = 34;

    @Override
    public String getTableName() {
        return "altertest";
    }
}
