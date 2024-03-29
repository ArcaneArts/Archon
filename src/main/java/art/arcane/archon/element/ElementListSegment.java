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

package art.arcane.archon.element;

import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.KList;
import lombok.Data;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
public class ElementListSegment<T extends Element> implements Iterable<T> {
    private Class<? extends T> type;
    private AtomicCache<List<T>> data;
    private Supplier<ArchonResult> resulter;

    public ElementListSegment(Class<? extends T> type, Supplier<ArchonResult> resulter) {
        this.type = type;
        this.resulter = resulter;
        data = new AtomicCache<>();
    }

    public List<T> getData() {
        return data.aquire(() -> {
            ArchonResult r = resulter.get();
            KList<T> rt = new KList<>();

            for (int i = 0; i < r.size(); i++) {
                try {
                    T ee = type.getConstructor().newInstance();
                    ee.pull(r, r.getRow(i));
                    rt.add(ee);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            return rt;
        });
    }

    public T get(int index) {
        return getData().get(index);
    }

    public int getSize() {
        return getData().size();
    }

    @Override
    public Iterator<T> iterator() {
        return getData().iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        getData().forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return getData().spliterator();
    }
}
