package com.mdreamfever.netty.example.file

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.util.CharsetUtil

fun main() {
    val server = FileServer()
    server.bind()
}

class FileServer(ssl: Boolean = false, private val port: Int = 8080) {
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
                        sslContext?.let {
                            addLast(it.newHandler(ch.alloc()))
                        }
                        addLast(StringDecoder(CharsetUtil.UTF_8))
                        addLast(LineBasedFrameDecoder(8192))
                        addLast(StringEncoder(CharsetUtil.UTF_8))
                        addLast(ChunkedWriteHandler())
                        addLast(FileServerHandler())
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