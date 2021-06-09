package com.mdreamfever.netty.example.http.snoop

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

fun main() {
    HttpSnoopServer().run {
        bind()
    }
}

class HttpSnoopServer {
    private val ssl = true
    private val port = if (ssl) 8080 else 443
    private val serverBootstrap = ServerBootstrap()
    private val bossGroup = NioEventLoopGroup()
    private val workGroup = NioEventLoopGroup()

    init {
        val sslContext = if (ssl) {
            val selfSignedCertificate = SelfSignedCertificate()
            SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build()
        } else null
        serverBootstrap.group(bossGroup, workGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(HttpSnoopServerInitializer(sslContext))
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