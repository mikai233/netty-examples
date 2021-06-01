package com.mdreamfever.netty.example.http.file

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContext
import io.netty.handler.stream.ChunkedWriteHandler

class HttpStaticFileServerInitializer(private val sslContext: SslContext?) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            sslContext?.let {
                addLast(sslContext.newHandler(ch.alloc()))
            }
            addLast(HttpServerCodec())
            addLast(HttpObjectAggregator(65536))
            addLast(ChunkedWriteHandler())
            addLast(HttpStaticFileServerHandler())
        }
    }
}