package art.arcane.archon.element;

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
        id = new ID(t.getPrimaryValue());
        type = t.getClass();
    }

    public Reference(Element parent,  Class<? extends T> c, ID id)
    {
        this.parent = parent;
        this.id = new ID(id.toString());
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
        id = new ID(t.getPrimaryValue());
    }

    public T get()
    {
        try {
            T t = (T) type.getConstructor().newInstance();
            Field f = t.getPrimaryField().getField();
            f.setAccessible(true);
            f.set(t, new ID(id.toString()));
            if(t.pull())
            {
                return t;
            }
        } catch (Throwable e) {
        }
        return null;
    }
}
