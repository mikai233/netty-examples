package com.mdreamfever.netty.example.http.helloworld

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil

class HelloWorldServerHandler : SimpleChannelInboundHandler<HttpRequest>() {
    private val content = Unpooled.directBuffer().writeBytes("hello world".toByteArray())
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val keepAlive = HttpUtil.isKeepAlive(msg)
        val response =
            DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK, content.retainedDuplicate()).also {
                it.headers()[HttpHeaderNames.CONTENT_TYPE] = HttpHeaderValues.TEXT_PLAIN
                it.headers()[HttpHeaderNames.CONTENT_LENGTH] = content.readableBytes()
            }
        if (keepAlive) {
            if (!msg.protocolVersion().isKeepAliveDefault) {
                response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
            }
        } else {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
        }
        val future = ctx.write(response)
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE)
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ReferenceCountUtil.release(content)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}