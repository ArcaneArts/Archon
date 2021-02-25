package art.arcane.archon.element;

import art.arcane.archon.Archon;
import art.arcane.archon.data.ArchonResult;
import art.arcane.archon.server.ArchonServer;
import art.arcane.archon.server.Edict;
import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.KMap;
import art.arcane.quill.execution.J;
import art.arcane.quill.execution.parallel.MultiBurst;
import art.arcane.quill.logging.L;
import com.google.gson.Gson;
import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Data
public abstract class Element
{
    private static final Gson gson = new Gson();
    private static final KMap<Class<? extends Element>, AtomicCache<KList<ElementField>>> fieldMapping = new KMap<>();
    private transient final AtomicCache<ElementField> primaryKey = new AtomicCache<>();
    private transient Element snapshot = null;

    public abstract String getTableName();

    public boolean pull()
    {
        try
        {
            ArchonResult r = Archon.query(getObjectKey() + ":data", "SELECT * FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");

            for(ElementField i : getFieldMapping())
            {
                if(!i.isIdentity())
                {
                    try
                    {
                        ElementUtil.insert(this, i.getField(), r.getRow(0).get(r.getH().indexOf(i.getSqlName())));
                    }

                    catch(Throwable e)
                    {
                        L.ex(e);
                    }
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

    private void takeSnapshot()
    {
        snapshot = null;
        MultiBurst.burst.lazy(() -> snapshot = gson.fromJson(gson.toJson(this), getClass()));
    }

    private void takeSnapshotNow()
    {
        snapshot = gson.fromJson(gson.toJson(this), getClass());
    }

    public void delete()
    {
        Archon.update(getObjectKey() + ":*", "DELETE FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "' LIMIT 1;");
    }

    public void push()
    {
        if(getPrimaryValue() == null)
        {
            return;
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
                    Archon.update(getObjectKey() + ":data", "UPDATE `" + getTableName() + "` SET " + changed.convert((i) -> {
                        try {
                            return "`" + i.getSqlName() + "` = '" + ElementUtil.escapeString(i.getField().get(Element.this).toString(), true) + "'";
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        return "";
                    }).toString(", ") + ";");

                    takeSnapshot();
                    return;
                }
            }

            Archon.update(getObjectKey() + ":data", "UPDATE `" + getTableName() + "` SET " + getFieldMapping().convert((i) -> {
                try {
                    return "`" + i.getSqlName() + "` = '" + ElementUtil.escapeString(i.getField().get(Element.this).toString(), true) + "'";
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return "";
            }).toString(", ") + ";");
        }

        else
        {
            Archon.update(getObjectKey() + ":exists", "INSERT INTO `" + getTableName() + "` (" + getFieldMapping().convert(ElementField::getSqlName).toString(", ") + ") VALUES (" + getFieldMapping().convert((i) -> {
                try {
                    return "'" + ElementUtil.escapeString(i.getField().get(Element.this).toString(), true) + "'";
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return "";
            }).toString(", ") + ");");
        }

        takeSnapshot();
    }

    public String getObjectKey()
    {
        return getTableName() + ":" + getPrimaryField().getSqlName() + ":" + getPrimaryValue();
    }

    public boolean exists()
    {
        if(getPrimaryValue() == null)
        {
            return false;
        }

        return Archon.query(getObjectKey() + ":exists", "SELECT COUNT(1) FROM `" + getTableName() + "` WHERE `" + getPrimaryField().getSqlName() + "` = '" + getPrimaryValue() + "';").getRow(0).getInt(0) > 0;
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
        if(!createTable())
        {
            KList<String> cols = tableColumns();
            KList<ElementField> mcols = getFieldMapping();
            KList<ElementField> add = new KList<>();
            KList<String> del = new KList<>();
            KList<String> alt = new KList<>();
            KMap<String, ElementField> altCols = new KMap<>();
            StringBuilder query = new StringBuilder();

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
                Archon.update("ALTER TABLE `" + getTableName() + "` " + alt.toString(", ") + ";");
            }
        }
    }

    public long tableSize()
    {
        return Archon.query("SELECT COUNT(*) FROM `" + getTableName() + "`")
            .getRow(0)
            .getLong(0);
    }

    private KList<String> tableColumns()
    {
        return Archon.query("SHOW COLUMNS FROM `" + getTableName() + "`")
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
    {
        Edict e = ArchonServer.get().access();
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
                f.add("`" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL DEFAULT '" + ElementUtil.escapeString(i.getDefaultValue().toString(), true) + "'");
            } else{
                f.add("`" + i.getSqlName() + "` " + i.getSqlType() + " NOT NULL");
            }
        }

        f.add("PRIMARY KEY " + pk);
        query.append("(").append(f.toString(", ")).append(")");
        query.append(";");
        return e.update(query.toString()) > 0;
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
                ef.setIdentity(i.isAnnotationPresent(Identity.class));
                ef.setSqlType(ElementUtil.getSQLType(i));
                map.add(ef);
            }

            return map;
        });
    }
}
