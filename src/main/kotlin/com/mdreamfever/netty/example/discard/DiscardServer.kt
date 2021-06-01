package com.mdreamfever.netty.example.discard

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

fun main() {
    DiscardServer().bind()
}

class DiscardServer(
    private val ssl: Boolean = System.getProperty("ssl") != null,
    private val port: Int = System.getProperty("port", "8009").toInt()
) {
    private val bossGroup: NioEventLoopGroup = NioEventLoopGroup()
    private val workGroup: NioEventLoopGroup = NioEventLoopGroup()
    private val bootstrap: ServerBootstrap = ServerBootstrap()

    init {
        val sslContext = if (ssl) {
            val selfSignedCertificate = SelfSignedCertificate()
            SslContextBuilder.forServer(
                selfSignedCertificate.certificate(),
                selfSignedCertificate.privateKey()
            ).build()
        } else null
        bootstrap.group(bossGroup, workGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    sslContext?.let {
                        pipeline.addLast(sslContext.newHandler(ch.alloc()))
                    }
                    pipeline.addLast(DiscardServerHandler())
                }
            })
    }

    fun bind() {
        try {
            val future = bootstrap.bind(port).sync()
            future.channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workGroup.shutdownGracefully()
        }
    }
}