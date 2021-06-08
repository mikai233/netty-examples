package com.mdreamfever.netty.example.factorial

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.math.BigInteger
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.min

class FactorialClientHandler : SimpleChannelInboundHandler<BigInteger>() {
    private lateinit var context: ChannelHandlerContext
    private var receivedMessage = 0
    private var next = 1
    val answer = LinkedBlockingDeque<BigInteger>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: BigInteger) {
        receivedMessage++
        if (receivedMessage == FactorialClient.count) {
            ctx.close().addListener {
                answer.offer(msg)
            }
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        context = ctx
        sendNumbers()
    }

    private fun sendNumbers() {
        var future: ChannelFuture? = null
        for (i in 0 until min(4096, FactorialClient.count)) {
            future = context.write(next).also {
                next++
            }
        }
        if (next <= FactorialClient.count) {
            future?.addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (future.isSuccess) {
                        sendNumbers()
                    } else {
                        future.cause().printStackTrace()
                        future.channel().close()
                    }
                }
            })
        }
        context.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}