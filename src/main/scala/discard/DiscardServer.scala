package discard

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.handler.ssl.{SslContext, SslContextBuilder}

object DiscardServer {
  private val ssl = true
  val port = 8080
  val serverBootstrap = new ServerBootstrap()
  val bossGroup = new NioEventLoopGroup()
  val workGroup = new NioEventLoopGroup()
  val sslContext: SslContext = if (ssl) {
    val selfSignedCertificate = new SelfSignedCertificate()
    SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build()
  } else null
  serverBootstrap
    .group(bossGroup, workGroup)
    .channel(classOf[NioServerSocketChannel])
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        val pipeline = ch.pipeline()
        if (sslContext != null) {
          pipeline.addLast(sslContext.newHandler(ch.alloc()))
        }
        pipeline.addLast(new DiscardServerHandler)
      }
    })

  def main(args: Array[String]): Unit = {
    try {
      serverBootstrap
        .bind(port)
        .sync()
        .channel()
        .closeFuture()
        .sync()
    } finally {
      workGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}
