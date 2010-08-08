/*
 * Copyright 2010 Juan Alberto López Cavallotti.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.juancavallotti.websockets4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the web Sockets Server. This server implements the WebSockets protocol.<br />
 * The protocol versions currently supported are from draft 75 and 76. <br />
 * At the moment, no subprotocol is implemented but the Subprotocol header is
 * answered at the moment of handshaking. <br /><br />
 * The easiest way to use this class in Java EE 6 is like the following:
 *
 * <pre>
 * {@code
 * import com.juancavallotti.websockets4j.WebSocketServer;
 * import javax.inject.Named;
 * import javax.inject.Singleton;
 *
 * \@Named
 * \@Singleton
 * public final class Server extends WebSocketServer {
 *
 * }
 * }
 * </pre>
 *
 * The server will be available for injections using CDI. It is also suggested
 * to be started by a web listener. The following code shows the sugested startup:
 *
 * <pre>
 * {@code
 * \@WebListener
 * public class WebappListener implements Serializable, ServletContextListener {
 *
 *
 *    \@Inject
 *    private Server server;
 *
 *    private static final Logger log = Logger.getLogger(WebappListener.class.getName());
 *
 *    \@Override
 *    public void contextInitialized(ServletContextEvent sce) {
 *        if (!server.isStarted()) {
 *           server.start();
 *           server.registerListener("/context",<i>some class implementing {@link WebScoketListner}</i>);
 *           log.log(Level.INFO, "Started WebSocketServer");
 *       }
 *    }
 *
 *    \@Override
 *    public void contextDestroyed(ServletContextEvent sce) {
 *            server.stop();
 *            log.log(Level.INFO, "Stopped WebSocketServer");
 *    }
 *}
 * }
 * </pre>
 *
 * @author Juan Alberto López Cavallotti
 */
public class WebSocketServer implements Serializable {

    private ServerSocket server;
    private final HashMap<String, LinkedList<WebSocketListener>> listeners = new HashMap<String, LinkedList<WebSocketListener>>();
    private static final int defaultPort = 10123;
    private static final int defaultBacklog = 40;
    private static final Logger log = Logger.getLogger(WebSocketServer.class.getName());

    /**
     * Defines if the server is listening for connections or not.
     * @return true if the server is started and listening for connections.
     */
    public boolean isStarted() {
        return server != null;
    }

    /**
     * Starts the websocket service. The service will be listening on any interface. <br />
     * The TCP port that will be listen defaults to 10123 but 'websocket4j.port'
     * system property may be setted up to change this value.'
     * The serverSocket backlog (the ammount of simultaneous clients) defaults
     * to 40 clients but 'websocket4j.backlog' system property may be setted up
     * to change this value.<br />
     * The server is not waranteed to be running afther this method call, the state
     * may be checked using the {@link  #isStarted} method.
     */
    public void start() {
        try {
            String portProp = System.getProperty("websocket4j.port");
            String backlogProp = System.getProperty("websocket4j.backlog");

            int port = portProp == null ? defaultPort : Integer.parseInt(portProp);
            int backlog = backlogProp == null ? defaultBacklog : Integer.parseInt(backlogProp);

            server = new ServerSocket(port, backlog);
            new ListenThread().start();
        } catch (IOException ex) {
            server = null;
            log.log(Level.SEVERE, "Error While Starting the server", ex);
        }
    }

    /**
     * Stops the server so it doesn't listen on the port any longer. Al current
     * client connections will be closed and the server will be shutted down.
     */
    public void stop() {

        if (server == null || server.isClosed()) {
            server = null;
            return;
        }

        try {
            server.close();
            server = null;
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error While stoping the server", ex);
        }
    }

    /**
     * Register a listener for the given path. If a client tries to get the path
     * and no listener is attachet to it, the connection will be aborted.
     * @param path The path for the connection (example ws://server:port/path)
     * @param listener the listener class to manage the connections.
     */
    public void registerListener(String path, WebSocketListener listener) {

        if (path == null) {
            throw new IllegalArgumentException("Path is null");
        }

        LinkedList<WebSocketListener> pathListeners = listeners.get(path);

        if (pathListeners == null) {
            pathListeners = new LinkedList<WebSocketListener>();
            listeners.put(path, pathListeners);
        }
        pathListeners.add(listener);
    }

