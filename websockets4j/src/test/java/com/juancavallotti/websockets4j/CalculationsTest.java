/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juancavallotti.websockets4j;

import java.nio.charset.Charset;
import java.io.UnsupportedEncodingException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the calculations of the algorithms based on the documentation of the protocol.
 * @author juancavallotti
 */
public class CalculationsTest {

    @Test
    public void testChallenge() throws UnsupportedEncodingException {
        String key1 = "3e6b263  4 17 80";
        String key2 = "17  9 G`ZD9   2 2b 7X 3 /r90";
        byte[] key3 = {0x57, 0x6A, 0x4E, 0x7D, 0x7C, 0x4D, 0x28, 0x36};

        byte[] ret = WebSocketServer.generateStreamSum(key1, key2, key3);

        String result = new String(ret);
        String expected = "n`9eBk9z$R8pOtVb";

        byte[] expBytes = {0x6E,
            0x60, 0x39, 0x65, 0x42, 0x6B, 0x39, 0x7A, 0x24, 0x52, 0x38, 0x70, 0x4F, 0x74,
            0x56, 0x62};

        assertArrayEquals(expBytes, ret);
        assertEquals(expected, result);
    }
}
