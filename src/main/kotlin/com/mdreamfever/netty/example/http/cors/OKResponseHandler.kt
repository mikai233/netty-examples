package com.mdreamfever.netty.example.http.cors

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*

class OKResponseHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        println(msg)
        val response =
            DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER).also {
                it.headers().set("custom-response-header", "Some value")
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
            }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }
}