    private WebSocket doHandShake(Socket s) throws IOException {

        Level outputLevel = Level.FINEST;

        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        WebSocketImpl ret = new WebSocketImpl(s, os, is, this);


        boolean isSecure = false;

        String line = readLine(is);
        log.log(outputLevel, "Header: {0}", line);

        if (line == null) {
            return null;
        }

        String[] parts = line.split(" ");
        if (parts.length >= 3) {
            ret.setContextPath(parts[1]);

            if (!listeners.containsKey(ret.getContextPath())) {
                log.log(Level.WARNING, "Path {0} has no listeners, "
                        + "aborting connection", ret.getContextPath());
                s.close();
                return null;
            }

        }

        //read the request
        while (!line.isEmpty()) {
            line = readLine(is);

            if (line == null) {
                continue;
            }

            if (line.isEmpty()) {
                continue;
            }

            log.log(outputLevel, "Header: {0}", line);
            parts = line.split(":", 2);
            if (parts.length != 2) {
                log.log(outputLevel, "Invalid Header: {0}", line);
                continue;
            }
            if (parts[0].toUpperCase().startsWith("SEC-WEBSOCKET-KEY")) {
                isSecure = true;
            }
            ret.putHeader(parts[0], parts[1]);
        }
        byte[] sum = null;
        if (isSecure) {
            byte[] key3 = new byte[8];
            is.read(key3);
            log.log(outputLevel, "Key 3: {0}", new String(key3));
            sum = generateStreamSum(ret.getHeader("Sec-WebSocket-Key1"),
                    ret.getHeader("Sec-WebSocket-Key2"), key3);
        }

        //answer the handshake
        StringBuilder sb = new StringBuilder();
        String lineSeparator = "\r\n";
        sb.append("HTTP/1.1 101 Web Socket Protocol Handshake").append(lineSeparator);
        sb.append("Upgrade: WebSocket").append(lineSeparator);
        sb.append("Connection: Upgrade").append(lineSeparator);
        if (isSecure) {
            sb.append("Sec-");
        }

        sb.append("WebSocket-Origin: ").append(ret.getHeader("Origin")).append(lineSeparator);


        if (isSecure) {
            sb.append("Sec-");
        }
        sb.append("WebSocket-Location: ws://").append(ret.getHeader("Host")).
                append(ret.getContextPath()).append(lineSeparator);

        if (ret.getHeader("Protocol") != null) {
            if (isSecure) {
                sb.append("Sec-");
            }
            sb.append("Protocol: ").append(ret.getHeader("Protocol")).append(lineSeparator);
        }
        sb.append(lineSeparator);

        String response = sb.toString();

        log.log(outputLevel, "RESPONSE: \n{0}", response);

        byte[] output = sb.toString().getBytes();

        os.write(output);

        if (isSecure) {
            String challenge = new String(sum);
            os.write(challenge.getBytes());
            log.log(outputLevel, "Secure Challenge: {0}", challenge);
        }
        os.flush();


        ret.startRecieving();

        return ret;
    }

    private void notifyListeners(WebSocket ws) {

        LinkedList<WebSocketListener> pathListeners = listeners.get(ws.getContextPath());

        if (pathListeners == null) {
            return;
        }

        for (WebSocketListener wsl : pathListeners) {
            wsl.clientConnected(ws);
        }
    }

    void notifyClosure(WebSocket ws) {

        LinkedList<WebSocketListener> pathListeners = listeners.get(ws.getContextPath());

        if (pathListeners == null) {
            return;
        }

        for (WebSocketListener wsl : pathListeners) {
            wsl.clientClosed(ws);
        }
    }

    static byte[] generateStreamSum(String key1, String key2, byte[] key3) {

        if (key1 == null || key2 == null || key3 == null) {
            return null;
        }

        long keyNo1 = generateKeyNumber(key1);
        long keyNo2 = generateKeyNumber(key2);

        int spaces1 = countSpaces(key1);
        int spaces2 = countSpaces(key2);

        //TODO - Verify that spaces multiple of keynos

        int part1 = (int) (keyNo1 / spaces1);
        int part2 = (int) (keyNo2 / spaces2);


        ByteBuffer challenge = ByteBuffer.allocate(16);
        challenge.putInt(part1);
        challenge.putInt(part2);
        challenge.put(key3);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.reset();
        } catch (NoSuchAlgorithmException ex) {
            log.log(Level.SEVERE, "MD5 Algorithm not found", ex);
        }

        return md.digest(challenge.array());
    }

    private static long generateKeyNumber(String key) {
        StringBuilder sb = new StringBuilder();

        for (char c : key.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return Long.parseLong(sb.toString());
    }

    private static int countSpaces(String key) {
        int ret = 0;

        for (char c : key.toCharArray()) {
            if (c == '\u0020') {
                ret++;
            }
        }
        return ret;
    }

    private String readLine(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();

        int cr = '\r';
        int nl = '\n';

        boolean gotcr = false;

        while (true) {
            int input = is.read();

            if (input == -1) {
                return null;
            }

            if (input == cr) {
                gotcr = true;
                continue;
            }
            if (input == nl && gotcr) {
                break;
            } else if (input == nl) {
                //we do this only because the protocol
                //tells ut that there must be a cr before the nl.
                return null;
            }
            sb.append((char) input);
        }
        return sb.toString();
    }

    private class ListenThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    Socket client = server.accept();
                    new ProtocolThread(client).start();
                } catch (SocketException ex) {
                    log.log(Level.INFO, "Server Closed Normally");
                    break;
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "Server Closed Abnormally");
                    if (!server.isClosed()) {
                        try {
                            server.close();
                        } catch (IOException sEx) {
                            log.log(Level.SEVERE, "Error Closing Server", sEx);
                        } finally {
                            server = null;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private class ProtocolThread extends Thread {

        private final Socket client;

        public ProtocolThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                WebSocket ws = doHandShake(client);

                if (ws != null) {
                    notifyListeners(ws);
                }
            } catch (SocketException ex) {
                log.log(Level.INFO, "Client closed connection");
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Server Closed Abnormally");

                if (!server.isClosed()) {
                    try {
                        server.close();
                    } catch (IOException sEx) {
                        log.log(Level.SEVERE, "Error Closing Server", sEx);
                    } finally {
                        server = null;
                    }
                }
            }
        }
    }
}
