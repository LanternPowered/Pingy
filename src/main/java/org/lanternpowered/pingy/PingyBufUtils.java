/*
 * This file is part of Pingy, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://github.com/LanternPowered>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the Software), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, andor sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.pingy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public final class PingyBufUtils {

    public static boolean readableVarInt(ByteBuf buf) {
        if (buf.readableBytes() > 5) {
            return true;
        }

        int idx = buf.readerIndex();
        byte in;
        do {
            if (buf.readableBytes() < 1) {
                buf.readerIndex(idx);
                return false;
            }
            in = buf.readByte();
        } while ((in & 0x80) != 0);

        buf.readerIndex(idx);
        return true;
    }

    public static void writeByteArray(ByteBuf byteBuf, byte[] data) {
        writeVarInt(byteBuf, data.length);
        byteBuf.writeBytes(data);
    }

    public static void writeVarInt(ByteBuf byteBuf, int value) {
        while ((value & 0xFFFFFF80) != 0L) {
            byteBuf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        byteBuf.writeByte(value & 0x7F);
    }

    public static int readVarInt(ByteBuf byteBuf) {
        int value = 0;
        int i = 0;
        int b;
        while (((b = byteBuf.readByte()) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            if (i > 35) {
                throw new DecoderException("Variable length is too long!");
            }
        }
        return value | (b << i);
    }

    public static byte[] readByteArray(ByteBuf byteBuf, int maxLength) {
        int length = readVarInt(byteBuf);
        if (length < 0) {
            throw new DecoderException("Byte array length may not be negative.");
        }
        if (length > maxLength) {
            throw new DecoderException("Exceeded the maximum allowed length, got " + length + " which is greater then " + maxLength);
        }
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    private PingyBufUtils() {
    }
}
