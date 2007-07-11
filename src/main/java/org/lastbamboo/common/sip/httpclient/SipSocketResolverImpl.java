package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.lastbamboo.common.answer.AnswerProcessor;
import org.lastbamboo.common.offer.OfferGenerator;
import org.lastbamboo.common.sip.client.SipClient;
import org.lastbamboo.common.sip.stack.message.SipMessage;
import org.lastbamboo.common.sip.stack.transaction.client.SipTransactionListener;

/**
 * This class generates a socket using SIP to negotiate the session.  
 * 
 * NOTE: This extra class may seem like an unnecessary abstraction, but it's
 * needed because it uses a locking scheme to create the socket, and a separate 
 * lock is needed for each socket creation.  DO NOT try to fold this class in 
 * to {@link SipSocketFactoryImpl} in an attempt to simplify the code, even
 * if it's tempting to do so.
 */
public final class SipSocketResolverImpl implements SipSocketResolver,
    SipTransactionListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(SipSocketResolverImpl.class);

    /**
     * The factory for creating SDP data for allowing the UAS in the
     * session to contact us.
     */
    private final OfferGenerator m_offerGenerator;

    /**
     * The generated socket.
     */
    private volatile Socket m_socket;

    /**
     * Lock allowing us to wait for event notification either on session
     * establishment failure or upon successful creation of the socket using
     * SDP data.
     */
    private final Object m_socketLock = new Object();

    private final SipClient m_sipClient;

    /**
     * Whether or not we've finished waiting for the socket.
     */
    private boolean m_finishedWaitingForSocket;

    /**
     * The time we started resolving this socket.
     */
    private long m_startTime;

    private final AnswerProcessor m_answerProcessor;

    private ByteBuffer m_offer;

    /**
     * Creates a new socket resolver that uses the specified collaborator
     * classes to create sockets using sip to initiate the session.  The
     * generated socket could ultimately run over UDP, TCP, or whatever -- this
     * will return some class that either is a straight java.net.Socket or
     * a custom subclass.
     * 
     * @param offerGenerator The class for creating the offer to send with
     * the SIP INVITE message.
     * @param answerProcessor The class for processing any answer received.
     * @param sipClient The SIP client for sending SIP messages through a proxy.
     */
    public SipSocketResolverImpl(final OfferGenerator offerGenerator,
        final AnswerProcessor answerProcessor, final SipClient sipClient)
        {
        this.m_offerGenerator = offerGenerator;
        this.m_answerProcessor = answerProcessor;
        this.m_sipClient = sipClient;

        this.m_finishedWaitingForSocket = false;
        }
    
    /**
     * Returns the elapsed time from the start time.  This method assumes that
     * the start time was previously set.
     * 
     * @return The elapsed time from the start time.
     */
    private long getElapsedTime()
        {
        final long now = System.currentTimeMillis();
        final long elapsedTime = now - this.m_startTime;
        
        return elapsedTime;
        }

    /**
     * {@inheritDoc}
     */
    public Socket resolveSocket(final URI sipUri) throws IOException
        {
        LOG.debug("Resolving socket for URI: "+sipUri);
        // Create the SDP string with addresses derived using STUN, TURN,
        // etc.
        final byte[] offer = this.m_offerGenerator.generateOffer();
        this.m_offer = ByteBuffer.wrap(offer);
        this.m_sipClient.invite(sipUri, offer, this);

        return waitForSocket(sipUri);
        }

    /**
     * Waits for the successful creation of a socket or a socket creation
     * error.
     * @param sipUri The URI we're connecting to.
     * @return The new socket.
     * @throws IOException If there's any problem creating the socket.
     */
    private Socket waitForSocket(final URI sipUri) throws IOException
        {
        synchronized (this.m_socketLock)
            {
            m_startTime = System.currentTimeMillis();
            
            while (!m_finishedWaitingForSocket)
                {
                LOG.trace("Waiting for socket...");
                
                try
                    {
                    m_socketLock.wait();
                    }
                catch (final InterruptedException interruptedException)
                    {
                    // Should never happen -- we don't use interrupts here.
                    LOG.error("Unexpectedly interrupted", interruptedException);
                    }
                }

            if (this.m_socket == null)
                {
                LOG.warn("Socket is null...");
                throw new IOException("Could not connect to remote host: "+
                    sipUri);
                }
            else
                {
                LOG.trace("Returning socket!!");
                return this.m_socket;
                }
            }
        }
    
    /**
     * Simply notifies the socket lock that it should stop waiting.  This
     * will happen both when we've successfully created a socket and when
     * there's been an error creating the socket.
     */
    private void notifySocketLock()
        {
        synchronized(this.m_socketLock)
            {
            this.m_finishedWaitingForSocket = true;
            this.m_socketLock.notify();
            }
        }

    /**
     * {@inheritDoc}
     */
    public void onTransactionSucceeded(final SipMessage response)
        {
        LOG.trace("Received INVITE OK");

        if (LOG.isDebugEnabled())
            {
            LOG.debug("Successful transaction after " + getElapsedTime() +
                " milliseconds...");
            }
        // Determine the ICE candidates for socket creation from the
        // response body.
        final ByteBuffer answer = response.getBody();
        
        try
            {
            synchronized (this.m_socketLock)
                {
                m_socket = 
                    this.m_answerProcessor.processAnswer(this.m_offer, answer);
                }
            
            LOG.debug("We resolved the UAC socket!!!");
            }
        catch (final IOException e)
            {
            LOG.debug("Could not resolve the socket", e);
            }
        finally
            {
            // We must always notify the socket lock, since it will wait for us
            // forever.  A socket will only have been created in the absence of
            // any problems.
            notifySocketLock();
            }
        }

    /**
     * {@inheritDoc}
     */
    public void onTransactionFailed(final SipMessage response)
        {
        LOG.warn("Failed transaction after " + getElapsedTime() +
            " milliseconds...");

        // We know the status of the remote host, so make sure the socket
        // fails as quickly as possible.
        notifySocketLock();
        }
    }
