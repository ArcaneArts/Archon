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

@Data
public abstract class Element
{
    private static final KSet<Class<? extends Element>> synced = new KSet<>();
    private static final KSet<Class<? extends Element>> syncedDone = new KSet<>();
    private static boolean tableExists = false;
    private transient Boolean exists = null;
    private static final Gson gson = buildGson();
    private transient ArchonService archon = Archon.defaultService;
    private static final KMap<Class<? extends Element>, AtomicCache<KList<ElementField>>> fieldMapping = new KMap<>();
    private transient final AtomicCache<ElementField> primaryKey = new AtomicCache<>();
    private transient Element snapshot = null;
    private int dropRequest;

    public abstract String getTableName();

    public int dropTableRequestCode()
    {
        dropRequest = RNG.r.imax();
        return dropRequest;
    }

    public void bruteForceUnregister()
    {
        synced.remove(getClass());
        syncedDone.remove(getClass());
        fieldMapping.remove(getClass());
    }

    public boolean dropTable(int requestCode)
    {
        enforceArchon();

        if(dropRequest == requestCode)
        {
            synced.remove(getClass());
            syncedDone.remove(getClass());
            fieldMapping.remove(getClass());
            getArchon().update("DROP TABLE `" + getTableName() + "`;");
            return true;
        }

        return false;
    }

