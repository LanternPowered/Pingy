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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.imageio.ImageIO;

public class PingyProperties {

    /**
     * The ip address to bind the server to.
     */
    @Expose @SerializedName("ip")
    private String ip = "";

    /**
     * The port to use for the server.
     */
    @Expose @SerializedName("port")
    private int port = 25565;

    /**
     * Whether the server should use epoll if
     * it's available.
     */
    @Expose @SerializedName("use-epoll-when-available")
    private boolean useEpollWhenAvailable = true;

    /**
     * The message of the day. Supports minecraft chat format.
     */
    @Expose @SerializedName("message-of-the-day")
    private JsonElement messageOfTheDay = new JsonPrimitive("Pingy was here...");

    /**
     * The message of the day for legacy clients. Only supports plain text.
     */
    @Expose @SerializedName("legacy-message-of-the-day")
    private String legacyMessageOfTheDay = "Pingy was here... This client is old...";

    /**
     * The message that is displayed in the upper right corner when
     * the server/client is outdated.
     */
    @Expose @SerializedName("outdated-message")
    private String outdatedMessage = "Move along...";

    /**
     * The message that is displayed when hovering
     * over the outdated message.
     *
     * Null character means ignore.
     */
    @Expose @SerializedName("outdated-message-tooltip")
    private String outdatedMessageTooltip = "";

    /**
     * The message that is used as kick/disconnect reason
     * of the server. Supports minecraft chat format.
     */
    @Expose @SerializedName("disconnect-message")
    private JsonElement disconnectMessage = new JsonPrimitive("Why are you even trying...");

    /**
     * The message that is used as kick/disconnect reason
     * of the server for legacy clients. Only supports plain text.
     */
    @Expose @SerializedName("legacy-disconnect-message")
    private String legacyDisconnectMessage = "Why are you even trying...";

    /**
     * The path of the favicon file, may be empty to disable
     * the favicon.
     */
    @Expose @SerializedName("favicon")
    private String favicon = "";

    /**
     * The server type, this will affect the icon in the
     * forge client. Supported values are {@code vanilla},
     * {@code bukkit} and {@code fml} (or {@code forge}).
     */
    @Expose @SerializedName("server-type")
    private String serverType = "vanilla";

    /**
     * The mod list tag used by the forge client, all these mods
     * will be visible when hovering over the server type icon.
     * This will only work if the {@link #serverType} is {@code fml}
     * (or {@code forge}).
     */
    @Expose @SerializedName("mod-list")
    private String[] modList = new String[0];

    private String faviconData;

    public void loadFavicon(Path directory) throws IOException {
        if (this.favicon.isEmpty()) {
            return;
        }
        final Path faviconPath = directory.resolve(this.favicon);
        if (!Files.exists(faviconPath)) {
            throw new IOException("Favicon file does not exist.");
        }
        final BufferedImage image;
        try {
            image = ImageIO.read(faviconPath.toFile());
        } catch (IOException e) {
            throw new IOException("Unable to read the favicon file.");
        }
        if (image.getWidth() != 64) {
            throw new IOException("Favicon must be 64 pixels wide.");
        }
        if (image.getHeight() != 64) {
            throw new IOException("Favicon must be 64 pixels high.");
        }
        final ByteBuf buf = Unpooled.buffer();
        try {
            ImageIO.write(image, "PNG", new ByteBufOutputStream(buf));
            final ByteBuf base64 = Base64.encode(buf, false);

            try {
                this.faviconData = "data:image/png;base64," + base64.toString(StandardCharsets.UTF_8);
            } finally {
                base64.release();
            }
        } finally {
            buf.release();
        }
    }

    public String getServerType() {
        return this.serverType;
    }

    public int getPort() {
        return this.port;
    }

    public JsonElement getMessageOfTheDay() {
        return this.messageOfTheDay;
    }

    public String getOutdatedMessage() {
        return this.outdatedMessage;
    }

    public String getIp() {
        return this.ip;
    }

    public String getLegacyMessageOfTheDay() {
        return this.legacyMessageOfTheDay;
    }

    public JsonElement getDisconnectMessage() {
        return this.disconnectMessage;
    }

    public String getLegacyDisconnectMessage() {
        return this.legacyDisconnectMessage;
    }

    public Optional<String> getFaviconData() {
        return Optional.ofNullable(this.faviconData);
    }

    public Optional<String> getOutdatedMessageTooltip() {
        return this.outdatedMessageTooltip.isEmpty() ? Optional.empty() : Optional.of(this.outdatedMessageTooltip);
    }

    public boolean isUseEpollWhenAvailable() {
        return this.useEpollWhenAvailable;
    }

    public String[] getModList() {
        return this.modList;
    }
}
