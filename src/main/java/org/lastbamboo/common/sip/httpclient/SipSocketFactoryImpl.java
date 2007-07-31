package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.offer.answer.OfferAnswer;
import org.lastbamboo.common.offer.answer.OfferAnswerFactory;
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

    private final SipClientTracker m_sipClientTracker;

    private final OfferAnswerFactory m_offerAnswerFactory;
    
    /**
     * Creates a new factory for creating SIP sockets.
     * @param offerAnswerFactory 
     * 
     * @param sipClientTracker The class for keeping track of SIP clients.
     */
    public SipSocketFactoryImpl(
        final OfferAnswerFactory offerAnswerFactory,
        final SipClientTracker sipClientTracker)
        {
        m_offerAnswerFactory = offerAnswerFactory;
        m_sipClientTracker = sipClientTracker;
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
        
        //final IceAgent agent = 
          //  this.m_iceAgentFactory.createControllingIceAgent();
        
        final OfferAnswer offerAnswer = 
            this.m_offerAnswerFactory.createOfferer();
        
        final SipSocketResolver resolver = 
            new SipSocketResolverImpl(offerAnswer, client);

        return (resolver.resolveSocket (sipUri));
        }
    }
