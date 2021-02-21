package art.arcane.archon.data;

import art.arcane.quill.collections.KList;

import java.util.function.Consumer;

public class ArchonResultRow {
    private final KList<Object> data;

    public ArchonResultRow(KList<Object> data){
     this.data = data;
    }

    public void forEachCell(Consumer<Object> cellConsumer)
    {
        for(int i = 0; i < size(); i++)
        {
            cellConsumer.accept(get(i));
        }
    }

    public Object get(int v)
    {
        return data.get(v);
    }

    public String getString(int v)
    {
        return get(v).toString();
    }

    public int getInt(int v)
    {
        return (int) get(v);
    }

    public long getLong(int v)
    {
        return (long) get(v);
    }

    public double getDouble(int v)
    {
        return (double) get(v);
    }

    public boolean getBoolean(int v)
    {
        return getInt(v) == 1;
    }

    public byte getByte(int v)
    {
        return (byte) get(v);
    }

    public float getFloat(int v)
    {
        return (float) get(v);
    }

    public int size()
    {
        return data.size();
    }
}
