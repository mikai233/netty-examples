package echo

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

import java.net.InetAddress

object EchoClient {
  private val ssl = true
  val port: Int = if (ssl) 8443 else 8989
  private val bootstrap = new Bootstrap()
  private val workGroup = new NioEventLoopGroup()
  private val sslContext = if (ssl) SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build() else null
  bootstrap.group(workGroup)
    .remoteAddress(InetAddress.getLocalHost, port)
    .channel(classOf[NioSocketChannel])
    .handler(new EchoClientChannelInitializer(sslContext))

  def main(args: Array[String]): Unit = {
    try {
      val channel = bootstrap.connect().sync().channel()
      channel.writeAndFlush("hello world")
      channel.closeFuture().sync()
    } finally {
      workGroup.shutdownGracefully()
    }
  }
}
