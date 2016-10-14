/*
 * This file is part of Pingy, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
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

import static org.lanternpowered.pingy.PingyBufUtils.readByteArray;
import static org.lanternpowered.pingy.PingyBufUtils.readVarInt;
import static org.lanternpowered.pingy.PingyBufUtils.writeByteArray;
import static org.lanternpowered.pingy.PingyBufUtils.writeVarInt;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class PingyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final static Gson GSON = new Gson();
    private final PingyProperties properties;

    private ProtocolState state = ProtocolState.HANDSHAKE;
    private int protocolVersion = -1;

    private enum ProtocolState {
        HANDSHAKE,
        PLAY,
        STATUS,
        LOGIN,
        ;

        private final static ProtocolState[] values = values();

        public static ProtocolState fromId(int id) {
            return ++id < 0 || id >= values.length ? null : values[id];
        }
    }

    public PingyHandler(PingyProperties properties) {
        this.properties = properties;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Pingy.info(ctx.channel().remoteAddress() + " connected to the server.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Pingy.info(ctx.channel().remoteAddress() + " disconnected from the server.");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (!ctx.channel().isActive()) {
            return;
        }
        final int messageId = readVarInt(msg);
        if (this.state == ProtocolState.HANDSHAKE) {
            switch (messageId) {
                case 0x00:
                    this.handleHandshake(ctx, msg);
                    break;
                default:
                    ctx.channel().close();
                    throw new DecoderException("Unknown handshake message type: " + messageId);
            }
        } else if (this.state == ProtocolState.STATUS) {
            switch (messageId) {
                case 0x00:
                    this.handleStatusRequest(ctx, msg);
                    break;
                case 0x01:
                    this.handleStatusPing(ctx, msg);
                    break;
                default:
                    ctx.channel().close();
                    throw new DecoderException("Unknown handshake message type: " + messageId);
            }
        }
    }

    private void handleHandshake(ChannelHandlerContext ctx, ByteBuf msg) {
        if (this.state != ProtocolState.HANDSHAKE) {
            ctx.channel().close();
            throw new DecoderException("Received unexpected handshake message");
        }
        this.protocolVersion = readVarInt(msg); // Protocol version
        readByteArray(msg, 255 * 4); // Hostname
        msg.readShort(); // Port

        final ProtocolState state = ProtocolState.fromId(readVarInt(msg)); // Protocol state
        switch (state) {
            case HANDSHAKE:
            case PLAY:
                ctx.channel().close();
                throw new DecoderException("Received unexpected handshake message");
            case LOGIN:
                sendMessage(ctx, 0x00, buf -> writeByteArray(buf, GSON.toJson(fixJson(this.properties.getDisconnectMessage()))
                        .getBytes(StandardCharsets.UTF_8))).addListener(ChannelFutureListener.CLOSE);
                return;
            case STATUS:
                this.state = state;
                return;
            default:
                throw new IllegalStateException("Unsupported protocol state: " + state);
        }
    }

    private void handleStatusPing(ChannelHandlerContext ctx, ByteBuf msg) {
        sendMessage(ctx, 0x01, buf -> buf.writeLong(msg.readLong()));
    }

    private void handleStatusRequest(ChannelHandlerContext ctx, ByteBuf msg) {
        final JsonObject rootObject = new JsonObject();
        final JsonObject versionObject = new JsonObject();
        versionObject.addProperty("name", this.properties.getOutdatedMessage());
        versionObject.addProperty("protocol", -1);

        final Optional<String> optTooltip = this.properties.getOutdatedMessageTooltip();
        if (optTooltip.isPresent()) {
            final JsonObject playersObject = new JsonObject();
            playersObject.addProperty("max", -1);
            playersObject.addProperty("online", -1);
            final JsonArray array = new JsonArray();
            for (String name : optTooltip.get().split("\n")) {
                final JsonObject playerEntry = new JsonObject();
                playerEntry.addProperty("name", name);
                playerEntry.addProperty("id", UUID.randomUUID().toString());
                array.add(playerEntry);
            }
            playersObject.add("sample", array);
            rootObject.add("players", playersObject);
        }

        rootObject.add("version", versionObject);
        rootObject.add("description", this.properties.getMessageOfTheDay());
        this.properties.getFaviconData().ifPresent(data -> rootObject.addProperty("favicon", data));

        sendMessage(ctx, 0x00, buf -> writeByteArray(buf, GSON.toJson(rootObject).getBytes(StandardCharsets.UTF_8)));
    }

    private static ChannelFuture sendMessage(ChannelHandlerContext ctx, int messageId, Consumer<ByteBuf> bufConsumer) {
        final ByteBuf buf = ctx.alloc().buffer();
        writeVarInt(buf, messageId);
        bufConsumer.accept(buf);
        return ctx.writeAndFlush(buf);
    }

    /**
     * The client doesn't like it when the server just sends a
     * primitive json string, so we put it as one entry in an array
     * to avoid errors.
     *
     * @param element The json element
     * @return The result json element
     */
    private static JsonElement fixJson(JsonElement element) {
        if (element instanceof JsonPrimitive) {
            final JsonArray array = new JsonArray();
            array.add(element);
            return array;
        }
        return element;
    }
}
