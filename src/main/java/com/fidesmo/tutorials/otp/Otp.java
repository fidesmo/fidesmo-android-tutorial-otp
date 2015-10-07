package com.fidesmo.tutorials.otp;


/*
 * OTP class
 * Cherry picked methods from https://github.com/Yubico/yubioath-android/blob/master/src/main/com/yubico/yubioath/model/YubiKeyNeo.java with the following licence:
 *
 * Copyright (c) 2013, Yubico AB.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

import java.io.IOException;

import nordpol.Apdu;

public class Otp {

    private static final byte[] CALCULATE_APDU = Apdu.decodeHex("00a2000100");
    static final String SEND_REMAINING_APDU = "00A5000000";

    static byte[] totpCodeApdu(String name, long timestamp) {
        final byte NAME_TAG = 0x71;
        final byte CHALLENGE_TAG = 0x74;

        byte[] nameBytes = name.getBytes();
        byte[] data = new byte[CALCULATE_APDU.length + 2 + nameBytes.length + 10];
        System.arraycopy(CALCULATE_APDU, 0, data, 0, CALCULATE_APDU.length);
        int offset = 4;
        data[offset++] = (byte) (data.length - 5);
        data[offset++] = NAME_TAG;
        data[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        offset += nameBytes.length;

        data[offset++] = CHALLENGE_TAG;
        data[offset++] = 8;
        data[offset++] = 0;
        data[offset++] = 0;
        data[offset++] = 0;
        data[offset++] = 0;
        data[offset++] = (byte) (timestamp >> 24);
        data[offset++] = (byte) (timestamp >> 16);
        data[offset++] = (byte) (timestamp >> 8);
        data[offset++] = (byte) timestamp;

        return data;
    }

    static String decipherTotpCode(byte[] data) throws IOException {
        final byte T_RESPONSE_TAG = 0x76;
        return codeFromTruncated(parseBlock(data, 0, T_RESPONSE_TAG));
    }

    static String codeFromTruncated(byte[] data) {
        final int[] MOD = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
        int num_digits = data[0];
        int code = (data[1] << 24) | ((data[2] & 0xff) << 16) | ((data[3] & 0xff) << 8) | (data[4] & 0xff);
        return String.format("%0" + num_digits + "d", code % MOD[num_digits]);
    }

    static byte[] parseBlock(byte[] data, int offset, byte identifier) throws IOException {
        if (data[offset] == identifier) {
            int length = data[offset + 1];
            byte[] block = new byte[length];
            System.arraycopy(data, offset + 2, block, 0, length);
            return block;
        } else {
            throw new IOException("Require block type: " + identifier + ", got: " + data[offset]);
        }
    }
}
