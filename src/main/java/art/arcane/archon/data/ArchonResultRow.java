package art.arcane.archon.data;

import art.arcane.quill.collections.KList;
import lombok.Data;

import java.util.function.Consumer;

@Data
public class ArchonResultRow {
    private final KList<Object> data;

    public ArchonResultRow(KList<Object> data){
     this.data = data;
    }

    public KList<String> toStringList()
    {
        return getData().toStringList();
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
        return Integer.parseInt(get(v).toString());
    }

    public long getLong(int v)
    {
        return Long.parseLong(get(v).toString());
    }

    public double getDouble(int v)
    {
        return Double.parseDouble(get(v).toString());
    }

    public boolean getBoolean(int v)
    {
        return getInt(v) == 1;
    }

    public byte getByte(int v)
    {
        return Byte.parseByte(get(v).toString());
    }

    public float getFloat(int v)
    {
        return Float.parseFloat(get(v).toString());
    }

    public int size()
    {
        return data.size();
    }
}
