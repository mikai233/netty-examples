package com.mdreamfever.netty.example.http.file

import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import io.netty.util.CharsetUtil
import io.netty.util.internal.SystemPropertyUtil
import jakarta.activation.MimetypesFileTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class HttpStaticFileServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    private lateinit var request: FullHttpRequest

    companion object {
        const val httpDateFormat = "EEE, dd MMM yyy HH:mm:ss zzz"
        const val httpDateGmtTimezone = "GMT"
        const val httpCacheSeconds = 60
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        request = msg
        request.decoderResult().takeIf { !it.isSuccess }?.let {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST)
            return
        }
        request.method().takeIf { it != HttpMethod.GET }?.let {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }
        val keepAlive = HttpUtil.isKeepAlive(request)
        val uri = request.uri()
        val path = sanitizeUri(uri)
        if (path == null) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN)
            return
        }
        val file = File(path)
        if (file.isHidden || !file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND)
            return
        }
        if (file.isDirectory) {
            if (uri.endsWith("/")) {
                sendListing(ctx, file, uri)
            } else {
                sendRedirect(ctx, "$uri/")
            }
            return
        }
        val ifModifiedSince = request.headers()[HttpHeaderNames.IF_MODIFIED_SINCE]
        if (ifModifiedSince != null && ifModifiedSince.isNotEmpty()) {
            val dateFormatter = SimpleDateFormat(httpDateFormat, Locale.US)
            val ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince)
            val ifModifiedSinceDateSeconds = ifModifiedSinceDate.time / 1000
            val fileLastModifiedSeconds = file.lastModified() / 1000
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx)
                return
            }
        }
        val randomAccessFile = try {
            RandomAccessFile(file, "r")
        } catch (ignore: FileNotFoundException) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND)
            return
        }
        val fileLength = randomAccessFile.length()
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        HttpUtil.setContentLength(response, fileLength)
        setContentTypeHeader(response, file)
        setDateAndCacheHeaders(response, file)
        if (!keepAlive) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
        } else if (request.protocolVersion() == HttpVersion.HTTP_1_0) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
        }
        ctx.write(response)
        val sendFileFuture: ChannelFuture
        val lastContentFuture: ChannelFuture
        if (ctx.pipeline().get(SslHandler::class.java) == null) {
            sendFileFuture =
                ctx.write(DefaultFileRegion(randomAccessFile.channel, 0, fileLength), ctx.newProgressivePromise())
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
        } else {
            sendFileFuture = ctx.writeAndFlush(
                HttpChunkedInput(ChunkedFile(randomAccessFile, 0, fileLength, 8192)),
                ctx.newProgressivePromise()
            )
            lastContentFuture = sendFileFuture
        }
        sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
            override fun operationComplete(future: ChannelProgressiveFuture) {
                println("${future.channel()} Transfer complete.")
            }

            override fun operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long) {
                if (total < 0) {
                    println("${future.channel()} Transfer progress: $progress")
                } else {
                    println("${future.channel()} Transfer progress: $progress / $total")
                }
            }
        })
        if (!keepAlive) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.channel().takeIf { it.isActive }?.let {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun sanitizeUri(uri: String): String? {
        val insecureUri = Pattern.compile(".*[<>&\"].*")
        var decodeUri = try {
            URLDecoder.decode(uri, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw Error(e)
        }
        if (decodeUri.isEmpty() || decodeUri.first() != '/')
            return null
        decodeUri = decodeUri.replace('/', File.separatorChar)
        if (decodeUri.contains(File.separator + '.') ||
            decodeUri.contains('.' + File.separator) ||
            decodeUri.last() == '.' ||
            insecureUri.matcher(decodeUri).matches()
        )
            return null
        return SystemPropertyUtil.get("user.dir") + File.separator + decodeUri
    }

    private fun sendListing(ctx: ChannelHandlerContext, dir: File, dirPath: String) {
        val allowedFileName = Pattern.compile("[^-._]?[^<>&\"]*")
        val buf = StringBuilder()
            .append("<!DOCTYPE html>\r\n")
            .append("<html><head><meta charset='utf-8' /><title>")
            .append("Listing of: ")
            .append(dirPath)
            .append("</title></head><body>\r\n")
            .append("<h3>Listing of: ")
            .append(dirPath)
            .append("</h3>\r\n")
            .append("<ul>")
            .append("<li><a href=\"../\">..</a></li>\r\n")
        dir.listFiles()?.let { files ->
            for (file in files) {
                if (file.isHidden || !file.canRead()) {
                    continue
                }
                val name = file.name
                if (!allowedFileName.matcher(name).matches()) {
                    continue
                }
                buf.append("<li><a href=\"")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a></li>\r\n")
            }
        }
        buf.append("</ul></body></html>\r\n")
        val buffer = ctx.alloc().buffer(buf.length)
        buffer.writeCharSequence(buf.toString(), CharsetUtil.UTF_8)
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer)
            .also {
                it.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/html; charset=UTF-8"
            }
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendRedirect(ctx: ChannelHandlerContext, newUri: String) {
        val response =
            DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER)
                .also {
                    it.headers()[HttpHeaderNames.LOCATION] = newUri
                }
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
        val response: FullHttpResponse = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: $status\r\n", CharsetUtil.UTF_8)
        ).also {
            it.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/plain; charset=UTF-8"
        }
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendNotModified(ctx: ChannelHandlerContext) {
        val response =
            DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, Unpooled.EMPTY_BUFFER)
        setDateHeader(response)
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendAndCleanupConnection(ctx: ChannelHandlerContext, response: FullHttpResponse) {
        val keepAlive = HttpUtil.isKeepAlive(request)
        HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
        if (!keepAlive) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
        } else if (request.protocolVersion() == HttpVersion.HTTP_1_0) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
        }
        val flushPromise = ctx.writeAndFlush(response)
        if (!keepAlive) {
            flushPromise.addListener(ChannelFutureListener.CLOSE)
        }
    }

    private fun setDateHeader(response: FullHttpResponse) {
        val dateFormatter = SimpleDateFormat(httpDateFormat, Locale.US)
        dateFormatter.timeZone = TimeZone.getTimeZone(httpDateGmtTimezone)
        val time = GregorianCalendar()
        response.headers()[HttpHeaderNames.DATE] = dateFormatter.format(time.time)
    }

    private fun setDateAndCacheHeaders(response: HttpResponse, fileToCache: File) {
        val dateFormatter = SimpleDateFormat(httpDateFormat, Locale.US)
        dateFormatter.timeZone = TimeZone.getTimeZone(httpDateGmtTimezone)
        val time: Calendar = GregorianCalendar()
        response.headers()[HttpHeaderNames.DATE] = dateFormatter.format(time.time)
        time.add(Calendar.SECOND, httpCacheSeconds)
        response.headers()[HttpHeaderNames.EXPIRES] = dateFormatter.format(time.time)
        response.headers()[HttpHeaderNames.CACHE_CONTROL] =
            "private, max-age=$httpCacheSeconds"
        response.headers()[HttpHeaderNames.LAST_MODIFIED] = dateFormatter.format(Date(fileToCache.lastModified()))
    }

    private fun setContentTypeHeader(response: HttpResponse, file: File) {
        val mimeTypesMap = MimetypesFileTypeMap()
        response.headers()[HttpHeaderNames.CONTENT_TYPE] = mimeTypesMap.getContentType(file.path)
    }

}