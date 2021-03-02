package art.arcane.archon.element;

import art.arcane.quill.collections.ID;
import lombok.Data;

import java.lang.reflect.Field;

@Data
public class Reference<T extends Element> {
    private final transient Element parent;
    private final Class<? extends Element> type;
    private ID id;

    public Reference(Element parent, T t)
    {
        this.parent = parent;
        id = ID.fromString(t.getPrimaryValue());
        type = t.getClass();
    }

    public Reference(Element parent,  Class<? extends T> c, ID id)
    {
        this.parent = parent;
        this.id = ID.fromString(id.toString());
        type = c;
    }

    public Reference(Element parent, Class<? extends T> c)
    {
        this.parent = parent;
        this.id = null;
        type = c;
    }

    public String toString()
    {
        if(id == null)
        {
            return "nullid";
        }

        return id.toString();
    }

    public void set(T t)
    {
        id = ID.fromString(t.getPrimaryValue());
    }

    public T get()
    {
        try {
            T t = (T) type.getConstructor().newInstance();
            Field f = t.getPrimaryField().getField();
            f.setAccessible(true);
            f.set(t, ID.fromString(id.toString()));
            if(t.pull())
            {
                return t;
            }
        } catch (Throwable e) {
        }
        return null;
    }
}
