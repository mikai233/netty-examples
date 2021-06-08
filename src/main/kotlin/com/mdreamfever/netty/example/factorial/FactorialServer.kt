package com.mdreamfever.netty.example.factorial

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

fun main() {
    FactorialServer().run {
        bind()
    }
}

class FactorialServer(private val port: Int = 8989) {
    private val bossGroup = NioEventLoopGroup()
    private val workGroup = NioEventLoopGroup()
    private val serverBootstrap = ServerBootstrap()

    init {
        serverBootstrap
            .group(bossGroup, workGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(FactorialServerInitializer())
    }

    fun bind() {
        try {
            val channel = serverBootstrap.bind(port).sync().channel()
            channel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workGroup.shutdownGracefully()
        }
    }
}