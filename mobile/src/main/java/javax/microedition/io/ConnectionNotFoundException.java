package javax.microedition.io;

import java.io.IOException;

public class ConnectionNotFoundException
        extends IOException
{
    public ConnectionNotFoundException() {}

    public ConnectionNotFoundException(String paramString)
    {
        super(paramString);
    }
}
