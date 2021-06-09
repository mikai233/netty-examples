package com.mdreamfever.netty.example.http.snoop

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpContentDecompressor
import io.netty.handler.ssl.SslContext

class HttpSnoopClientInitializer(private val sslContext: SslContext?) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            sslContext?.let {
                addLast(sslContext.newHandler(ch.alloc()))
            }
            addLast(HttpClientCodec())
            addLast(HttpContentDecompressor())
            addLast(HttpSnoopClientHandler())
        }
    }
}