package com.mdreamfever.netty.example.http.snoop

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.netty.handler.codec.http.cookie.ServerCookieEncoder
import io.netty.util.CharsetUtil

class HttpSnoopServerHandler : SimpleChannelInboundHandler<HttpObject>() {
    private val stringBuilder = StringBuilder()
    private lateinit var request: HttpRequest
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
        when (msg) {
            is HttpRequest -> {
                request = msg
                HttpUtil.is100ContinueExpected(msg).takeIf { it }?.let {
                    send100Continue(ctx)
                }
                stringBuilder.apply {
                    setLength(0)
                    append("WELCOME TO TEH WILD WILD WEB SERVER\r\n")
                    append("===================================\r\n")
                    append("VERSION: ${msg.protocolVersion()}\r\n")
                    append("HOSTNAME: ${msg.headers()[HttpHeaderNames.HOST] ?: "unknown"}\r\n")
                    append("REQUEST_URI: ${msg.uri()}\r\n\r\n")
                }
                msg.headers().forEach { entry ->
                    stringBuilder.append("HEADER: ${entry.key}=${entry.value}\r\n")
                }
                stringBuilder.append("\r\n")

                val queryStringDecoder = QueryStringDecoder(msg.uri())
                queryStringDecoder.parameters().forEach { (t, u) ->
                    u.forEach {
                        stringBuilder.append("PARAM: $t=${it}\r\n")
                    }
                }
                stringBuilder.append("\r\n")
                appendDecoderResult(stringBuilder, msg)
            }
            is LastHttpContent -> {
                stringBuilder.append("END OF CONTENT\r\n")
                msg.trailingHeaders().takeUnless {
                    it.isEmpty
                }?.let {
                    stringBuilder.append("\r\n")
                    msg.trailingHeaders().names().forEach { name ->
                        msg.trailingHeaders().getAll(name).forEach {
                            stringBuilder.append("TRAILING HEADER: ")
                            stringBuilder.append("$name=$it\r\n")
                        }
                    }
                }
                if (!writeResponse(ctx, msg)) {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
                }
            }
            is HttpContent -> {
                val httpContent = msg.content()
                if (httpContent.isReadable) {
                    stringBuilder.apply {
                        append("CONTENT: ")
                        append(httpContent.toString(CharsetUtil.UTF_8))
                        append("\r\n")
                        appendDecoderResult(stringBuilder, request)
                    }
                }
            }
        }
    }

    private fun appendDecoderResult(stringBuilder: StringBuilder, msg: HttpObject) {
        val result = msg.decoderResult().also {
            it.takeIf { it.isFailure }?.let {
                return
            }
        }
        stringBuilder.apply {
            append(".. WITH DECODER FAILURE: ")
            append(result.cause())
            append("\r\n")
        }
    }

    private fun writeResponse(context: ChannelHandlerContext, currentObject: HttpObject): Boolean {
        val keepAlive = HttpUtil.isKeepAlive(request)
        val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            if (currentObject.decoderResult().isSuccess) HttpResponseStatus.OK else HttpResponseStatus.BAD_REQUEST,
            Unpooled.copiedBuffer(stringBuilder.toString(), CharsetUtil.UTF_8)
        )
        response.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/plain; charset=UTF-8"
        if (keepAlive) {
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
        }
        val cookieString = request.headers()[HttpHeaderNames.COOKIE]
        if (cookieString != null) {
            ServerCookieDecoder.STRICT.decode(cookieString).forEach {
                response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(it))
            }
        } else {
            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key1", "value1"))
            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key2", "value2"))
        }
        context.write(response)
        return keepAlive
    }

    private fun send100Continue(context: ChannelHandlerContext) {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER)
        context.write(response)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}