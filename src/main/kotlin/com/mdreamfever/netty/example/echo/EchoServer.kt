package com.mdreamfever.netty.example.echo

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

fun main() {
    val server = EchoServer()
    server.bind()
}

class EchoServer(private val port: Int = 8089) {
    private val bossGroup = NioEventLoopGroup()
    private val workGroup = NioEventLoopGroup()
    private val serverBootstrap = ServerBootstrap()

    init {
        serverBootstrap
            .group(bossGroup, workGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 100)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        addLast(StringDecoder())
                        addLast(StringEncoder())
                        addLast(EchoServerHandler())
                    }
                }
            })
    }

    fun bind() {
        try {
            val channel = serverBootstrap.bind(port).sync().channel()
            channel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workGroup.shutdownGracefully()
        }
    }
}