package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import org.lastbamboo.common.offer.answer.OfferAnswerFactory;
import org.lastbamboo.common.rudp.RudpService;
import org.lastbamboo.common.sip.client.SipClient;
import org.lastbamboo.common.sip.client.SipClientTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating a network socket using SIP to establish the connection.
 */
public final class SipSocketFactoryImpl implements SipSocketFactory
    {
    /**
     * The log for this class.
     */
    private static final Logger m_log =
        LoggerFactory.getLogger (SipSocketFactoryImpl.class);

    private final SipClientTracker m_sipClientTracker;

    private final RudpService m_rudpService;

    private final OfferAnswerFactory m_offerAnswerFactory;

    /**
     * Creates a new factory for creating SIP sockets.
     *
     * @param sipClientTracker The class for keeping track of SIP clients.
     * @param rudpService The reliable UDP socket service.
     * @param offerAnswerFactory Factory for creating offers and answers.
     */
    public SipSocketFactoryImpl(final SipClientTracker sipClientTracker,
        final RudpService rudpService, 
        final OfferAnswerFactory offerAnswerFactory)
        {
        this.m_sipClientTracker = sipClientTracker;
        this.m_rudpService = rudpService;
        this.m_offerAnswerFactory = offerAnswerFactory;
        }
    
    public Socket createSipSocket (final URI sipUri) throws IOException
        {
        m_log.trace ("Creating SIP socket for URI: " + sipUri);
        final SipClient client = this.m_sipClientTracker.getSipClient();
        if (client == null)
            {
            m_log.warn("No available SIP clients!!");
            throw new IOException (
                "No available connections to SIP proxies!!");
            }
        
        final TcpUdpSocket tcpUdpSocket = 
            new DefaultTcpUdpSocket(client, this.m_rudpService,
                this.m_offerAnswerFactory);
        
        return tcpUdpSocket.newSocket(sipUri);
        }
    }
