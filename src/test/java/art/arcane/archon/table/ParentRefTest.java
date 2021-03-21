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