    public boolean pull()
    {
        enforceArchon();

        try
        {
            sync();
            return pull(archon.query("SELECT * FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;"));
        }

        catch(Throwable e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public ID getIdentityWhere(String field, String value)
    {
        ArchonResult r = getArchon().getReadSQLConnection().query("SELECT `" + getPrimaryField().getSqlName() + "` FROM `"+getTableName()+"` WHERE `" + field + "` = '" + value + "' LIMIT 1;");

        if(r.size() > 0)
        {
            return ID.fromString(r.getRow(0).getString(0));
        }

        return null;
    }

    public boolean where(String field, String value)
    {
        return pull(getArchon().getReadSQLConnection().query("SELECT * FROM `"+getTableName()+"` WHERE `" + field + "` = '" + value + "' LIMIT 1;"));
    }

    public <T extends Element> T archon(ArchonService a)
    {
        setArchon(a);
        return (T) this;
    }

    private void enforceArchon()
    {
        if(!synced.contains(getClass()))
        {
            synced.add(getClass());
            sync();
        }

        if(archon == null)
        {
            throw new NullPointerException("You must use setArchon() on this element to push or pull.");
        }
    }

    public boolean pull(ArchonResult r)
    {
        enforceArchon();
        try
        {
            if(r.size() <= 0)
            {
                return false;
            }

            return pull(r, r.getRow(0));
        }

        catch(Throwable e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public boolean pull(ArchonResult r, ArchonResultRow row)
    {enforceArchon();
        try
        {
            if(r.size() <= 0)
            {
                return false;
            }

            sync();
            for(ElementField i : getFieldMapping())
            {
                try
                {
                    ElementUtil.insert(this, i.getField(), row.get(r.getH().indexOf(i.getSqlName())));
                }

                catch(Throwable e)
                {
                    L.ex(e);
                }
            }

            takeSnapshot();

            return true;
        }

        catch(Throwable e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public <T extends Element> T push()
    {enforceArchon();
        return push(false);
    }

    public <T extends Element> T push(boolean forcePush)
    {enforceArchon();
        if(getPrimaryValue() == null)
        {
            return null;
        }

        if(exists())
        {
            if(snapshot != null)
            {
                KList<ElementField> changed = new KList<>();
                for(ElementField i : getFieldMapping())
                {
                    if(!i.isIdentity())
                    {
                        try {
                            Object s = i.getField().get(snapshot);
                            Object r = i.getField().get(this);

                            if(!ElementUtil.equals(s, r))
                            {
                                changed.add(i);
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if(changed.isNotEmpty())
                {
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

            if(forcePush)
            {
                archon.update("UPDATE `" + getTableName() + "` SET " + getFieldMapping().convert((i) -> {
                    try {
                        return "`" + i.getSqlName() + "` = '" + ElementUtil.escapeString(boolsafe(i.getField().get(Element.this)).toString(), true) + "'";
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return "";
                }).toString(", ") + " WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");
            }
        }

        else
        {
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

    private void takeSnapshot()
    {
        snapshot = gson.fromJson(gson.toJson(this), getClass());
        snapshot.setArchon(getArchon());
    }

    public <T extends Element> ElementList<T> allWhere(String where)
    {
        enforceArchon();
        Class<T> c = (Class<T>) getClass();
        return (ElementList<T>) ElementList.where(getArchon(), getClass(), where);
    }

    public <T extends Element> ElementList<T> allWhere(String where, String orderBy)
    {
        return allWhere(where, orderBy, true);
    }

    public <T extends Element> ElementList<T> allWhere(String where, String orderBy, boolean ascending)
    {
        enforceArchon();
        Class<T> c = (Class<T>) getClass();
        return (ElementList<T>) ElementList.where(getArchon(), getClass(), where, orderBy, ascending);
    }

    public boolean delete()
    {enforceArchon();
        sync();
        int c = archon.update("DELETE FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");
        exists = false;

        return c > 0;
    }

    public boolean exists()
    {enforceArchon();
        if(getPrimaryValue() == null)
        {
            return false;
        }

        if(exists != null)
        {
            return exists;
        }

        sync();
        exists = archon.query("SELECT COUNT(1) FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "';").getRow(0).getInt(0) > 0;
        return exists;
    }

    public String getPrimaryValue()
    {
        try {
            return getPrimaryField().getField().get(this).toString();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    public ElementField getPrimaryField()
    {
        return primaryKey.aquire(() -> {
            for(ElementField i : getFieldMapping())
            {
                if(i.isIdentity())
                {
                    return i;
                }
            }

            return null;
        });
    }

    public void sync()
    {
        if(syncedDone.contains(getClass()))
        {
            return;
        }

        enforceArchon();
        for(ElementField i : getFieldMapping()) {
            if (i.isReference()) {
                Field f = i.getField();
                f.setAccessible(true);

                try {
                    Reference<?> o = (Reference<?>) f.get(this);

                    if (o == null) {
                        L.v("Initialized Reference");
                        f.set(this, Reference.class.getConstructor(Class.class).newInstance(f.getDeclaredAnnotation(ReferenceType.class).value()));
                    }

                    else if(o.getClass() == null)
                    {
                        L.v("Repaired Reference");
                        f.set(this, Reference.class.getConstructor(Class.class, ID.class).newInstance(f.getDeclaredAnnotation(ReferenceType.class).value(), o.getId()));
                    }
                } catch (Throwable e) {
                    L.ex(e);
                }
            }
        }

        if(tableExists)
        {
            return;
        }

        if(!createTable())
        {
            KList<String> cols = tableColumns();
            KList<ElementField> mcols = getFieldMapping();
            KList<ElementField> add = new KList<>();
            KList<String> del = new KList<>();
            KList<String> alt = new KList<>();

            for(ElementField i : mcols)
            {
                if(!cols.contains(i.getSqlName()))
                {
                    add.add(i);
                }
            }

            removing: for(String i : cols)
            {
                for(ElementField j : mcols)
                {
                    if(j.getSqlName().equals(i))
                    {
                        continue removing;
                    }
                }

                del.add(i);
            }

            del.forEach((i) -> alt.add("DROP `" + i + "`"));

            for(ElementField i : add)
            {
                if (i.getDefaultValue() != null && !i.getSqlType().equalsIgnoreCase("TEXT")) {
                    alt.add("ADD `" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL DEFAULT '" + ElementUtil.escapeString(i.getDefaultValue().toString(), true) + "'");
                } else{
                    alt.add("ADD `" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL");
                }
            }

            if(alt.isNotEmpty())
            {
                archon.update("ALTER TABLE `" + getTableName() + "` " + alt.toString(", ") + ";");
            }

            syncedDone.add(getClass());
        }

        else
        {
            sync();
            tableExists = true;
        }
    }

    public long tableSize()
    {enforceArchon();
        sync();
        return archon.query("SELECT COUNT(*) FROM `" + getTableName() + "`")
            .getRow(0)
            .getLong(0);
    }

    private KList<String> tableColumns()
    {enforceArchon();
        return archon.query("SHOW COLUMNS FROM `" + getTableName() + "`")
            .rowFromColumn("COLUMN_NAME")
            .toStringList();
    }

    private ElementField fieldForSQLName(String sqlName)
    {
        for(ElementField i : getFieldMapping())
        {
            if(i.getSqlName().equals(sqlName))
            {
                return i;
            }
        }

        return null;
    }

    private boolean createTable()
    {enforceArchon();
        Edict e = archon.access();
        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS ");
        query.append("`").append(getTableName()).append("` ");
        KList<String> f = new KList<>();
        String pk = "ERROR";
        for(ElementField i : getFieldMapping())
        {
            if(i.isIdentity())
            {
                pk = "(`" + i.getSqlName() + "`)";
            }

            if (i.getDefaultValue() != null && !i.getSqlType().equalsIgnoreCase("TEXT")) {
                f.add("`" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL DEFAULT '" + ElementUtil.escapeString((boolsafe(i.getDefaultValue())).toString(), true) + "'");

            } else{
                f.add("`" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL");
            }
        }

        f.add("PRIMARY KEY " + pk);
        query.append("(").append(f.toString(", ")).append(")");
        query.append(";");
        return e.update(query.toString()) > 0;
    }

    private Object boolsafe(Object defaultValue) {

        if(defaultValue == null)
        {
            return null;
        }

        if(defaultValue instanceof Reference)
        {
            return ((Reference<?>)defaultValue).getId();
        }

        if(defaultValue instanceof Boolean)
        {
            return ((Boolean)defaultValue) ? 1 : 0;
        }

        return defaultValue;
    }

    public KList<ElementField> getFieldMapping()
    {
        Class<? extends Element> baseClass = getClass();

        return fieldMapping.compute(baseClass, (k,v) -> v == null ? new AtomicCache<>() : v).aquire(() -> {
            KList<ElementField> map = new KList<>();

            L.v("Processing " + baseClass.getSimpleName() + " (" + baseClass.getDeclaredFields().length + " Fields)");

            for(Field i : baseClass.getDeclaredFields())
            {
                i.setAccessible(true);
                if(Modifier.isTransient(i.getModifiers()) || Modifier.isStatic(i.getModifiers()))
                {
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
}
