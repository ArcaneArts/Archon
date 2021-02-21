package art.arcane.archon.element;

import art.arcane.quill.random.RNG;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ID {
    public static final int LENGTH = 64;
    private final String value;

    public ID(String value)
    {
        this.value = value;
    }

    public ID()
    {
        this(new RNG().s(LENGTH));
    }

    public String toString()
    {
        return value;
    }
}
