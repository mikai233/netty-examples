package com.mdreamfever.netty.example.echo

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.net.InetSocketAddress

fun main() {
    val client = EchoClient()
    client.connect()
}

class EchoClient(private val inetAddress: InetSocketAddress = InetSocketAddress("127.0.0.1", 8089)) {
    private val bootstrap = Bootstrap()
    private val group = NioEventLoopGroup()

    init {
        bootstrap
            .group(group)
            .remoteAddress(inetAddress)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().run {
                        addLast(StringDecoder())
                        addLast(StringEncoder())
                        addLast(EchoClientHandler())
                    }
                }
            })
    }

    fun connect() {
        try {
            val channel = bootstrap.connect().sync().channel()
            channel.writeAndFlush("hello world")
            channel.closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}