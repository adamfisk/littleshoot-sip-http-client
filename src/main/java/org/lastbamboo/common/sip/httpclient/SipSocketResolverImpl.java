package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.lastbamboo.common.ice.IceCandidateFactory;
import org.lastbamboo.common.ice.IceCandidateTracker;
import org.lastbamboo.common.ice.IceException;
import org.lastbamboo.common.ice.UacIceCandidateTracker;
import org.lastbamboo.common.ice.sdp.SdpFactory;
import org.lastbamboo.common.sdp.api.SdpException;
import org.lastbamboo.common.sdp.api.SdpParseException;
import org.lastbamboo.common.sdp.api.SessionDescription;
import org.lastbamboo.common.sip.client.SipClient;
import org.lastbamboo.common.sip.stack.message.SipMessage;
import org.lastbamboo.common.sip.stack.transaction.client.SipTransactionListener;
import org.lastbamboo.common.util.mina.MinaUtils;

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
    private final SdpFactory m_sdpFactory;

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

    private final IceCandidateTracker m_iceCandidateTracker;

    private final SipClient m_sipClient;

    private final IceCandidateFactory m_iceCandidateFactory;

    /**
     * Whether or not we've finished waiting for the socket.
     */
    private boolean m_finishedWaitingForSocket;

    /**
     * The time we started resolving this socket.
     */
    private long m_startTime;

    /**
     * Creates a new socket resolver that uses the specified collaborator
     * classes to create sockets using sip to initiate the session.  The
     * generated socket could ultimately run over UDP, TCP, or whatever -- this
     * will return some class that either is a straight java.net.Socket or
     * a custom subclass.
     * 
     * @param factory The factory for creating the SDP to pass over SIP.
     * @param sipClient The SIP client for sending SIP messages through a proxy.
     * @param iceCandidateFactory The factory for creating ICE candidates from
     * the remote host's SDP.
     */
    public SipSocketResolverImpl(
        final SdpFactory factory,
        final SipClient sipClient, 
        final IceCandidateFactory iceCandidateFactory)
        {
        this.m_sdpFactory = factory;
        this.m_sipClient = sipClient;

        this.m_iceCandidateFactory = iceCandidateFactory;

        // Create our new tracker for ICE connection candidates now, as this
        // will be collecting data at various points of the SIP negotiation.
        this.m_iceCandidateTracker = new UacIceCandidateTracker();
        
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
        try
            {
            // Create the SDP string with addresses derived using STUN, TURN,
            // etc.
            final SessionDescription sdp = this.m_sdpFactory.createSdp();
            LOG.debug("Sending SDP: "+sdp.toString());
            this.m_sipClient.invite(sipUri, sdp.toBytes(), this);

            return waitForSocket(sipUri);
            }
        catch (final SdpException e)
            {
            LOG.warn("Could not create SDP", e);
            throw new IOException("Could not handle SDP: "+e.getMessage());
            }
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
        
        try
            {
            // Determine the ICE candidates for socket creation from the
            // response body.
            final ByteBuffer responseBody = response.getBody();
            final String responseBodyString = 
                MinaUtils.toAsciiString(responseBody);
            
            final SessionDescription sdp = 
                m_sdpFactory.createSdp(responseBodyString);
            
            final Collection iceCandidates =
                m_iceCandidateFactory.createCandidates(sdp);
    
            if (iceCandidates.isEmpty())
                {
                // Give up when there are no ICE candidates.
                LOG.warn("No ICE candidates!!");
                }
            else
                {
                this.m_iceCandidateTracker.visitCandidates(iceCandidates);
        
                try
                    {
                    synchronized (this.m_socketLock)
                        {
                        m_socket = m_iceCandidateTracker.getBestSocket();
                        }
                    
                    LOG.trace("We resolved the UAC socket!!!");
                    }
                catch (final IceException e)
                    {
                    LOG.debug("Could not connect!!", e);
                    }
                }
            }
        catch (final SdpParseException e)
            {
            LOG.warn("Could not parse SDP", e);
            // TODO Send an error response.
            }
        catch (final SdpException e)
            {
            LOG.warn("Could not read SDP", e);
            // TODO Send an error response.
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
        LOG.warn("Request timed out...");

        if (LOG.isDebugEnabled())
            {
            LOG.warn("Failed transaction after " + getElapsedTime() +
                " milliseconds...");
            }

        // We know the status of the remote host, so make sure the socket
        // fails as quickly as possible.
        notifySocketLock();
        }
    }
