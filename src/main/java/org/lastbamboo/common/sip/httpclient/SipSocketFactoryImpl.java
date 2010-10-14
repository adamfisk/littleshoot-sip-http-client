package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import org.lastbamboo.common.offer.answer.OfferAnswerFactory;
import org.lastbamboo.common.p2p.DefaultTcpUdpSocket;
import org.lastbamboo.common.p2p.TcpUdpSocket;
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

    private final OfferAnswerFactory m_offerAnswerFactory;

    private final int m_relayWaitTime;

    /**
     * Creates a new factory for creating SIP sockets.
     *
     * @param sipClientTracker The class for keeping track of SIP clients.
     * @param offerAnswerFactory Factory for creating offers and answers.
     * @param relayWaitTime The number of seconds to wait before using a relay.
     */
    public SipSocketFactoryImpl(final SipClientTracker sipClientTracker,
        final OfferAnswerFactory offerAnswerFactory, final int relayWaitTime)
        {
        this.m_sipClientTracker = sipClientTracker;
        this.m_offerAnswerFactory = offerAnswerFactory;
        this.m_relayWaitTime = relayWaitTime;
        }
    
    public Socket newSocket (final URI sipUri) throws IOException
        {
        m_log.trace ("Creating SIP socket for URI: {}", sipUri);
        final SipClient client = this.m_sipClientTracker.getSipClient();
        if (client == null)
            {
            m_log.warn("No available SIP clients!!");
            throw new IOException (
                "No available connections to SIP proxies!!");
            }
        
        final TcpUdpSocket tcpUdpSocket = 
            new DefaultTcpUdpSocket(client, this.m_offerAnswerFactory,
                this.m_relayWaitTime);
        
        return tcpUdpSocket.newSocket(sipUri);
        }
    }
