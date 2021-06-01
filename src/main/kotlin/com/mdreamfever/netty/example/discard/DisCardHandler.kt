package com.mdreamfever.netty.example.discard

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class DiscardClientHandler : SimpleChannelInboundHandler<ByteBuf>(), CoroutineScope {
    lateinit var content: ByteBuf
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        TODO("Not yet implemented")
    }

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    override fun channelActive(ctx: ChannelHandlerContext) {
        content = ctx.alloc().directBuffer(256).writeZero(256)
        launch {
            while (true) {
                println("client send a message at ${LocalDateTime.now()}")
                ctx.writeAndFlush(content.retainedDuplicate())
                delay(1000)
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}