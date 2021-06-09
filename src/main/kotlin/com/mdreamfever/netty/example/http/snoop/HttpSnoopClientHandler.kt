package com.mdreamfever.netty.example.http.snoop

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil

class HttpSnoopClientHandler : SimpleChannelInboundHandler<HttpObject>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
        when (msg) {
            is HttpResponse -> {
                println("STATUS: ${msg.status()}")
                println("VERSION: ${msg.protocolVersion()}")
                println()

                msg.headers().takeUnless { it.isEmpty }?.let { httpHeaders ->
                    httpHeaders.names().forEach { name ->
                        httpHeaders.getAll(name).forEach {
                            println("HEADER: $name=$it")
                        }
                    }
                    println()
                }

                if (HttpUtil.isTransferEncodingChunked(msg)) {
                    println("CHUNKED CONTENT {")
                } else {
                    println("CONTENT {")
                }
            }
            is LastHttpContent -> {
                println("} END OF CONTENT")
                ctx.close()
            }
            is HttpContent -> {
                println(msg.content().toString(CharsetUtil.UTF_8))
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}