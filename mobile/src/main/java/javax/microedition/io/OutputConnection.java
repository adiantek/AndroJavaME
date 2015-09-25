package javax.microedition.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract interface OutputConnection
        extends Connection
{
    public abstract OutputStream openOutputStream()
            throws IOException;

    public abstract DataOutputStream openDataOutputStream()
            throws IOException;
}
