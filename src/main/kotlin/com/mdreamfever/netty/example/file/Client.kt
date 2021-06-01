package com.mdreamfever.netty.example.file

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.io.BufferedWriter
import java.io.FileWriter

fun main() {
    val client = Client()
    client.connect()
}

class Client(
    private val fileName: String = "D:/r.txt",
    private val outFileName: String = "D:/rr.txt",
    private val port: Int = 8080
) {
    private val bootstrap = Bootstrap()
    private val group = NioEventLoopGroup()

    init {
        bootstrap
            .group(group)
            .remoteAddress("127.0.0.1", port)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        addLast(StringEncoder())
                        addLast(StringDecoder())
                        addLast(object : SimpleChannelInboundHandler<String>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
                                BufferedWriter(FileWriter(outFileName)).use {
                                    it.write(msg)
                                }
                            }

                            override fun channelReadComplete(ctx: ChannelHandlerContext) {
                                ctx.close().sync()
                            }
                        })
                    }
                }
            })
    }

    fun connect() {
        try {
            val channel = bootstrap.connect().sync().channel()
            channel.writeAndFlush(fileName)
            channel.closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}