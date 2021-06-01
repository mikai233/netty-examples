package com.mdreamfever.netty.example.http.cors

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

fun main() {
    Client().run {
        connect()
    }
}

class Client(
    private val ssl: Boolean = true,
    private val host: String = "127.0.0.1",
    private val port: Int = if (ssl) 8443 else 8080
) {
    private val bootstrap = Bootstrap()
    private val group = NioEventLoopGroup()

    init {
        val sslContext =
            if (ssl) SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
            else null
        bootstrap
            .group(group)
            .remoteAddress(host, port)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        sslContext?.let {
                            addLast(sslContext.newHandler(ch.alloc(), host, port))
                        }
                        addLast(HttpClientCodec())
                        addLast(HttpObjectAggregator(65536))
                        addLast(object : SimpleChannelInboundHandler<FullHttpResponse>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse) {
                                println(msg)
                            }
                        })

                    }
                }
            })
    }

    fun connect() {
        try {
            val channel = bootstrap.connect().sync().channel()
            channel.writeAndFlush(DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"))
            channel.closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}