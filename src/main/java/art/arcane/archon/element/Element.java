package art.arcane.archon.element;

import art.arcane.archon.Archon;
import art.arcane.archon.server.ArchonServer;
import art.arcane.archon.server.Edict;
import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.KMap;
import art.arcane.quill.logging.L;
import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public abstract class Element
{
    private final KMap<Class<? extends Element>, AtomicCache<KList<ElementField>>> fieldMapping = new KMap<>();

    public abstract String getTableName();

    public boolean sync()
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

        return true;
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
