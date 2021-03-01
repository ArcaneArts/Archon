package art.arcane.archon.element;

import lombok.Data;

import java.lang.reflect.Field;

@Data
public class ElementField
{
    private String sqlType;
    private String name;
    private String sqlName;
    private Field field;
    private Object defaultValue;
    private boolean identity;
    private boolean reference;
}
