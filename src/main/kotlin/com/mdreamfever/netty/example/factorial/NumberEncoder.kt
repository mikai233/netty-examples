package com.mdreamfever.netty.example.factorial

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import java.math.BigInteger

class NumberEncoder : MessageToByteEncoder<Number>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Number, out: ByteBuf) {
        val value = if (msg is BigInteger) {
            msg
        } else {
            BigInteger(msg.toString())
        }
        val data = value.toByteArray()
        val length = data.size
        out.apply {
            writeChar('F'.code)
            writeInt(length)
            writeBytes(data)
        }
    }
}