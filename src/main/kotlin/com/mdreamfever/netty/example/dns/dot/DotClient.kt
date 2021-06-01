package com.mdreamfever.netty.example.dns.dot

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.dns.*
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.NetUtil
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun main() {
    val dotClient = DotClient()
    dotClient.connect()
}

class DotClient(
    private val queryDomain: String = "www.baidu.com",
    private val dnsServerPort: Int = 853,
    private val dnsServerHost: String = "8.8.8.8"
) {
    private val group = NioEventLoopGroup()
    private val bootstrap = Bootstrap()

    init {
        val sslContext = SslContextBuilder.forClient()
            .protocols("TLSv1.3", "TLSv1.2")
            .build()
        bootstrap
            .group(group)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        addLast(sslContext.newHandler(ch.alloc(), dnsServerHost, dnsServerPort))
                        addLast(TcpDnsQueryEncoder())
                        addLast(TcpDnsResponseDecoder())
                        addLast(object : SimpleChannelInboundHandler<DefaultDnsResponse>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultDnsResponse) {
                                try {
                                    handleQueryResp(msg)
                                } finally {
                                    ctx.close()
                                }
                            }
                        })
                    }
                }
            })
    }

    private fun handleQueryResp(defaultDnsResponse: DefaultDnsResponse) {
        defaultDnsResponse.takeIf { it.count(DnsSection.QUESTION) > 0 }?.let {
            val question = it.recordAt<DnsQuestion>(DnsSection.QUESTION, 0)
            println("name: ${question.name()}")
        }
        for (i in 0 until defaultDnsResponse.count(DnsSection.ANSWER)) {
            val record = defaultDnsResponse.recordAt<DnsRecord>(DnsSection.ANSWER, i)
            record.takeIf { it.type() == DnsRecordType.A }?.let {
                val raw = it as DnsRawRecord
                println(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())))
            }
        }
    }

    fun connect() {
        try {
            val channel = bootstrap.connect(dnsServerHost, dnsServerPort).sync().channel()
            val randomId = Random.nextInt(60000 - 1000) + 1000
            val query =
                DefaultDnsQuery(randomId, DnsOpCode.QUERY)
                    .setRecord(DnsSection.QUESTION, DefaultDnsQuestion(queryDomain, DnsRecordType.A))
            channel.writeAndFlush(query).sync()
            val success = channel.closeFuture().await(10, TimeUnit.SECONDS)
            success.takeIf { !it }?.let {
                println("dns query timeout!")
                channel.close().sync()
            }
        } finally {
            group.shutdownGracefully()
        }
    }
}