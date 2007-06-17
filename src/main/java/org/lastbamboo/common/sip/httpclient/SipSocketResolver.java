package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

/**
 * Interface for classes that can create sockets using SIP to establish the
 * session.
 */
public interface SipSocketResolver
    {

    /**
     * Creates a SIP socket from the specified SIP URI, using the URI to 
     * establish a session with the remote host.
     * 
     * @param sipUri The SIP URI to connect to.
     * @return A socket or socket subclass for a connection to the remote host.
     * @throws IOException If any IO error occurs establishing the socket.
     */
    Socket resolveSocket(final URI sipUri) throws IOException;

    }
