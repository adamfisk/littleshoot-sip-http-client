package org.lastbamboo.common.sip.httpclient;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

public interface TcpUdpSocket
    {

    Socket newSocket(URI sipUri) throws IOException;
    }
