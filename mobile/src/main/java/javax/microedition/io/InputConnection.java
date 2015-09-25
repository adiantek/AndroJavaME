package javax.microedition.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract interface InputConnection
        extends Connection
{
    public abstract InputStream openInputStream()
            throws IOException;

    public abstract DataInputStream openDataInputStream()
            throws IOException;
}
