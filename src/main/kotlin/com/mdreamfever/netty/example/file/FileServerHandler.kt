package com.mdreamfever.netty.example.file

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.DefaultFileRegion
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import java.io.FileNotFoundException
import java.io.RandomAccessFile

class FileServerHandler : SimpleChannelInboundHandler<String>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
        println("server read $msg")
        try {
            RandomAccessFile(msg, "r").use {
                if (ctx.pipeline().get(SslHandler::class.java) != null) {
                    ctx.writeAndFlush(DefaultFileRegion(it.channel, 0, it.length()))
                } else {
                    ctx.writeAndFlush(ChunkedFile(it))
                }
            }
        } catch (e: FileNotFoundException) {
            ctx.writeAndFlush("文件不存在")
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (ctx.channel().isActive) {
            ctx.writeAndFlush("ERR: ${cause.javaClass.simpleName}: ${cause.message}")
                .addListener { ChannelFutureListener.CLOSE }
        }
    }
}