/*
 * @(#)Protocol.java	1.25 02/09/20 @(#)
 *
 * Copyright (c) 2001-2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.midp.io.j2me.https;

import java.util.Hashtable;
import java.util.Enumeration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.*;

import javax.microedition.pki.*;

import com.sun.midp.ssl.*;

import com.sun.midp.main.Configuration;

import com.sun.midp.io.*;

import com.sun.midp.io.j2me.http.*;

import com.sun.midp.publickeystore.WebPublicKeyStore;

import com.sun.midp.security.*;

/**
 * This class implements the necessary functionality
 * for an HTTPS connection. With support for HTTPS tunneling.
 * <p>
 * Handshake error codes at the beginning of IOException messages:</p>
 * <blockquote><p>
 *   (1) certificate is expired
 * </p><p>
 *   (2) certificate is not yet valid
 * </p><p>
 *   (3)  certificate failed signature verification
 * </p><p>
 *   (4)  certificate was signed using an unsupported algorithm
 * </p><p>
 *   (5)  certificate was issued by an unrecognized certificate authority
 * </p><p>
 *   (6)  certificate does not contain the correct site name
 * </p><p>
 *   (7)  certificate chain exceeds the length allowed
 * </p><p>
 *   (8)  certificate does not contain a signature
 * </p><p>
 *   (9)  version 3 certificate has unrecognized critical extensions
 * </p><p>
 *   (10) version 3 certificate has an inappropriate keyUsage or
 *        extendedKeyUsage extension
 * </p><p>
 *   (11) certificate in the a chain was not issued by the next
 *        authority in the chain
 * </p><p>
 *   (12) trusted certificate authority's public key is expired
 * </p></blockquote>
 */
