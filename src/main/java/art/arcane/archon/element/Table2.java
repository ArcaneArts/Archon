package art.arcane.archon.element;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Table2 extends Element {
    @Identity
    private ID id = new ID();

    private int val = 3404;

    @Override
    public String getTableName() {
        return "table2";
    }
}
