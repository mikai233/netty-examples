package com.mdreamfever.netty.example.http.helloworld

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerExpectContinueHandler
import io.netty.handler.ssl.SslContext

class HelloWorldServerInitializer(val sslContext: SslContext?) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            sslContext?.let {
                addLast(sslContext.newHandler(ch.alloc()))
            }
            addLast(HttpServerCodec())
            addLast(HttpServerExpectContinueHandler())
            addLast(HelloWorldServerHandler())
        }
    }
}