package echo

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class EchoServerHandler extends SimpleChannelInboundHandler[String] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: String): Unit = {
    println(s"server read ${msg}")
    ctx.writeAndFlush(msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}
