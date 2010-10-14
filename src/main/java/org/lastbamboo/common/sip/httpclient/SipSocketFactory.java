package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

/**
 * Interface for classes that can create {@link Socket}s from SIP URIs.
 */
public interface SipSocketFactory
    {

    /**
     * Resolves the specified SIP <code>URI</code> into a <code>Socket</code>
     * instance for reading and writing data.
     * 
     * @param sipUri The <code>URI</code> for the SIP UA to connect to.
     * @return A <code>Socket</code> connection to the remote SIP UA.
     * @throws IOException If the socket could not be created for any reason,
     * such as an unsupported media type or a network error.
     */
    Socket newSocket(URI sipUri) throws IOException;

    }
