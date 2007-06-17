package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.ice.IceCandidateFactory;
import org.lastbamboo.common.ice.sdp.SdpFactory;
import org.lastbamboo.common.sip.client.SipClient;
import org.lastbamboo.common.sip.client.SipClientTracker;

/**
 * Factory for creating a network socket using SIP to establish the connection.
 */
public final class SipSocketFactoryImpl implements SipSocketFactory
    {
    /**
     * The log for this class.
     */
    private static final Log LOG =
        LogFactory.getLog (SipSocketFactoryImpl.class);
    
    /**
     * The factory to use for creating SDP data.
     */
    private final SdpFactory m_sdpFactory;

    private final IceCandidateFactory m_iceCandidateFactory;

    private final SipClientTracker m_sipClientTracker;
    
    /**
     * Creates a new factory for creating SIP sockets.
     * 
     * @param sipClientTracker The class for keeping track of SIP clients.
     * @param iceCandidateFactory The factory for creating ICE candidates from
     * wire SDP data.
     * @param sdpFactory The class for creating SDP from wire data.
     */
    public SipSocketFactoryImpl(final SipClientTracker sipClientTracker,
        final IceCandidateFactory iceCandidateFactory,
        final SdpFactory sdpFactory)
        {
        this.m_sipClientTracker = sipClientTracker;
        this.m_iceCandidateFactory = iceCandidateFactory;
        this.m_sdpFactory = sdpFactory;
        }
    
    public Socket createSipSocket (final URI sipUri) throws IOException
        {
        LOG.trace ("Creating SIP socket for URI: " + sipUri);
        final SipClient client = this.m_sipClientTracker.getSipClient();
        if (client == null)
            {
            LOG.warn("No available SIP clients!!");
            throw new IOException (
                "No available connections to SIP proxies!!");
            }
        
        final SipSocketResolver resolver = new SipSocketResolverImpl (
            this.m_sdpFactory, client, this.m_iceCandidateFactory);

        return (resolver.resolveSocket (sipUri));
        }
    }
