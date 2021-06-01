package com.mdreamfever.netty.example.discard

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() = runBlocking {
    withContext(Dispatchers.IO) {
        DiscardClient().connect()
    }
}

class DiscardClient(
    private val ssl: Boolean = System.getProperty("ssl") != null,
    private val host: String = System.getProperty("host", "127.0.0.1"),
    private val port: Int = System.getProperty("port", "8009").toInt(),
    val size: Int = System.getProperty("size", "256").toInt(),
) {
    private val bootstraps: Bootstrap = Bootstrap()
    private val group: NioEventLoopGroup = NioEventLoopGroup()

    init {
        val sslContext =
            if (ssl) SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build() else null
        bootstraps.group(group).channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    sslContext?.let {
                        pipeline.addLast(it.newHandler(ch.alloc(), host, port))
                    }
                    pipeline.addLast(DiscardClientHandler())
                }
            })
    }

    fun connect() {
        try {
            val future = bootstraps.connect(host, port).sync()
            future.channel().closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}



