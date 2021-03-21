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

import art.arcane.quill.collections.ID;
import lombok.Data;

import java.lang.reflect.Field;

@Data
public class Reference<T extends Element> {
    private final Class<? extends Element> type;
    private ID id;

    public Reference(T t) {
        id = ID.fromString(t.getPrimaryValue());
        type = t.getClass();
    }

    public Reference(Class<? extends T> c, ID id) {
        this.id = ID.fromString(id.toString());
        type = c;
    }

    public Reference(Class<? extends T> c) {
        this.id = null;
        type = c;
    }

    public String toString() {
        if (id == null) {
            return "nullid";
        }

        return id.toString();
    }

    /**
     * Simply sets the id of this element's reference to the specified element's identity id does not push anything.
     *
     * @param t the referenced (child) element to set in this element's reference
     */
    public void set(T t) {
        id = ID.fromString(t.getPrimaryValue());
    }

    /**
     * Commit will set the id and push both the parent (reference holder) and the child (the referebced)
     *
     * @param t the referenced (child) element to set in this element's reference
     */
    public void commit(T t) {
        id = ID.fromString(t.getPrimaryValue());
        t.push();
    }

    /**
     * Get (create/pull) the child object if there is one, otherwise null
     *
     * @return the element
     */
    public T get() {
        try {
            T t = (T) type.getConstructor().newInstance();
            Field f = t.getPrimaryField().getField();
            f.setAccessible(true);
            f.set(t, ID.fromString(id.toString()));
            if (t.pull()) {
                return t;
            }
        } catch (Throwable e) {
        }
        return null;
    }
}
