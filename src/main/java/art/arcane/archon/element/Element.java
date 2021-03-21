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

import art.arcane.archon.Archon;
import art.arcane.archon.data.ArchonResult;
import art.arcane.archon.data.ArchonResultRow;
import art.arcane.archon.server.ArchonService;
import art.arcane.archon.server.Edict;
import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.ID;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.KMap;
import art.arcane.quill.collections.KSet;
import art.arcane.quill.logging.L;
import art.arcane.quill.random.RNG;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Data;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * An element represents a table in SQL or other db mediums. Each field represents a column.
 * Every element requires one @Identity annotated field using the ID class as it's type.
 * You can use @Type to define a custom SQL type but it will assume that variable's type by
 * default. You can also use Reference<AnotherElement> as references to other tables instead
 * of just juggling ids.
 */
@Data
public abstract class Element {
    private static final KSet<Class<? extends Element>> synced = new KSet<>();
    private static final KSet<Class<? extends Element>> syncedDone = new KSet<>();
    private static final Gson gson = buildGson();
    private static final KMap<Class<? extends Element>, AtomicCache<KList<ElementField>>> fieldMapping = new KMap<>();
    private static boolean tableExists = false;
    private transient final AtomicCache<ElementField> primaryKey = new AtomicCache<>();
    private transient Boolean exists = null;
    private transient ArchonService archon = Archon.defaultService;
    private transient Element snapshot = null;
    private int dropRequest;

    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeHierarchyAdapter(Reference.class, new TypeAdapter<Reference<?>>() {
                    @Override
                    public void write(JsonWriter out, Reference<?> value) throws IOException {
                        out.value(value.getId().toString());
                    }

                    @Override
                    public Reference<?> read(JsonReader in) throws IOException {
                        return new Reference<>(null, ID.fromString(in.nextString()));
                    }
                })

