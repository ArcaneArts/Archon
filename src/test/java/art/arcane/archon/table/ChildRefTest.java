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
public class ChildRefTest extends Element {
    @Builder.Default
    @Identity
    private ID id = new ID();

    @Type("VARCHAR(128)")
    @Builder.Default
    private String password = "12345";

    @Type("VARCHAR(64)")
    @Builder.Default
    private String salt = "data";

    @Type("VARCHAR(64)")
    @Builder.Default
    private String pepper = "data";

    @Override
    public String getTableName() {
        return "childreftest";
    }
}
