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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Pingy {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static void log(PrintStream printStream, String msg) {
        printStream.printf("[%s] %s\n", TIME_FORMATTER.format(LocalDateTime.now()), msg);
    }

    public static void info(String msg) {
        log(System.out, msg);
    }

    public static void warn(String msg) {
        log(System.err, msg);
    }

    public static void main(String[] args) {
        final Path directory = Paths.get("");
        Path propsFile = new File("pingy.json").toPath();

        int index = 0;
        while (index < args.length) {
            final String arg = args[index++];
            switch (arg) {
                case "--config-path":
                case "--cp":
                    if (index >= args.length) {
                        throw new IllegalArgumentException("The parameter \"--config-path\" doesn't have a value.");
                    }
                    final String value = args[index++];
                    try {
                        propsFile = Paths.get(value);
                        // Try to parse the file to make sure it's valid
                        URL ignore = propsFile.toUri().toURL();
                        // May not be a directory
                        if (Files.isDirectory(propsFile)) {
                            throw new IllegalArgumentException("The config path is invalid: " + value + ", it may not be a directory.");
                        }
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("The config path is invalid: " + value);
                    }
                    info("Set config path to: " + value);
                    continue;
                // Any other properties?
                default:
                    warn("Unknown launch parameter: " + arg);
            }
        }

        final PingyProperties properties;
        final boolean newlyCreated;
        if (Files.exists(propsFile)) {
            try (BufferedReader reader = Files.newBufferedReader(propsFile)) {
                properties = new Gson().fromJson(reader, PingyProperties.class);
            } catch (IOException e) {
                throw new IllegalStateException("Invalid properties file, try to resolve the issue or regenerate the file", e);
            }

            newlyCreated = false;
            info("Loading the properties file...");
        } else {
            properties = new PingyProperties();
            newlyCreated = true;
        }

        final Path parent = propsFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (BufferedWriter writer = Files.newBufferedWriter(propsFile, StandardCharsets.UTF_8)) {
            gson.toJson(properties, writer);
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the properties file", e);
        }
        info(newlyCreated ? "Generating the properties file..." : "Updating the properties file...");

        try {
            properties.loadFavicon(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Pingy pingy = new Pingy(properties);
        try {
            pingy.start();
            info("Pingy is successfully started.");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start the server", e);
        }
    }

    private final PingyProperties properties;

    public Pingy(PingyProperties properties) {
        this.properties = properties;
    }

    /**
     * Gets the {@link InetSocketAddress} that should be
     * used for the specified ip and port.
     *
     * @param ip The ip
     * @param port The port
     * @return The socket address
     */
    private static InetSocketAddress getBindAddress(String ip, int port) {
        if (ip.length() == 0) {
            return new InetSocketAddress(port);
        } else {
            return new InetSocketAddress(ip, port);
        }
    }

    /**
     * Starts the pingy server.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        final boolean epoll = Epoll.isAvailable() && this.properties.isUseEpollWhenAvailable();

        final ServerBootstrap bootstrap = new ServerBootstrap();
        final EventLoopGroup group = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();

        final ChannelFuture future = bootstrap
                .group(group)
                .channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new ReadTimeoutHandler(20))
                                .addLast(new PingyLegacyHandler(properties))
                                .addLast(new PingyFramingHandler())
                                .addLast(new PingyHandler(properties));
                    }
                })
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .bind(getBindAddress(this.properties.getIp(), this.properties.getPort()));
        final Channel channel = future.awaitUninterruptibly().channel();
        if (!channel.isActive()) {
            final Throwable cause = future.cause();
            if (cause instanceof BindException) {
                throw (BindException) cause;
            }
            throw new RuntimeException("Failed to bind to address", cause);
        }
        info("Successfully bound to: " + channel.localAddress());
    }
}
