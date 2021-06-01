package com.mdreamfever.netty.example.http.cors

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.cors.CorsConfigBuilder
import io.netty.handler.codec.http.cors.CorsHandler
import io.netty.handler.ssl.SslContext

class HttpCorsServerInitializer(private val sslContext: SslContext? = null) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val corsConfig = CorsConfigBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .build()
        ch.pipeline().apply {
            sslContext?.let {
                addLast(sslContext.newHandler(ch.alloc()))
            }
            addLast(HttpServerCodec())
            addLast(HttpObjectAggregator(65536))
            addLast(CorsHandler(corsConfig))
            addLast(OKResponseHandler())
        }
    }
}