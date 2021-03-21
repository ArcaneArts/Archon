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
import art.arcane.quill.collections.KMap;
import art.arcane.quill.logging.L;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.UUID;

public class ElementUtil {
    private static final KMap<Class<?>, String> tableNameCache = new KMap<>();
    private static final boolean CUSTOM_NAMES = false;

    public static String getSQLType(Field i) {
        Type t = i.getDeclaredAnnotation(Type.class);

        if (t != null) {
            return t.value();
        }

        Class<?> c = i.getType();

        if (c.isEnum()) {
            Class<? extends Enum<?>> ec = (Class<? extends Enum<?>>) c;
            StringBuilder sb = new StringBuilder();
            sb.append("ENUM(");
            boolean first = true;

            for (Enum<?> j : ec.getEnumConstants()) {
                if (!first) {
                    sb.append(", ");
                }

                sb.append("'").append(j.name()).append("'");
                first = false;
            }

            return sb.append(")").toString();
        }

        if (c.equals(byte.class) || c.equals(Byte.class) || c.equals(boolean.class) || c.equals(Boolean.class)) {
            return "TINYINT";
        }

        if (c.equals(short.class) || c.equals(Short.class)) {
            return "SMALLINT";
        }

        if (c.equals(int.class) || c.equals(Integer.class)) {
            return "INT";
        }

        if (c.equals(long.class) || c.equals(Long.class)) {
            return "BIGINT";
        }

        if (c.equals(double.class) || c.equals(Double.class)) {
            return "DOUBLE";
        }

        if (c.equals(float.class) || c.equals(Float.class)) {
            return "FLOAT";
        }

        if (c.equals(String.class)) {
            return "CHAR(255)";
        }

        if (c.equals(ID.class)) {
            return "CHAR(" + ID.LENGTH + ")";
        }

        if (c.equals(Reference.class)) {
            return "CHAR(" + ID.LENGTH + ")";
        }

        L.w("Unable to determine SQL Type for " + i.getDeclaringClass().getCanonicalName() + "." + i.getName() + " (Type " + i.getType().getCanonicalName() + ")");

        return null;
    }

    public static Object getDefaultValue(Field i) {
        try {
            Element fakeInstance = (Element) i.getDeclaringClass().getConstructor().newInstance();
            i.setAccessible(true);
            return i.get(fakeInstance);
        } catch (Throwable ignored) {

        }

        return null;
    }

    public static String getSQLName(String fieldName) {
        if (CUSTOM_NAMES) {
            StringBuilder sb = new StringBuilder();

            for (char i : fieldName.toCharArray()) {
                if (Character.isUpperCase(i)) {
                    sb.append("_").append(Character.toLowerCase(i));
                    continue;
                }

                sb.append(i);
            }

            return sb.toString();
        }
        return fieldName;
    }

    public static String getFieldName(String sqlName) {
        if (CUSTOM_NAMES) {
            StringBuilder sb = new StringBuilder();
            boolean up = false;

            for (char i : sqlName.toCharArray()) {
                if (i == '_') {
                    up = true;
                } else {
                    sb.append(up ? Character.toUpperCase(i) : i);
                    up = false;
                }
            }

            return sb.toString();
        }

        return sqlName;
    }

    public static String escapeString(String x, boolean escapeDoubleQuotes) {
        StringBuilder sBuilder = new StringBuilder(x.length() * 11 / 10);

        int stringLength = x.length();

        for (int i = 0; i < stringLength; ++i) {
            char c = x.charAt(i);

            switch (c) {
                case 0: /* Must be escaped for 'mysql' */
                    sBuilder.append('\\');
                    sBuilder.append('0');

                    break;

                case '\n': /* Must be escaped for logs */
                    sBuilder.append('\\');
                    sBuilder.append('n');

                    break;

                case '\r':
                    sBuilder.append('\\');
                    sBuilder.append('r');

                    break;

                case '\\':
                    sBuilder.append('\\');
                    sBuilder.append('\\');

                    break;

                case '\'':
                    sBuilder.append('\\');
                    sBuilder.append('\'');

                    break;

                case '"': /* Better safe than sorry */
                    if (escapeDoubleQuotes) {
                        sBuilder.append('\\');
                    }

                    sBuilder.append('"');

                    break;

                case '\032': /* This gives problems on Win32 */
                    sBuilder.append('\\');
                    sBuilder.append('Z');

                    break;

                case '\u00a5':
                case '\u20a9':
                    // escape characters interpreted as backslash by mysql
                    // fall through

                default:
                    sBuilder.append(c);
            }
        }

        return sBuilder.toString();
    }

    public static void insert(Object object, Field i, Object o) throws IllegalAccessException {
        if (i.getType().equals(Reference.class)) {
            Reference<?> r = (Reference<?>) i.get(object);
            r.setId(ID.fromString(o.toString()));
        }

        if (i.getType().equals(UUID.class)) {
            i.set(object, UUID.fromString(o.toString()));
        }

        if (i.getType().equals(ID.class)) {
            i.set(object, ID.fromString(o.toString()));
        } else if (i.getType().equals(String.class)) {
            i.set(object, o.toString());
        } else if (i.getType().equals(Byte.class) || i.getType().equals(byte.class)) {
            i.set(object, Byte.valueOf(o.toString()));
        } else if (i.getType().equals(Integer.class) || i.getType().equals(int.class)) {
            i.set(object, Integer.valueOf(o.toString()));
        } else if (i.getType().equals(Double.class) || i.getType().equals(double.class)) {
            i.set(object, Double.valueOf(o.toString()));
        } else if (i.getType().equals(Long.class) || i.getType().equals(long.class)) {
            i.set(object, Long.valueOf(o.toString()));
        } else if (i.getType().equals(Boolean.class) || i.getType().equals(boolean.class)) {
            i.set(object, Integer.parseInt(o.toString()) == 1);
        } else {
            System.out.println("Cannot handle type injection from table: " + i.getType().toString());
        }
    }

    public static boolean equals(Object s, Object r) {
        if (s != null && r != null) {
            if (s.getClass().equals(Reference.class) && r.getClass().equals(Reference.class)) {
                return equals(((Reference<?>) s).getId(), ((Reference<?>) r).getId());
            }
        }

        if ((s == null) != (r == null)) {
            return false;
        }

        if (s == null) {
            return false;
        }

        return Objects.equals(s, r);
    }

    public static String getTableName(Class<?> type) {
        return tableNameCache.compute(type, (k, v) -> {
            if (v == null) {
                try {
                    return ((Element) type.getConstructor().newInstance()).getTableName();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            return v;
        });
    }
}
