package com.mdreamfever.netty.example.factorial

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException
import java.math.BigInteger

/**
 * Decodes the binary representation of a {@link BigInteger} prepended
 * with a magic number ('F' or 0x46) and a 32-bit integer length prefix into a
 * {@link BigInteger} instance.  For example, { 'F', 0, 0, 0, 1, 42 } will be
 * decoded into new BigInteger("42").
 */
class BigIntegerDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        `in`.takeIf { it.readableBytes() >= 5 }?.let {
            it.markReaderIndex()
            it.takeIf { it.readChar() != 'F' }?.let {
                throw CorruptedFrameException("Invalid magic number: $it")
            }
            val dataLength = it.readInt()
            if (`in`.readableBytes() < dataLength) {
                `in`.resetReaderIndex()
                return
            }
            val decoded = ByteArray(dataLength)
            `in`.readBytes(decoded)
            out.add(BigInteger(decoded))
        }
    }
}