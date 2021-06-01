package com.mdreamfever.netty.example.dns.tcp

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.dns.*
import io.netty.util.NetUtil
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun main() {
    val tcpDnsClient = TcpDnsClient()
    tcpDnsClient.connect()
}

class TcpDnsClient(
    private val queryDomain: String = "www.baidu.com",
    private val dnsServerHost: String = "8.8.8.8",
    private val dnsServerPort: Int = 53
) {
    private val group = NioEventLoopGroup()
    private val bootstrap = Bootstrap()

    init {
        bootstrap
            .group(group)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        addLast(TcpDnsQueryEncoder())
                        addLast(TcpDnsResponseDecoder())
                        addLast(object : SimpleChannelInboundHandler<DefaultDnsResponse>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultDnsResponse) {
                                try {
                                    handleResponse(msg)
                                } finally {
                                    ctx.close()
                                }
                            }
                        })
                    }
                }
            })
    }

    private fun handleResponse(resp: DefaultDnsResponse) {
        resp.takeIf { resp.count(DnsSection.QUESTION) > 0 }?.let {
            val record = resp.recordAt<DnsQuestion>(DnsSection.QUESTION, 0)
            println("name: ${record.name()}")
        }
        for (i in 0 until resp.count(DnsSection.ANSWER)) {
            val record = resp.recordAt<DnsRecord>(DnsSection.ANSWER, i)
            record.takeIf { it.type() == DnsRecordType.A }?.let {
                val raw = record as DnsRawRecord
                println(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())))
            }
        }
    }

    fun connect() {
        try {
            val channel = bootstrap.connect(dnsServerHost, dnsServerPort).sync().channel()
            val randomId = Random.nextInt(60000 - 1000) + 1000
            val dnsQuery = DefaultDnsQuery(randomId, DnsOpCode.QUERY)
            dnsQuery.setRecord(DnsSection.QUESTION, DefaultDnsQuestion(queryDomain, DnsRecordType.A))
            channel.writeAndFlush(dnsQuery).sync()
            channel.closeFuture().await(10, TimeUnit.SECONDS).takeIf { !it }?.let {
                println("dns query timeout!")
                channel.close().sync()
            }

        } finally {
            group.shutdownGracefully()
        }
    }
}