package art.arcane.archon.element;

import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.KMap;
import art.arcane.quill.logging.L;
import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

@Data
public abstract class Element
{
    private final KMap<Class<? extends Element>, AtomicCache<KMap<String,ElementField>>> fieldMapping = new KMap<>();
    public abstract String getTableName();

    public void verifyTable()
    {

    }

    public KMap<String, ElementField> getFieldMapping()
    {
        Class<? extends Element> baseClass = getClass();

        return fieldMapping.compute(baseClass, (k,v) -> v == null ? new AtomicCache<>() : v).aquire(() -> {
            KMap<String, ElementField> map = new KMap<>();

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
                map.put(i.getName(), ef);
            }

            return map;
        });
    }
}
