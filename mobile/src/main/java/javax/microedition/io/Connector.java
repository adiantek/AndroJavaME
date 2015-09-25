package javax.microedition.io;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Connector
{
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;
    private static String platform;
    private static boolean j2me = false;
    private static String classRoot;

    public static Connection open(String paramString)
            throws IOException
    {
        return open(paramString, 3);
    }

    public static Connection open(String paramString, int paramInt)
            throws IOException
    {
        return open(paramString, paramInt, false);
    }

    public static Connection open(String paramString, int paramInt, boolean paramBoolean)
            throws IOException
    {
        if (platform != null) {
            try
            {
                return openPrim(paramString, paramInt, paramBoolean, platform);
            }
            catch (ClassNotFoundException localClassNotFoundException1) {}
        }
        try
        {
            return openPrim(paramString, paramInt, paramBoolean, j2me ? "j2me" : "j2se");
        }
        catch (ClassNotFoundException localClassNotFoundException2)
        {
            throw new ConnectionNotFoundException("The requested protocol does not exist " + paramString);
        }
    }

    private static Connection openPrim(String paramString1, int paramInt, boolean paramBoolean, String paramString2)
            throws IOException, ClassNotFoundException
    {
        if (paramString1 == null) {
            throw new IllegalArgumentException("Null URL");
        }
        int i = paramString1.indexOf(':');
        if (i < 1) {
            throw new IllegalArgumentException("no ':' in URL");
        }
        try
        {
            String str = paramString1.substring(0, i);
            paramString1 = paramString1.substring(i + 1);
            Class localClass = Class.forName(classRoot + "." + paramString2 + "." + str + ".Protocol");
            ConnectionBaseInterface localConnectionBaseInterface = (ConnectionBaseInterface)localClass.newInstance();
            return localConnectionBaseInterface.openPrim(paramString1, paramInt, paramBoolean);
        }
        catch (InstantiationException localInstantiationException)
        {
            throw new IOException(localInstantiationException.toString());
        }
        catch (IllegalAccessException localIllegalAccessException)
        {
            throw new IOException(localIllegalAccessException.toString());
        }
        catch (ClassCastException localClassCastException)
        {
            throw new IOException(localClassCastException.toString());
        }
    }

    public static DataInputStream openDataInputStream(String paramString)
            throws IOException
    {
        InputConnection localInputConnection = (InputConnection)open(paramString, 1);
        try
        {
            DataInputStream localDataInputStream = localInputConnection.openDataInputStream();
            return localDataInputStream;
        }
        finally
        {
            localInputConnection.close();
        }
    }

    public static DataOutputStream openDataOutputStream(String paramString)
            throws IOException
    {
        OutputConnection localOutputConnection = (OutputConnection)open(paramString, 2);
        try
        {
            DataOutputStream localDataOutputStream = localOutputConnection.openDataOutputStream();
            return localDataOutputStream;
        }
        finally
        {
            localOutputConnection.close();
        }
    }

    public static InputStream openInputStream(String paramString)
            throws IOException
    {
        return openDataInputStream(paramString);
    }

    public static OutputStream openOutputStream(String paramString)
            throws IOException
    {
        return openDataOutputStream(paramString);
    }

    static
    {
        if (System.getProperty("microedition.configuration") != null) {
            j2me = true;
        }
        platform = System.getProperty("microedition.platform");
        classRoot = System.getProperty("javax.microedition.io.Connector.protocolpath");
        if (classRoot == null) {
            classRoot = "com.sun.cldc.io";
        }
    }
}
