package com.mdreamfever.netty.example.haproxy

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.haproxy.*
import io.netty.util.CharsetUtil

fun main() {
    HAProxyClient().run {
        connect()
    }
}

class HAProxyClient(private val host: String = "127.0.0.1", private val port: Int = 8080) {
    private val bootstrap = Bootstrap()
    private val workGroup = NioEventLoopGroup()

    init {
        bootstrap
            .group(workGroup)
            .remoteAddress(host, port)
            .channel(NioSocketChannel::class.java)
            .handler(ClientInitializer())
    }

    fun connect() {
        try {
            val channel = bootstrap.connect().sync().channel()
            val message = HAProxyMessage(
                HAProxyProtocolVersion.V2,
                HAProxyCommand.PROXY,
                HAProxyProxiedProtocol.TCP4,
                "127.0.0.1",
                "127.0.0.2",
                8000,
                9000
            )
            channel.apply {
                writeAndFlush(message).sync()
                writeAndFlush(Unpooled.copiedBuffer("Hello world!", CharsetUtil.US_ASCII)).sync()
                writeAndFlush(Unpooled.copiedBuffer("Bye now!", CharsetUtil.US_ASCII)).sync()
                close().sync()
            }
        } finally {
            workGroup.shutdownGracefully()
        }
    }
}

class ClientInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            addLast(HAProxyMessageEncoder.INSTANCE)
        }
    }
}