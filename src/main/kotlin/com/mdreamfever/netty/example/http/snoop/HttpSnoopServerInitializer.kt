package com.mdreamfever.netty.example.http.snoop

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContext

class HttpSnoopServerInitializer(private val sslContext: SslContext?) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            sslContext?.let {
                addLast(it.newHandler(ch.alloc()))
            }
            addLast(HttpServerCodec())
            addLast(HttpSnoopServerHandler())
        }
    }
}