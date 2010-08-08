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

/**
 * Represents a connection using the WebSocket protocol.
 * @author Juan Alberto López Cavallotti
 */
public interface WebSocket {
    /**
     * Read some of the headers recieved at the handshake stage.
     * @param key The case-insensitive key for the header.
     * @return the header or null if the header wasn't present.
     */
    public String getHeader(String key);
    /**
     * Closes the connection with the client. <br />
     * Note that the {@link WebSocketListener#clientClosed(WebSocket)}
     * event will be fired.
     */
    void close();
    /**
     * Send an UTF-8 encoded string to the client.
     * @param message should be an UTF-8 String as described in draft 76 of the
     * protocol specification.
     */
    void sendMessage(String message);
    /**
     * Add a listener that will be notified once an UTF-8 string is sent from
     * the client.
     * @param listener a class implementing {@link WebSocketMessageListener}.
     */
    void addMessageListener(WebSocketMessageListener listener);
    /**
     * Remove a listener from the list.
     * @param listener a class implementing {@link WebSocketMessageListener}.
     */
    void removeMessageListener(WebSocketMessageListener listener);
    /**
     * Gets the context path associated with this WebSocket, for example, if the
     * client connects to ws://some.host/path then the value returned will be
     * /path
     * @return the path associated with this web socket.
     */
    String getContextPath();
    /**
     * Checks if the connection for this WebSocket is closed.
     * @return true if closed, false if not.
     */
    boolean isClosed();
}
