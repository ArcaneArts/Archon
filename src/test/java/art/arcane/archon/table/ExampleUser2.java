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
public class ExampleUser2 extends Element {
    @Builder.Default
    @Identity
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

    @Builder.Default
    private int someId = 0;

    @Override
    public String getTableName() {
        return "exampleuser2";
    }
}
