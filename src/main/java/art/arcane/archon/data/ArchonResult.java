/*
 * This file is part of Archon by Arcane Arts.
 *
 * Archon by Arcane Arts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Archon by Arcane Arts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License in this package for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Archon.  If not, see <https://www.gnu.org/licenses/>.
 */

package art.arcane.archon.data;

import art.arcane.quill.collections.KList;
import com.google.gson.Gson;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

@Data
public class ArchonResult {
    private static final Gson gson = new Gson();
    private KList<String> h;
    private KList<Integer> t;
    private KList<Integer> l;
    private KList<KList<Object>> d;

    public ArchonResult() {
        this.h = new KList<>();
        this.t = new KList<>();
        this.l = new KList<>();
        this.d = new KList<>();
    }

    public ArchonResult(ResultSet rs) throws SQLException {
        this();
        ResultSetMetaData md = rs.getMetaData();

        for (int i = 1; i <= md.getColumnCount(); i++) {
            h.add(md.getColumnName(i));
            t.add(md.getColumnType(i));
            l.add(md.getColumnDisplaySize(i));
        }

        while (rs.next()) {
            KList<Object> data = new KList<>();

            for (int i = 1; i <= h.size(); i++) {
                data.add(rs.getObject(i));
            }

            d.add(data);
        }
    }

    public static ArchonResult fromJson(String json) {
        return gson.fromJson(json, ArchonResult.class);
    }

    public int size() {
        return d.size();
    }

    public int columnSize() {
        return h.size();
    }

    public int getColumnType(int column) {
        return t.get(column);
    }

    public int getColumnTypeLength(int column) {
        return l.get(column);
    }

    public String getColumnName(int column) {
        return h.get(column);
    }

    public ArchonResultRow rowFromColumn(String name) {
        int v = h.indexOf(name);

        if (v == -1) {
            v = h.indexOf(name.toLowerCase());

            if (v == -1) {
                throw new RuntimeException("Cannot find column'" + name + "' in [" + h.toString(", ") + "] (also tried lower case)");
            }
        }

        return rowFromColumn(v);
    }

    public ArchonResultRow rowFromColumn(int column) {
        if (column < 0) {
            throw new RuntimeException("Cannot get a row for the column index " + column);
        }

        KList<Object> o = new KList<>();

        for (int i = 0; i < size(); i++) {
            o.add(d.get(i).get(column));
        }

        return new ArchonResultRow(o);
    }

    public void forEachRow(Consumer<ArchonResultRow> rowConsumer) {
        for (int i = 0; i < size(); i++) {
            rowConsumer.accept(getRow(i));
        }
    }

    public ArchonResultRow getRow(int row) {
        return new ArchonResultRow(d.get(row));
    }

    public String toJSON() {
        return gson.toJson(this);
    }
}
