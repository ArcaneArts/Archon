package art.arcane.archon.element;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExampleTable extends Element {
    @Identity
    private ID id;

    @Type("CHAR(16)")
    private String shortName;

    @Override
    public String getTableName() {
        return "example";
    }
}
