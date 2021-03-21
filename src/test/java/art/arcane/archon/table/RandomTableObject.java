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
public class RandomTableObject extends Element {
    private final ID tableID = new ID();

    @Builder.Default
    @Identity
    private ID id = new ID();

    @Type("VARCHAR(64)")
    @Builder.Default
    private String value = "default";

    @Override
    public String getTableName() {
        return tableID.toString();
    }
}