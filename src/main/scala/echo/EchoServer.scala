package echo

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

object EchoServer {
  private val ssl = true
  val port: Int = if (ssl) 8443 else 8989
  private val sslContext = if (ssl) {
    val selfSignedCertificate = new SelfSignedCertificate()
    SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build()
  } else null
  private val serverBootstrap = new ServerBootstrap()
  private val bossGroup = new NioEventLoopGroup()
  private val workGroup = new NioEventLoopGroup()
  serverBootstrap.group(bossGroup, workGroup)
    .channel(classOf[NioServerSocketChannel])
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new EchoServerChannelInitializer(sslContext))

  def main(args: Array[String]): Unit = {
    try {
      val channel = serverBootstrap.bind(port).sync().channel()
      channel.closeFuture().sync()
    } finally {
      bossGroup.shutdownGracefully()
      workGroup.shutdownGracefully()
    }
  }
}
