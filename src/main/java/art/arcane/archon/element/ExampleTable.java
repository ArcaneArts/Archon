package art.arcane.archon.element;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExampleTable extends Element {
    @Identity
    private ID id = new ID();

    private String astring = "derp";
    private int anint = 0;
    private byte abyte = 0;
    private short ashort = 0;
    private long along = 0;
    private float afloat = 0;
    private double adouble = 0;
    private boolean aboolean = false;

    @Override
    public String getTableName() {
        return "example2";
    }
}
