package discard

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

object DiscardClient {
  private val ssl = true
  private val host = "127.0.0.1"
  private val port = 8080
  private val workGroup = new NioEventLoopGroup
  private val bootstrap = new Bootstrap
  private val sslContext = if (ssl) SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build() else null
  bootstrap
    .group(workGroup)
    .channel(classOf[NioSocketChannel])
    .remoteAddress(host, port)
    .handler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        val pipeline = ch.pipeline()
        if (sslContext != null) {
          pipeline.addLast(sslContext.newHandler(ch.alloc(), host, port))
        }
        pipeline.addLast(new DiscardClientHandler)
      }
    })

  def main(args: Array[String]): Unit = {
    try {
      bootstrap
        .connect()
        .sync()
        .channel()
        .closeFuture()
        .sync()
    } finally {
      workGroup.shutdownGracefully()
    }
  }
}
