package art.arcane.archon.server;

public interface ArchonConnection
{
    public boolean isConnected();

    public void disconnect();

    public String getName();

    public void connect();

    public default void ensureConnected() {
        if(!isConnected())
        {
            connect();
        }
    }
    public default void ensureDisconnected() {
        if(isConnected())
        {
            disconnect();
        }
    }
}
