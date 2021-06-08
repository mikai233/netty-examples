package com.mdreamfever.netty.example.factorial

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.ZlibCodecFactory
import io.netty.handler.codec.compression.ZlibWrapper

class FactorialServerInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().apply {
            addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP))
            addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP))
            addLast(BigIntegerDecoder())
            addLast(NumberEncoder())
            addLast(FactorialServerHandler())
        }
    }
}