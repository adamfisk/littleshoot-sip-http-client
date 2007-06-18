package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.answer.AnswerProcessor;
import org.lastbamboo.common.offer.OfferGenerator;
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

    private final OfferGenerator m_offerGenerator;

    private final AnswerProcessor m_answerProcessor;
    
    /**
     * Creates a new factory for creating SIP sockets.
     * 
     * @param offerGenerator The class for creating the offer to send with
     * the SIP INVITE message.
     * @param answerProcessor The class for processing any answer received.
     * @param sipClientTracker The class for keeping track of SIP clients.
     */
    public SipSocketFactoryImpl(final OfferGenerator offerGenerator,
        final AnswerProcessor answerProcessor,
        final SipClientTracker sipClientTracker)
        {
        m_offerGenerator = offerGenerator;
        m_answerProcessor = answerProcessor;
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
        
        final SipSocketResolver resolver = new SipSocketResolverImpl (
            this.m_offerGenerator, this.m_answerProcessor, client);

        return (resolver.resolveSocket (sipUri));
        }
    }
