package discard

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

import java.time.LocalDate
import java.util.concurrent.{ExecutorService, Executors}

class DiscardClientHandler extends SimpleChannelInboundHandler[String] {
  var content: ByteBuf = _
  val service: ExecutorService = Executors.newSingleThreadExecutor()

  override def channelRead0(ctx: ChannelHandlerContext, msg: String): Unit = println(msg)

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    content = ctx.alloc().directBuffer(256).writeZero(256)
    service.submit(new Runnable {
      override def run(): Unit = while (true) {
        if (ctx.channel().isActive) {
          println(s"client send a message at ${LocalDate.now()}")
          ctx.channel().writeAndFlush(content.retainedDuplicate())
          Thread.sleep(1000)
        }
      }
    })
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}
