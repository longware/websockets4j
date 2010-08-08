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
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete implementation for the webSocket handler.
 * @author Juan Alberto López Cavallotti
 */
final class WebSocketImpl implements WebSocket {

    private final Socket client;
    private final OutputStream writer;
    private final InputStream reader;
    private final HashMap<String, String> headers;
    private final WebSocketServer createdIn;
    private String contextPath;
    private LinkedList<WebSocketMessageListener> listeners = new LinkedList<WebSocketMessageListener>();
    private LinkedList<WebSocketMessageListener> removed = new LinkedList<WebSocketMessageListener>();
    private static final Logger log = Logger.getLogger(WebSocketImpl.class.getName());
    private RecieverThread reciever;

    public WebSocketImpl(Socket client, OutputStream writer, InputStream reader, WebSocketServer createdIn) {
        this.client = client;
        this.writer = writer;
        this.reader = reader;
        this.headers = new HashMap<String, String>();
        this.createdIn = createdIn;
        reciever = new RecieverThread();
    }

    public void putHeader(String key, String value) {
        headers.put(key.toUpperCase(), value.trim());
    }

    @Override
    public String getHeader(String key) {
        return headers.get(key.toUpperCase());
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Exception trying to close socket", ex);
        }
    }

    @Override
    public void sendMessage(String message) {
        try {
            writer.write(0x00);
            writer.write(message.getBytes());
            writer.write(0xFF);
        } catch (IOException ex) {
            log.log(Level.WARNING, null, ex);
        }
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    void startRecieving() {
        reciever.start();
    }

    @Override
    public void addMessageListener(WebSocketMessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeMessageListener(WebSocketMessageListener listener) {
        removed.add(listener);
    }

    @Override
    public boolean isClosed() {
        return client.isClosed();
    }

    private class RecieverThread extends Thread {

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        StringBuilder builder = new StringBuilder();
                        int character = reader.read();
                        if (character == -1) {
                            log.log(Level.INFO, "Socket closed by client");
                            break;
                        }

                        if (character == 0xFFFC) {
                            log.log(Level.WARNING, "Client Sent Error");
                            break;
                        }

                        if (character != 0x00) {
                            continue;
                        }

                        while (character != 0xFF) {
                            character = reader.read();

                            if (character == -1) {
                                log.log(Level.INFO, "Socket closed by client");
                                break;
                            }

                            if (character == 0xFFFC) {
                                throw new IOException("Client sent error");
                            }
                            if (character == 0xFF) {
                                continue;
                            }
                            builder.append((char) character);
                        }
                        notifyListeners(builder.toString());
                    } catch (SocketException ex) {
                        log.log(Level.INFO, "Socket closed");
                        break;
                    } catch (IOException ex) {
                        log.log(Level.SEVERE, "Error While trying to read", ex);
                        break;
                    }
                }
            } finally {
                if (!client.isClosed()) {
                    try {
                        client.close();
                    } catch (IOException ex) {
                        log.log(Level.SEVERE, "Could not close socket", ex);
                    }
                }
                createdIn.notifyClosure(WebSocketImpl.this);
            }
        }

        private void notifyListeners(String message) {
            listeners.removeAll(removed);
            removed.clear();
            for (WebSocketMessageListener listener : listeners) {
                listener.onMessage(message);
            }
        }
    }
}