public class Protocol extends com.sun.midp.io.j2me.http.Protocol
    implements HttpsConnection {

    /** Common name label. */
    private static final String COMMON_NAME_LABEL = "CN=";

    /** Common name label length. */
    private static final int COMMON_NAME_LABEL_LENGTH =
        COMMON_NAME_LABEL.length();

    /** This class has a different security domain than the MIDlet suite */
    private static SecurityToken classSecurityToken;
    
    /**
     * Initializes the security token for this class, so it can
     * perform actions that a normal MIDlet Suite cannot.
     *
     * @param token security token for this class.
     */
    public static void initSecurityToken(SecurityToken token) {
	if (classSecurityToken != null) {
	    return;
	}
	
	classSecurityToken = token;
    }

    /**
     * Parse the common name out of a distinguished name.
     *
     * @param name distinguished name
     *
     * @return common name attribute without the label
     */
    private static String getCommonName(String name) {
        int start;
        int end;

        if (name == null) {
            return null;
        }

        /* The common name starts with "CN=" label */
        start = name.indexOf(COMMON_NAME_LABEL);
        if (start < 0) {
            return null;
        }

        start += COMMON_NAME_LABEL_LENGTH;
        end = name.indexOf(';', start);
        if (end < 0) {
            end = name.length();
        }

        return name.substring(start, end);
    }

    /**
     * Check to see if the site name given by the user matches the site
     * name of subject in the certificate. The method supports the wild card
     * character for the machine name if a domain name is included after it.
     *
     * @param siteName site name the user provided
     * @param certName site name of the subject from a certificate
     *
     * @return true if the common name checks out, else false
     */
    private static boolean checkSiteName(String siteName, String certName) {
        int startOfDomain;
        int domainLength;

        if (certName == null) {
            return false;
        }

        // try the easy way first, ignoring case
        if ((siteName.length() == certName.length()) &&
            siteName.regionMatches(true, 0, certName, 0,
                                   certName.length())) {
            return true;
        }

        if (!certName.startsWith("*.")) {
            // not a wild card, done
            return false;
        }

        startOfDomain = siteName.indexOf('.');
        if (startOfDomain == -1) {
            // no domain name
            return false;
        }

        // skip past the '.'
        startOfDomain++;

        domainLength = siteName.length() - startOfDomain;
        if ((certName.length() - 2) != domainLength) {
            return false;
        }

        // compare the just the domain names, ignoring case
        if (siteName.regionMatches(true, startOfDomain, certName, 2,
                                   domainLength)) {
            return true;
        }

        return false;
    }

    /** collection of "Proxy-" headers as name/value pairs */
    private Properties proxyHeaders = new Properties();

    /** Underlying SSL connection. */
    private SSLStreamConnection sslConnection;

    /**
     * Create a new instance of this class. Override the some of the values
     * in our super class.
     */
    public Protocol() {
        protocol = "https";
        default_port = 443; // 443 is the default port for HTTPS

        requiredPermission = Permissions.HTTPS;
    }

    /** 
     * Get the request header value for the named property.
     * @param key property name of specific HTTP 1.1 header field
     * @return value of the named property, if found, null otherwise.
     */
    public String getRequestProperty(String key) {
        /* https handles the proxy fields in a different way */
        if (key.startsWith("Proxy-")) {
            return proxyHeaders.getProperty(key);
        }

        return super.getRequestProperty(key);
    }

    /**
     * Add the named field to the list of request fields.
     *
     * @param key key for the request header field.
     * @param value the value for the request header field.
     */
    protected void setRequestField(String key, String value) {
        /* https handles the proxy fields in a different way */
        if (key.startsWith("Proxy-")) {
            proxyHeaders.setProperty(key, value);
            return;
        }

        super.setRequestField(key, value);
    }

    /**
     * Connect to the underlying secure socket transport.
     * Perform the SSL handshake and then proceded to the underlying
     * HTTP protocol connect semantics.
     *
     * @return SSL/TCP stream connection
     * @exception IOException is thrown if the connection cannot be opened
     */
    protected StreamConnection connect() throws IOException {
        String httpsTunnel;
        com.sun.midp.io.j2me.socket.Protocol tcpConnection;
        OutputStream tcpOutputStream;
        InputStream tcpInputStream;
        X509Certificate serverCert;

        verifyPermissionCheck();

        /*
         * To save memory for applications the do not use HTTPS,
         * the public keys of the certificate authorities may not
         * have been loaded yet.
         */
        WebPublicKeyStore.loadCertificateAuthorities();

        // Open socket connection
        tcpConnection =
            new com.sun.midp.io.j2me.socket.Protocol();

        // check to see if a protocol is specified for the tunnel
        httpsTunnel = Configuration.getProperty("com.sun.midp.io.http.proxy");
        if (httpsTunnel != null) {
            // Make the connection to the ssl tunnel
            tcpConnection.openPrim(classSecurityToken, "//" + httpsTunnel);

            // Do not delay request since this delays the response.
            tcpConnection.setSocketOption(SocketConnection.DELAY, 0);

            tcpOutputStream = tcpConnection.openOutputStream();
            tcpInputStream = tcpConnection.openInputStream();
            
            // Do the handshake with the ssl tunnel
            try {
                doTunnelHandshake(tcpOutputStream, tcpInputStream);
            } catch (IOException ioe) {
                String temp = ioe.getMessage();

                tcpConnection.close();
                tcpOutputStream.close();
                tcpInputStream.close();

                if (temp.indexOf(" 500 ") > -1) {
                    throw new ConnectionNotFoundException(temp);
                }

                throw ioe;
            }    
        } else {
            tcpConnection.openPrim(classSecurityToken, "//" + hostAndPort);

            // Do not delay request since this delays the response.
            tcpConnection.setSocketOption(SocketConnection.DELAY, 0);

            tcpOutputStream = tcpConnection.openOutputStream();
            tcpInputStream = tcpConnection.openInputStream();
        }

        tcpConnection.close();

        try {
            // Get the SSLStreamConnection
            sslConnection = new SSLStreamConnection(url.host, url.port,
                                tcpInputStream, tcpOutputStream);
        } catch (Exception e) {
            try {
                tcpInputStream.close();
            } finally {
                try {
                    tcpOutputStream.close();
                } finally {
                    if (e instanceof IOException) {
                        throw (IOException)e;
                    } else {
                        throw (RuntimeException)e;
                    }
                }
            }
        }

        try {
            serverCert = sslConnection.getServerCertificate();

            /*
             * if the subject alternate name is a DNS name,
             * then use that instead of the common name for a
             * site name match
             */
            if (serverCert.getSubjectAltNameType() ==
                X509Certificate.TYPE_DNS_NAME) {
                if (!checkSiteName(url.host,
                        (String)serverCert.getSubjectAltName())) {
                    throw new CertificateException(
                        "Subject alternative name did not match site name",
                        serverCert, CertificateException.SITENAME_MISMATCH);
                }
            } else {
                String cname = getCommonName(serverCert.getSubject());
                if (cname == null) {
                    throw new CertificateException(
                        "Common name missing from subject name",
                        serverCert, CertificateException.SITENAME_MISMATCH);
                }
                
                if (!checkSiteName(url.host, cname)) {
                    throw new CertificateException(serverCert,
                        CertificateException.SITENAME_MISMATCH);
                }
            }

            return sslConnection;
        } catch (Exception e) {
            try {
                sslConnection.close();
            } finally {
                if (e instanceof IOException) {
                    throw (IOException)e;
                } else {
                    throw (RuntimeException)e;
                }
            }
        }
    }

    /**
     * disconnect the current connection.
     *
     * @param connection connection return from {@link #connect()}
     * @param inputStream input stream opened from <code>connection</code>
     * @param outputStream output stream opened from <code>connection</code>
     * @exception IOException if an I/O error occurs while
     *                  the connection is terminated.
     */
    protected void disconnect(StreamConnection connection,
           InputStream inputStream, OutputStream outputStream) 
	throws IOException {
        try {
            try {
                inputStream.close();
            } finally {
                try {
                    outputStream.close();
                } finally {
                    connection.close();
                }
            }
        } catch (IOException e) {
        } catch (NullPointerException e) {
        }
    }

    /**
     * Return the security information associated with this connection.
     * If the connection is still in <CODE>Setup</CODE> state then
     * the connection is initiated to establish the secure connection
     * to the server.  The method returns when the connection is
     * established and the <CODE>Certificate</CODE> supplied by the
     * server has been validated.
     * The <CODE>SecurityInfo</CODE> is only returned if the
     * connection has been successfully made to the server.
     *
     * @return the security information associated with this open connection.
     *
     * @exception CertificateException if the <code>Certificate</code>
     * supplied by the server cannot be validated.
     * The <code>CertificateException</code> will contain
     * the information about the error and indicate the certificate in the
     * validation chain with the error.
     * @exception IOException if an arbitrary connection failure occurs
     */
    public SecurityInfo getSecurityInfo() throws IOException {
        ensureOpen();

        sendRequest();

        if (sslConnection == null) {
            /*
             * This is a persistent connection so the connect method did 
             * not get called, so the stream connection of HTTP class
             * will be a SSL connection. Get the info from that.
             */
            StreamConnection sc =
                ((StreamConnectionElement)getStreamConnection()).
                    getBaseConnection();

            return ((SSLStreamConnection)sc).getSecurityInfo();
        }

        return sslConnection.getSecurityInfo();
    }
}
