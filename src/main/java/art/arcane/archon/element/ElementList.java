package art.arcane.archon.element;

import art.arcane.archon.data.ArchonResult;
import art.arcane.archon.server.ArchonServiceWorker;
import art.arcane.quill.cache.AtomicCache;
import art.arcane.quill.collections.KList;
import art.arcane.quill.collections.KMap;
import lombok.Data;

@Data
public abstract class ElementList<T extends Element> {
    private Class<? extends T> type;
    private AtomicCache<Long> count;
    private KMap<Integer, ElementListSegment<T>> segmentCache;
    private int chunkSize;
    private ArchonServiceWorker archon;

    public ElementList(ArchonServiceWorker archon, Class<? extends T> type, int chunkSize)
    {
        this.archon = archon;
        segmentCache = new KMap<>();
        this.chunkSize = chunkSize;
        this.type = type;
        count = new AtomicCache<>();
    }

    public abstract ArchonResult getResult(int offset, int limit);

    public KList<T> toList()
    {
        KList<T> v = new KList<>();
        for(int i = 0; i < size(); i++)
        {
            v.add(get(i));
        }

        return v;
    }

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

    public static <T extends Element> ElementList<T> whereField(ArchonServiceWorker archon, Class<? extends T> type, String where, String fieldResult)
    {
        return new ElementList<T>(archon, type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT `" + fieldResult + "` FROM `" + tn + "` WHERE " + where + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }

    public static <T extends Element> ElementList<T> where(ArchonServiceWorker archon, Class<? extends T> type, String where)
    {
        return new ElementList<T>(archon, type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT * FROM `" + tn + "` WHERE " + where + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }

    public static <T extends Element> ElementList<T> where(ArchonServiceWorker archon, Class<? extends T> type, String where, String orderBy, boolean ascending)
    {
        return new ElementList<T>(archon, type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT * FROM `" + tn + "` WHERE " + where + " ORDER BY `" + orderBy + "` " + (ascending ? "ASC" : "DESC") + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }

    public static <T extends Element> ElementList<T> whereField(ArchonServiceWorker archon, Class<? extends T> type, String where, String fieldResult, String orderBy, boolean ascending)
    {
        return new ElementList<T>(archon, type, 128) {
            @Override
            public ArchonResult getResult(int offset, int limit) {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT `" + fieldResult + "` FROM `" + tn + "` WHERE " + where + " ORDER BY `" + orderBy + "` " + (ascending ? "ASC" : "DESC") + " LIMIT " + offset + "," + limit + ";");
            }

            @Override
            public long getSize() {
                String tn = ElementUtil.getTableName(type);
                return getArchon().query("SELECT COUNT(*) FROM `" + tn + "` WHERE " + where + ";").getRow(0).getLong(0);
            }
        };
    }
}
