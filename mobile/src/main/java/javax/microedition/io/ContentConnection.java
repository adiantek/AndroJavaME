package javax.microedition.io;

public abstract interface ContentConnection
        extends StreamConnection
{
    public abstract String getType();

    public abstract String getEncoding();

    public abstract long getLength();
}
