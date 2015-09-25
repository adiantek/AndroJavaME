package javax.microedition.io;

import java.io.IOException;

public abstract interface Connection
{
    public abstract void close()
            throws IOException;
}
