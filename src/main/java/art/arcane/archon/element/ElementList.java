package art.arcane.archon.element;

import art.arcane.archon.Archon;
import art.arcane.archon.data.ArchonResult;
import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.KMap;
import art.arcane.quill.collections.functional.Function2;
import lombok.Data;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
public abstract class ElementList<T extends Element> {
    private Class<? extends T> type;
    private AtomicCache<Long> count;
    private KMap<Integer, ElementListSegment<T>> segmentCache;
    private int chunkSize;

    public ElementList(Class<? extends T> type, int chunkSize)
    {
        segmentCache = new KMap<>();
        this.chunkSize = chunkSize;
        this.type = type;
        count = new AtomicCache<>();
    }

    public abstract ArchonResult getResult(int offset, int limit);

    public int getBaseIndex(int index)
    {
        return (index / chunkSize) * chunkSize;
    }

    public ElementListSegment<T> getSublistFor(int index)
    {
        if(segmentCache.size() * chunkSize > 1024)
        {
            segmentCache.clear();
        }

        return segmentCache.compute(getBaseIndex(index), (k, v) -> v == null ? new ElementListSegment<>(type, () -> getResult(getBaseIndex(index), chunkSize)) : v);
    }

    public abstract long getSize();

    public long size()
    {
        return count.aquire(this::getSize);
    }

    public KList<T> get(int from, int to)
    {
        KList<T> t = new KList<>();

        for(int i = from; i < to; i++)
        {

        }

        return t;
    }

    public T get(int index)
    {
        return getSublistFor(index).get(index - getBaseIndex(index));
    }

    public static <T extends Element> ElementList<T> whereField(Class<? extends T> type, String where, String fieldResult)
    {
        return new ElementList<T>(type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT `" + fieldResult + "` FROM `" + tn + "` WHERE " + where + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }

    public static <T extends Element> ElementList<T> where(Class<? extends T> type, String where)
    {
        return new ElementList<T>(type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT * FROM `" + tn + "` WHERE " + where + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }

    public static <T extends Element> ElementList<T> where(Class<? extends T> type, String where, String orderBy, boolean ascending)
    {
        return new ElementList<T>(type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT * FROM `" + tn + "` WHERE " + where + " ORDER BY `" + orderBy + "` " + (ascending ? "ASC" : "DESC") + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }

    public static <T extends Element> ElementList<T> whereField(Class<? extends T> type, String where, String fieldResult, String orderBy, boolean ascending)
    {
        return new ElementList<T>(type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT `" + fieldResult + "` FROM `" + tn + "` WHERE " + where + " ORDER BY `" + orderBy + "` " + (ascending ? "ASC" : "DESC") + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return Archon.query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }
}
