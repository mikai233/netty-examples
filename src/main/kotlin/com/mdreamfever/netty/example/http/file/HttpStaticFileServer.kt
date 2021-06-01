package com.mdreamfever.netty.example.http.file

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

fun main() {
    HttpStaticFileServer().run {
        bind()
    }
}

class HttpStaticFileServer(private val ssl: Boolean = false, private val port: Int = if (ssl) 8443 else 8080) {

    private val serverBootstrap = ServerBootstrap()
    private val bossGroup = NioEventLoopGroup()
    private val workGroup = NioEventLoopGroup()

    init {
        val sslContext = if (ssl) {
            val selfSignedCertificate = SelfSignedCertificate()
            SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build()
        } else null
        serverBootstrap
            .group(bossGroup, workGroup)
            .option(ChannelOption.SO_BACKLOG, 100)
            .handler(LoggingHandler(LogLevel.INFO))
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        addLast(LoggingHandler(LogLevel.INFO))
                        addLast(HttpStaticFileServerInitializer(sslContext))
                    }
                }
            })
    }

    fun bind() {
        try {
            val channel = serverBootstrap.bind(port).sync().channel()
            println("Open your web browser and navigate to ${if (ssl) "https" else "http"}://127.0.0.1:$port/")
            channel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workGroup.shutdownGracefully()
        }
    }
}