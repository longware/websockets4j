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
 * Interface for events related to connections. The implementing class should be
 * registered as a listener using an instance of {@link WebSocketServer}. <br />
 * The implementing class should take care of the connection once the handshake
 * was successfully made ({@link #clientConnected(WebSocket socket)} and should
 * forget all the references of the connection when the connection is closed
 * ({@link #clientClosed(WebSocket socket)}). <br />
 * Note that the clientClosed event is not waranteed to fire immediately. This
 * event will fire when the user tries to make some interaction and the
 * connection is dropped.
 *
 * @author Juan Alberto López Cavallotti
 */
public interface WebSocketListener {
    /**
     * Event triggered after a successful handshake is made with some client.
     * @param socket the resulting socket.
     */
    void clientConnected(WebSocket socket);
    /**
     * Event triggered after some end has closed the connection and some
     * interation is done.
     * @param socket the socket that has been closed.
     */
    void clientClosed(WebSocket socket);
}
