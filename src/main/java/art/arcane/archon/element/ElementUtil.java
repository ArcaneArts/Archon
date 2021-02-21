package art.arcane.archon.element;

import art.arcane.quill.logging.L;

import java.lang.reflect.Field;

public class ElementUtil {
    public static String getSQLType(Field i) {
        Type t = i.getDeclaredAnnotation(Type.class);

        if(t != null)
        {
            return t.value();
        }

        Class<?> c = i.getType();

        if(c.isEnum())
        {
            Class<? extends Enum<?>> ec = (Class<? extends Enum<?>>) c;
            StringBuilder sb = new StringBuilder();
            sb.append("ENUM(");
            boolean first = true;

            for(Enum<?> j : ec.getEnumConstants()) {
                if (!first)
                {
                    sb.append(", ");
                }

                sb.append("'").append(j.name()).append("'");
                first = false;
            }

            return sb.append(")").toString();
        }

        if(c.equals(byte.class) || c.equals(Byte.class) || c.equals(boolean.class) || c.equals(Boolean.class))
        {
            return "TINYINT";
        }

        if(c.equals(short.class) || c.equals(Short.class))
        {
            return "SMALLINT";
        }

        if(c.equals(int.class) || c.equals(Integer.class))
        {
            return "INT";
        }

        if(c.equals(long.class) || c.equals(Long.class))
        {
            return "BIGINT";
        }

        if(c.equals(double.class) || c.equals(Double.class))
        {
            return "DOUBLE";
        }

        if(c.equals(float.class) || c.equals(Float.class))
        {
            return "FLOAT";
        }

        if(c.equals(String.class))
        {
            return "CHAR(255)";
        }

        if(c.equals(ID.class))
        {
            return "CHAR(" + ID.LENGTH + ")";
        }

        L.w("Unable to determine SQL Type for " + i.getDeclaringClass().getCanonicalName() + "." + i.getName() + " (Type " + i.getType().getCanonicalName() + ")");

        return null;
    }

    public static Object getDefaultValue(Field i) {
        try
        {
            Element fakeInstance = (Element) i.getDeclaringClass().getConstructor().newInstance();
            i.setAccessible(true);
            return i.get(fakeInstance);
        }

        catch(Throwable ignored)
        {

        }

        return null;
    }

    public static String getSQLName(String fieldName)
    {
        StringBuilder sb = new StringBuilder();

        for(char i : fieldName.toCharArray())
        {
            if(Character.isUpperCase(i))
            {
                sb.append("_").append(Character.toLowerCase(i));
            }

            sb.append(i);
        }

        return sb.toString();
    }

    public static String getFieldName(String sqlName)
    {
        StringBuilder sb = new StringBuilder();
        boolean up = false;

        for(char i : sqlName.toCharArray())
        {
            if(i == '_')
            {
                up = true;
            }

            else
            {
                sb.append(up ? Character.toUpperCase(i) : i);
                up = false;
            }
        }

        return sb.toString();
    }
}
