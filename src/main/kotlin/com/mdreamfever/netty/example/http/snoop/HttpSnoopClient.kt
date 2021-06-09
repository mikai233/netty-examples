package com.mdreamfever.netty.example.http.snoop

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.cookie.ClientCookieEncoder
import io.netty.handler.codec.http.cookie.DefaultCookie
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.net.URI

fun main() {
    HttpSnoopClient().run {
        connect()
    }
}

class HttpSnoopClient {
    private val url = "https://127.0.0.1:8080"
    private val uri = URI(url)
    private val scheme = uri.scheme ?: "http"
    private val host = uri.host ?: "127.0.0.1"
    private val port = uri.port
    private val bootstrap = Bootstrap()
    private val group = NioEventLoopGroup()

    init {
        if (!scheme.equals("http", true) && !scheme.equals("https", true)) {
            throw Exception("Only HTTP(S) is supported.")
        }
        val sslContext = if (scheme.equals("https", true)) {
            SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        } else null
        bootstrap.group(group)
            .channel(NioSocketChannel::class.java)
            .remoteAddress(host, port)
            .handler(HttpSnoopClientInitializer(sslContext))
    }

    fun connect() {
        try {
            val channel = bootstrap.connect().sync().channel()
            val request =
                DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.rawPath, Unpooled.EMPTY_BUFFER).also {
                    it.headers()[HttpHeaderNames.HOST] = host
                    it.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
                    it.headers()[HttpHeaderNames.ACCEPT_ENCODING] = HttpHeaderValues.GZIP
                    it.headers()[HttpHeaderNames.COOKIE] =
                        ClientCookieEncoder.STRICT.encode(
                            DefaultCookie("my-cookie", "foo"),
                            DefaultCookie("another-cookie", "bar")
                        )
                }
            channel.writeAndFlush(request)
            channel.closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}