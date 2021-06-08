package com.mdreamfever.netty.example.factorial

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetAddress

fun main() {
    FactorialClient().run {
        connect()
    }
}

class FactorialClient(private val port: Int = 8989) {
    val workGroup = NioEventLoopGroup()
    val bootstrap = Bootstrap()

    companion object {
        const val count = 1000
    }

    init {
        bootstrap.remoteAddress(InetAddress.getLocalHost(), port)
            .group(workGroup)
            .channel(NioSocketChannel::class.java)
            .handler(FactorialClientInitializer())
    }

    fun connect() {
        try {
            val channel = bootstrap.connect().sync().channel()
            val factorialClientHandler = channel.pipeline().last() as FactorialClientHandler
            val answer = factorialClientHandler.answer.take()
            println("Factorial of $count is: $answer")
        } finally {
            workGroup.shutdownGracefully()
        }
    }
}
