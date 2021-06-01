package com.mdreamfever.netty.example.dns.udp

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.dns.*
import io.netty.util.NetUtil
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

fun main() {
    val dnsClient = DnsClient()
    dnsClient.connect()
}

class DnsClient(
    private val queryDomain: String = "www.baidu.com",
    private val dnsServerHost: String = "8.8.8.8",
    private val dnsServerPort: Int = 53
) {
    private val group = NioEventLoopGroup()
    private val bootstrap = Bootstrap()

    init {
        bootstrap
            .group(group)
            .channel(NioDatagramChannel::class.java)
            .handler(object : ChannelInitializer<DatagramChannel>() {
                override fun initChannel(ch: DatagramChannel) {
                    ch.pipeline().apply {
                        addLast(DatagramDnsQueryEncoder())
                        addLast(DatagramDnsResponseDecoder())
                        addLast(object : SimpleChannelInboundHandler<DatagramDnsResponse>() {
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: DatagramDnsResponse) {
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

    private fun handleResponse(resp: DatagramDnsResponse) {
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
            val channel = bootstrap.bind(0).sync().channel()
            val dnsQuery = DatagramDnsQuery(null, InetSocketAddress(dnsServerHost, dnsServerPort), 1)
                .setRecord(DnsSection.QUESTION, DefaultDnsQuestion(queryDomain, DnsRecordType.A))
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