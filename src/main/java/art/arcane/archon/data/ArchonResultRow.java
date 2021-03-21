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
import lombok.Data;

import java.util.function.Consumer;

@Data
public class ArchonResultRow {
    private final KList<Object> data;

    public ArchonResultRow(KList<Object> data) {
        this.data = data;
    }

    public KList<String> toStringList() {
        return getData().toStringList();
    }

    public void forEachCell(Consumer<Object> cellConsumer) {
        for (int i = 0; i < size(); i++) {
            cellConsumer.accept(get(i));
        }
    }

    public Object get(int v) {
        return data.get(v);
    }

    public String getString(int v) {
        return get(v).toString();
    }

    public int getInt(int v) {
        return Integer.parseInt(get(v).toString());
    }

    public long getLong(int v) {
        return Long.parseLong(get(v).toString());
    }

    public double getDouble(int v) {
        return Double.parseDouble(get(v).toString());
    }

    public boolean getBoolean(int v) {
        return getInt(v) == 1;
    }

    public byte getByte(int v) {
        return Byte.parseByte(get(v).toString());
    }

    public float getFloat(int v) {
        return Float.parseFloat(get(v).toString());
    }

    public int size() {
        return data.size();
    }
}
