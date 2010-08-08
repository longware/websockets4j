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
 * Interface for the Message event. This event listener should be regirtered into
 * a {@link WebSocket} instance. The onMessage event is fired when a message from
 * the client is recieved.
 *
 * @author  Juan Alberto López Cavallotti
 */
public interface WebSocketMessageListener {
    /**
     * The implementing class should take care of this new arrived message.
     * @param message is the message recieved by the client.
     */
    void onMessage(String message);
}
