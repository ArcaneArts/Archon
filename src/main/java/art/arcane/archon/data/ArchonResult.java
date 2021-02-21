package art.arcane.archon.data;

import art.arcane.quill.collections.KList;
import com.google.gson.Gson;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

@Data
public class ArchonResult
{
    private static final Gson gson = new Gson();
    private KList<String> h;
    private KList<Integer> t;
    private KList<Integer> l;
    private KList<KList<Object>> d;

    public ArchonResult()
    {
        this.h = new KList<>();
        this.t = new KList<>();
        this.l = new KList<>();
        this.d = new KList<>();
    }

    public ArchonResult(ResultSet rs) throws SQLException {
        this();
        ResultSetMetaData md = rs.getMetaData();

        for(int i = 1; i <= md.getColumnCount(); i++)
        {
            h.add(md.getColumnName(i));
            t.add(md.getColumnType(i));
            l.add(md.getColumnDisplaySize(i));
        }

        while(rs.next())
        {
            KList<Object> data = new KList<>();

            for(int i = 1; i <= h.size(); i++)
            {
                data.add(rs.getObject(i));
            }

            d.add(data);
        }
    }

    public int size()
    {
        return d.size();
    }

    public int columnSize()
    {
        return h.size();
    }

    public int getColumnType(int column)
    {
        return t.get(column);
    }

    public int getColumnTypeLength(int column)
    {
        return l.get(column);
    }

    public String getColumnName(int column)
    {
        return h.get(column);
    }

    public ArchonResultRow rowFromColumn(String name)
    {
        return rowFromColumn(h.indexOf(name));
    }

    public ArchonResultRow rowFromColumn(int column)
    {
        KList<Object> o = new KList<>();

        for(int i = 0; i < size(); i++)
        {
            o.add(d.get(i).get(column));
        }

        return new ArchonResultRow(o);
    }

    public void forEachRow(Consumer<ArchonResultRow> rowConsumer)
    {
        for(int i = 0; i < size(); i++)
        {
            rowConsumer.accept(getRow(i));
        }
    }

    public ArchonResultRow getRow(int row)
    {
        return new ArchonResultRow(d.get(row));
    }

    public static ArchonResult fromJson(String json)
    {
        return gson.fromJson(json, ArchonResult.class);
    }

    public String toJSON()
    {
        return gson.toJson(this);
    }
}
