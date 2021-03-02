package art.arcane.archon.element;

import art.arcane.quill.collections.ID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Table1 extends Element {
    @Identity
    private ID id = new ID();

    private String name = "some name";

    @ReferenceType(Table2.class)
    private Reference<Table2> table2;

    @Override
    public String getTableName() {
        return "table1";
    }
}
