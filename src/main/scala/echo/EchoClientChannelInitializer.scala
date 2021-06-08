package echo

import echo.EchoClient.port
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.netty.handler.ssl.SslContext

import java.net.InetAddress

class EchoClientChannelInitializer(private val sslContext: SslContext) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val pipeline = ch.pipeline()
    if (sslContext != null) {
      pipeline.addLast(sslContext.newHandler(ch.alloc(), InetAddress.getLocalHost.toString, port))
    }
    pipeline.addLast(new StringDecoder())
    pipeline.addLast(new StringEncoder())
    pipeline.addLast(new EchoClientHandler())
  }
}
