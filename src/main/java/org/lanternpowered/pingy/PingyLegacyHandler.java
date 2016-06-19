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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

public class PingyLegacyHandler extends ChannelInboundHandlerAdapter {

    private final PingyProperties properties;

    public PingyLegacyHandler(PingyProperties properties) {
        this.properties = properties;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg0) throws Exception {
        final ByteBuf msg = (ByteBuf) msg0;

        boolean legacy = false;
        msg.markReaderIndex();
        try {
            // Try first as a legacy ping message
            int messageId = msg.readUnsignedByte();
            // Make sure that old clients don't attempt to login
            if (messageId == 0x02) {
                legacy = this.tryHandleLegacyJoin(ctx, msg);
            } else if (messageId == 0xfe) {
                legacy = this.tryHandleLegacyPing(ctx, msg);
            }
        } catch (Exception e) {
        }
        if (!legacy) {
            msg.resetReaderIndex();
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        } else {
            msg.release();
        }
    }

    private boolean tryHandleLegacyPing(ChannelHandlerContext ctx, ByteBuf msg) {
        int readable = msg.readableBytes();

        // Full message, contains more info
        boolean full = false;

        if (readable > 0) {
            // Is always 1
            if (msg.readUnsignedByte() != 1) {
                return false;
            }
            full = true;
        }

        if (readable > 1) {
            if (msg.readUnsignedByte() != 0xfa) {
                return false;
            }
            byte[] bytes = new byte[msg.readShort() << 1];
            msg.readBytes(bytes);
            if (!new String(bytes, StandardCharsets.UTF_16BE).equals("MC|PingHost")){
                return false;
            }
        }

        final String motd = getFirstLine(this.properties.getLegacyMessageOfTheDay());
        if (full) {
            sendLegacyDisconnectMessage(ctx, String.format("\u00A7%s\0%s\0%s\0%s\0%s\0%s",
                    1, 127, this.properties.getOutdatedMessage(), motd, -1, -1));
        } else {
            sendLegacyDisconnectMessage(ctx, String.format("%s\u00A7%s\u00A7%s",
                    motd, -1, -1));
        }

        return true;
    }

    private boolean tryHandleLegacyJoin(ChannelHandlerContext ctx, ByteBuf msg) {
        msg.readByte(); // Protocol version
        int value = msg.readShort();
        // Check the length
        if (value < 0 || value > 16) {
            return false;
        }
        msg.readBytes(value << 1); // Username
        value = msg.readShort();
        // Check the length
        if (value < 0 || value > 255) {
            return false;
        }
        msg.readBytes(value << 1); // Host address
        msg.readInt(); // Port
        if (msg.readableBytes() > 0) {
            return false;
        }
        sendLegacyDisconnectMessage(ctx, this.properties.getLegacyDisconnectMessage());
        return true;
    }

    /**
     * Sends a disconnect message to a legacy client and closes the connection.
     *
     * @param ctx The channel handler context
     * @param message The message
     */
    private static void sendLegacyDisconnectMessage(ChannelHandlerContext ctx, String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_16BE);

        ByteBuf output = ctx.alloc().buffer();
        output.writeByte(0xff);
        output.writeShort(data.length >> 1);
        output.writeBytes(data);

        ctx.pipeline().firstContext().writeAndFlush(output).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Gets the first line of the string.
     *
     * @param value The string
     * @return The first line
     */
    private static String getFirstLine(String value) {
        int i = value.indexOf('\n');
        return i == -1 ? value : value.substring(0, i);
    }
}
