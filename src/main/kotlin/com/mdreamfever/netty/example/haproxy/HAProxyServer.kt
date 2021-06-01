package com.mdreamfever.netty.example.haproxy

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

fun main() {
    HAProxyServer().run {
        bind()
    }
}

class HAProxyServer(private val port: Int = 8080) {
    private val serverBootstrap = ServerBootstrap()
    private val bossGroup = NioEventLoopGroup()
    private val workGroup = NioEventLoopGroup()

    init {
        serverBootstrap
            .group(bossGroup, workGroup)
            .handler(LoggingHandler(LogLevel.INFO))
            .channel(NioServerSocketChannel::class.java)
            .childHandler(ServerInitializer())
    }

    fun bind() {
        try {
            val future = serverBootstrap.bind(port).sync()
            future.channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workGroup.shutdownGracefully()
        }
    }
}

class ServerInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            addLast(LoggingHandler(LogLevel.DEBUG))
            addLast(HAProxyMessageDecoder())
            addLast(object : SimpleChannelInboundHandler<Any>() {
                override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
                    when (msg) {
                        is HAProxyMessage -> {
                            println("proxy message: $msg")
                        }
                        is ByteBuf -> {
                            println("bytebuf message: ${ByteBufUtil.prettyHexDump(msg)}")
                        }
                    }
                }
            })
        }
    }
}