                .create();
    }

    /**
     * Get the name of the SQL table this element should be called
     *
     * @return simply the table name
     */
    public abstract String getTableName();

    /**
     * Dropping tables is dangerous. This returns a random number and stores it in this
     * instance. Use this code to drop the table if you choose to do so
     *
     * @return the drop code.
     */
    public int dropTableRequestCode() {
        dropRequest = RNG.r.imax();
        return dropRequest;
    }

    /**
     * This is used if you want to force archon to re-validate this table, meaning
     * Checking if the table exists, re-checking colums and altering the table to sync
     * with this object's field mapping.
     */
    public void bruteForceUnregister() {
        synced.remove(getClass());
        syncedDone.remove(getClass());
        fieldMapping.remove(getClass());
    }

    /**
     * Drop the entire SQL table (WARNING)
     *
     * @param requestCode a random code generated with dropTableRequestCode()
     * @return true if it succeeds
     */
    public boolean dropTable(int requestCode) {
        enforceArchon();

        if (dropRequest == requestCode) {
            synced.remove(getClass());
            syncedDone.remove(getClass());
            fieldMapping.remove(getClass());
            getArchon().update("DROP TABLE `" + getTableName() + "`;");
            return true;
        }

        return false;
    }

    /**
     * Pull all of the fields from SQL that are on the same row as the IDENTITY field.
     *
     * @return true if it successfully pulled
     */
    public boolean pull() {
        enforceArchon();

        try {
            sync();
            return pull(archon.query("SELECT * FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;"));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Returns an ID where FIELD is equal to VALUE
     *
     * @param field the field name as it appears in java
     * @param value the value (dont use quotes)
     * @return the ID or null if it cannot be found
     */
    public ID getIdentityWhere(String field, String value) {
        return getIdentityWhereRaw(field, "'" + value + "'");
    }

    /**
     * Returns an ID where FIELD is equal to VALUE. This is the raw version,
     * meaning you can use non quoted values such as numbers
     *
     * @param field the field name as it appears in java
     * @param value the value if it's a string use 'single quotes'
     * @return the ID or null if it cannot be found
     */
    public ID getIdentityWhereRaw(String field, String value) {
        ArchonResult r = getArchon().getReadSQLConnection().query("SELECT `" + getPrimaryField().getSqlName() + "` FROM `" + getTableName() + "` WHERE `" + field + "` = " + value + " LIMIT 1;");

        if (r.size() > 0) {
            return ID.fromString(r.getRow(0).getString(0));
        }

        return null;
    }

    /**
     * Pulls this instance where a field is equal to a value
     * (raw) so use 'single quotes' for strings
     *
     * @param field the field name as it appears in java
     * @param value the raw value, use 'single quotes' if it's a string
     * @return true if this instance's fields were populated with a row that
     * matched your condition
     */
    public boolean whereRaw(String field, String value) {
        return pull(getArchon().getReadSQLConnection().query("SELECT * FROM `" + getTableName() + "` WHERE `" + field + "` = " + value + " LIMIT 1;"));
    }

    /**
     * Pulls this instance where a field is equal to a value assumes single quotes
     *
     * @param field the field name as it appears in java
     * @param value the value. use whereRaw for numbers (non strings)
     * @return true if this instance's fields were populated with a row that
     * matched your condition
     */
    public boolean where(String field, String value) {
        return whereRaw(field, "'" + value + "'");
    }

    /**
     * Builder method to apply archon service to this instance
     *
     * @param a   the archon service
     * @param <T> this type
     * @return this (builder)
     */
    public <T extends Element> T archon(ArchonService a) {
        setArchon(a);
        return (T) this;
    }

    private void enforceArchon() {
        if (!synced.contains(getClass())) {
            synced.add(getClass());
            sync();
        }

        if (archon == null) {
            throw new NullPointerException("You must use setArchon() on this element to push or pull.");
        }
    }

    /**
     * Pull all the data from an archon result. Assumes the first row in the archon result
     *
     * @param r the archon result
     * @return true if it managed to pull information into this instance from the result
     */
    public boolean pull(ArchonResult r) {
        enforceArchon();
        try {
            if (r.size() <= 0) {
                return false;
            }

            return pull(r, r.getRow(0));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Pulls a specific row from an archon result
     *
     * @param r   the result
     * @param row the result's row reference
     * @return true if it pulled information otherwise false
     */
    public boolean pull(ArchonResult r, ArchonResultRow row) {
        enforceArchon();
        try {
            if (r.size() <= 0) {
                return false;
            }

            sync();
            for (ElementField i : getFieldMapping()) {
                try {
                    ElementUtil.insert(this, i.getField(), row.get(r.getH().indexOf(i.getSqlName())));
                } catch (Throwable e) {
                    L.ex(e);
                }
            }

            takeSnapshot();

            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Pushes information back to SQL. If it has already pulled then
     * it will only push values that have changed. If you want to force it,
     * use push(true) to force it.
     *
     * @param <T> this type (builder return)
     * @return this (builder return)
     */
    public <T extends Element> T push() {
        enforceArchon();
        return push(false);
    }

    /**
     * Pushes information back to SQL. If it has already pulled then
     * it will only push values that have changed. If you want to force it,
     * use push(true) to force it.
     *
     * @param forcePush should we force push, meaning ignore what has and has not changed
     *                  and just set every column in the row
     * @param <T>       this type (builder return)
     * @return this (builder return)
     */
    public <T extends Element> T push(boolean forcePush) {
        enforceArchon();
        if (getPrimaryValue() == null) {
            return null;
        }

        if (exists()) {
            if (snapshot != null) {
                KList<ElementField> changed = new KList<>();
                for (ElementField i : getFieldMapping()) {
                    if (!i.isIdentity()) {
                        try {
                            Object s = i.getField().get(snapshot);
                            Object r = i.getField().get(this);

                            if (!ElementUtil.equals(s, r)) {
                                changed.add(i);
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (changed.isNotEmpty()) {
                    archon.update("UPDATE `" + getTableName() + "` SET " + changed.convert((i) -> {
                        try {
                            return "`" + i.getSqlName() + "` = '" + ElementUtil.escapeString(boolsafe(i.getField().get(Element.this)).toString(), true) + "'";
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        return "";
                    }).toString(", ") + " WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");

                    takeSnapshot();
                    return (T) this;
                }
            }

            if (forcePush) {
                archon.update("UPDATE `" + getTableName() + "` SET " + getFieldMapping().convert((i) -> {
                    try {
                        return "`" + i.getSqlName() + "` = '" + ElementUtil.escapeString(boolsafe(i.getField().get(Element.this)).toString(), true) + "'";
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return "";
                }).toString(", ") + " WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");
            }
        } else {
            archon.update("INSERT INTO `" + getTableName() + "` (" + getFieldMapping().convert(ElementField::getSqlName).toString(", ") + ") VALUES (" + getFieldMapping().convert((i) -> {
                try {
                    return "'" + ElementUtil.escapeString(boolsafe(i.getField().get(Element.this)).toString(), true) + "'";
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return "";
            }).toString(", ") + ");");
            exists = true;
        }

        takeSnapshot();
        return (T) this;
    }

    private void takeSnapshot() {
        snapshot = gson.fromJson(gson.toJson(this), getClass());
        snapshot.setArchon(getArchon());
    }

    /**
     * Return an element list that contains elements where the given condition is met
     *
     * @param where the condition (SQL)
     * @param <T>   this type (builder return)
     * @return this (builder return)
     */
    public <T extends Element> ElementList<T> allWhere(String where) {
        enforceArchon();
        Class<T> c = (Class<T>) getClass();
        return (ElementList<T>) ElementList.where(getArchon(), getClass(), where);
    }

    /**
     * Get all where (in an element list) the condition is met,
     * along with an order by field (assumes ascending)
     *
     * @param where   the where condition
     * @param orderBy the field to sort by (assumes ascending)
     * @param <T>     this type (builder return)
     * @return this (builder return)
     */
    public <T extends Element> ElementList<T> allWhere(String where, String orderBy) {
        return allWhere(where, orderBy, true);
    }

    /**
     * Get all where (in an element list) the condition is met,
     * along with an order by field
     *
     * @param where     the where condition
     * @param orderBy   the field to sort by
     * @param ascending ascending? or decending (sort)
     * @param <T>       this type (builder return)
     * @return this (builder return)
     */
    public <T extends Element> ElementList<T> allWhere(String where, String orderBy, boolean ascending) {
        enforceArchon();
        Class<T> c = (Class<T>) getClass();
        return (ElementList<T>) ElementList.where(getArchon(), getClass(), where, orderBy, ascending);
    }

    /**
     * Delete this representation of a row in SQL. Only uses the IDENTITY field to
     * figure out which row to delete. ONLY DELETES ONE
     *
     * @return true if one was deleted otherwise false.
     */
    public boolean delete() {
        enforceArchon();
        sync();
        int c = archon.update("DELETE FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");
        exists = false;

        return c > 0;
    }

    /**
     * Check if this instance reference to an SQL row actually exists in SQL
     * Only checks the IDENTITY field to verify
     *
     * @return true if a row in SQL has an ID that matches
     * this object's ID (IDENTITY)
     */
    public boolean exists() {
        enforceArchon();
        if (getPrimaryValue() == null) {
            return false;
        }

        if (exists != null) {
            return exists;
        }

        sync();
        exists = archon.query("SELECT COUNT(1) FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "';").getRow(0).getInt(0) > 0;
        return exists;
    }

    /**
     * Get the primary value in string (the field with @Identity on it)
     *
     * @return the primary ID of this row
     */
    public String getPrimaryValue() {
        try {
            return getPrimaryField().getField().get(this).toString();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get the element field instance of this IDENTITY field
     *
     * @return the element field
     */
    public ElementField getPrimaryField() {
        return primaryKey.aquire(() -> {
            for (ElementField i : getFieldMapping()) {
                if (i.isIdentity()) {
                    return i;
                }
            }

            return null;
        });
    }

    /**
     * Synchronize this table with SQL (java is the overriding party here)
     * ALTER TABLE / CREATE TABLE IF NOT EXISTS will be used here. (cached)
     */
    public void sync() {
        if (syncedDone.contains(getClass())) {
            return;
        }

        enforceArchon();
        for (ElementField i : getFieldMapping()) {
            if (i.isReference()) {
                Field f = i.getField();
                f.setAccessible(true);

                try {
                    Reference<?> o = (Reference<?>) f.get(this);

                    if (o == null) {
                        L.v("Initialized Reference");
                        f.set(this, Reference.class.getConstructor(Class.class).newInstance(f.getDeclaredAnnotation(ReferenceType.class).value()));
                    } else if (o.getClass() == null) {
                        L.v("Repaired Reference");
                        f.set(this, Reference.class.getConstructor(Class.class, ID.class).newInstance(f.getDeclaredAnnotation(ReferenceType.class).value(), o.getId()));
                    }
                } catch (Throwable e) {
                    L.ex(e);
                }
            }
        }

        if (tableExists) {
            return;
        }

        if (!createTable()) {
            KList<String> cols = tableColumns();
            KList<ElementField> mcols = getFieldMapping();
            KList<ElementField> add = new KList<>();
            KList<String> del = new KList<>();
            KList<String> alt = new KList<>();

            for (ElementField i : mcols) {
                if (!cols.contains(i.getSqlName())) {
                    add.add(i);
                }
            }

            removing:
            for (String i : cols) {
                for (ElementField j : mcols) {
                    if (j.getSqlName().equals(i)) {
                        continue removing;
                    }
                }

                del.add(i);
            }

            del.forEach((i) -> alt.add("DROP `" + i + "`"));

            for (ElementField i : add) {
                if (i.getDefaultValue() != null && !i.getSqlType().equalsIgnoreCase("TEXT")) {
                    alt.add("ADD `" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL DEFAULT '" + ElementUtil.escapeString(i.getDefaultValue().toString(), true) + "'");
                } else {
                    alt.add("ADD `" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL");
                }
            }

            if (alt.isNotEmpty()) {
                archon.update("ALTER TABLE `" + getTableName() + "` " + alt.toString(", ") + ";");
            }

            syncedDone.add(getClass());
        } else {
            sync();
            tableExists = true;
        }
    }

    /**
     * Get the count of rows in this table
     *
     * @return the row count
     */
    public long tableSize() {
        enforceArchon();
        sync();
        return archon.query("SELECT COUNT(*) FROM `" + getTableName() + "`")
                .getRow(0)
                .getLong(0);
    }

    private KList<String> tableColumns() {
        enforceArchon();
        return archon.query("SHOW COLUMNS FROM `" + getTableName() + "`")
                .rowFromColumn("COLUMN_NAME")
                .toStringList();
    }

    private ElementField fieldForSQLName(String sqlName) {
        for (ElementField i : getFieldMapping()) {
            if (i.getSqlName().equals(sqlName)) {
                return i;
            }
        }

        return null;
    }

    private boolean createTable() {
        enforceArchon();
        Edict e = archon.access();
        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS ");
        query.append("`").append(getTableName()).append("` ");
        KList<String> f = new KList<>();
        String pk = "ERROR";
        for (ElementField i : getFieldMapping()) {
            if (i.isIdentity()) {
                pk = "(`" + i.getSqlName() + "`)";
            }

            if (i.getDefaultValue() != null && !i.getSqlType().equalsIgnoreCase("TEXT")) {
                f.add("`" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL DEFAULT '" + ElementUtil.escapeString((boolsafe(i.getDefaultValue())).toString(), true) + "'");

            } else {
                f.add("`" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL");
            }
        }

        f.add("PRIMARY KEY " + pk);
        query.append("(").append(f.toString(", ")).append(")");
        query.append(";");
        return e.update(query.toString()) > 0;
    }

    private Object boolsafe(Object defaultValue) {

        if (defaultValue == null) {
            return null;
        }

        if (defaultValue instanceof Reference) {
            return ((Reference<?>) defaultValue).getId();
        }

        if (defaultValue instanceof Boolean) {
            return ((Boolean) defaultValue) ? 1 : 0;
        }

        return defaultValue;
    }

    /**
     * Get the field mapping for this table
     *
     * @return the field mapping
     */
    public KList<ElementField> getFieldMapping() {
        Class<? extends Element> baseClass = getClass();

        return fieldMapping.compute(baseClass, (k, v) -> v == null ? new AtomicCache<>() : v).aquire(() -> {
            KList<ElementField> map = new KList<>();

            L.v("Processing " + baseClass.getSimpleName() + " (" + baseClass.getDeclaredFields().length + " Fields)");

            for (Field i : baseClass.getDeclaredFields()) {
                i.setAccessible(true);
                if (Modifier.isTransient(i.getModifiers()) || Modifier.isStatic(i.getModifiers())) {
                    continue;
                }

                ElementField ef = new ElementField();
                ef.setDefaultValue(ElementUtil.getDefaultValue(i));
                ef.setName(i.getName());
                ef.setSqlName(ElementUtil.getSQLName(i.getName()));
                ef.setField(i);
                ef.setReference(i.getType().equals(Reference.class) && i.isAnnotationPresent(ReferenceType.class));
                ef.setIdentity(i.isAnnotationPresent(Identity.class));
                ef.setSqlType(ElementUtil.getSQLType(i));
                map.add(ef);
            }

            return map;
        });
    }
}
