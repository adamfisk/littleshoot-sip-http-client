package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.prefs.Preferences;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.sip.stack.SipUriFactory;

/**
 * Socket factory for creating SIP-negotiated sockets for HTTP clients.
 */
public final class SipProtocolSocketFactory implements ProtocolSocketFactory
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(SipProtocolSocketFactory.class);
    
    private final SipSocketFactory m_sipSocketFactory;

    private final SipUriFactory m_sipUriFactory;

    /**
     * Creates a new factory for creating sockets from SIP URIs.
     * 
     * @param sipSocketFactory The factory that actually creates the sockets.
     * @param sipUriFactory The class for creating SIP URIs from the host
     * names from HTTP client.
     */
    public SipProtocolSocketFactory(final SipSocketFactory sipSocketFactory,
        final SipUriFactory sipUriFactory)
        {
        this.m_sipSocketFactory = sipSocketFactory;
        this.m_sipUriFactory = sipUriFactory;
        }

    public Socket createSocket(final String host, final int port, 
        final InetAddress clientHost, final int clientPort)
        {
        LOG.warn("Attempted unsupported socket call");
        throw new UnsupportedOperationException("not allowed");
        }

    public Socket createSocket(final String host, final int port, 
        final InetAddress clientHost, final int clientPort, 
        final HttpConnectionParams params) throws IOException, 
        UnknownHostException, ConnectTimeoutException
        {
        LOG.trace("Creating a socket for user: "+host);
        return createSocket(host, port);
        }
    
    public Socket createSocket(final String host, final int port) 
        throws IOException, UnknownHostException
        {
        LOG.trace("Creating a socket for user: "+host);
        final Preferences prefs = Preferences.userRoot();
        final long id = prefs.getLong("LITTLESHOOT_ID", -1);
        if (id == Long.parseLong(host))
            {
            LOG.debug("Ignoring request to download from ourselves...");
            throw new IOException("Not downloading from ourselves...");
            }
        
        final URI sipUri = this.m_sipUriFactory.createSipUri(host);
        try 
            {
            LOG.trace("About to create socket...");
            return this.m_sipSocketFactory.createSipSocket(sipUri);
            }
        catch (final IOException e)
            {
            LOG.warn("Exception creating SIP socket", e);
            throw e;
            }
        }
    }
