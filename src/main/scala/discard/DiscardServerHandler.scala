package discard

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class DiscardServerHandler extends SimpleChannelInboundHandler[ByteBuf] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = println("server discard a client message")

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}
