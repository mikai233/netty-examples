package com.mdreamfever.netty.example.factorial

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.math.BigInteger

class FactorialServerHandler : SimpleChannelInboundHandler<BigInteger>() {
    private var lastMultiplier = BigInteger("1")
    private var factorial = BigInteger("1")
    override fun channelRead0(ctx: ChannelHandlerContext, msg: BigInteger) {
        lastMultiplier = msg
        factorial = factorial.multiply(msg)
        ctx.writeAndFlush(factorial)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        println("Factorial of $lastMultiplier is: $factorial")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}