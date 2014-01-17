/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kontrol.webserver

import org.apache.commons.io.IOUtils
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * HTTP response. Return one of these from serve().
 */



public open class Response(var status: Status = Status.OK, var mimeType: String = "text/html", text: String? = null, var data: InputStream? = if (text != null) ByteArrayInputStream(text.getBytes("UTF-8")) else null) {

    /**
     * Headers for the HTTP response. Use addHeader() to add lines.
     */
    var header: MutableMap<String, String> = HashMap<String, String>()
    /**
     * The request method that spawned this response.
     */
    var requestMethod: Method? = null
    /**
     * Use chunkedTransfer
     */
    var chunkedTransfer: Boolean = false
    /**
     * Adds given line to the header.
     */
    public open fun addHeader(name: String, value: String): Unit {
        header.put(name, value)
    }
    /**
     * Sends given response to the socket.
     */
    open fun send(outputStream: OutputStream): Unit {
        val mime = mimeType
        val gmtFrmt = SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"))
        try
        {
            val pw = PrintWriter(outputStream)
            pw.print("HTTP/1.1 " + status.getFullDescription() + " \r\n")
            pw.print("Content-Type: " + mime + "\r\n")
            if ( header.get("Date") == null)
            {
                pw.print("Date: " + gmtFrmt.format(Date()) + "\r\n")
            }

            for (key in header.keySet()!!)
            {
                val value = header.get(key)
                pw.print(key + ": " + value + "\r\n")
            }

            pw.print("Connection: keep-alive\r\n")
            if (requestMethod != Method.HEAD && chunkedTransfer)
            {
                sendAsChunked(outputStream, pw)
            }
            else
            {
                sendAsFixedLength(outputStream, pw)
            }
            outputStream.flush()
            IOUtils.closeQuietly(data)
        }
        catch (ioe: IOException) {
            // Couldn't write? No can do.
        }

    }
    private fun sendAsChunked(outputStream: OutputStream, pw: PrintWriter): Unit {
        pw.print("Transfer-Encoding: chunked\r\n")
        pw.print("\r\n")
        pw.flush()
        val BUFFER_SIZE = 16 * 1024
        val CRLF = "\r\n".getBytes()
        val buff = ByteArray(BUFFER_SIZE)
        while ( true)
        {
            val read = data?.read(buff)!!
            if (read < 0) {
                break
            }
            outputStream.write("%x\r\n".format(read).getBytes())
            outputStream.write(buff, 0, read)
            outputStream.write(CRLF)
        }
        outputStream.write(("0\r\n\r\n".format().getBytes()))
    }
    private fun sendAsFixedLength(outputStream: OutputStream, pw: PrintWriter): Unit {
        var pending: Int = (if (data != null)
            data?.available()?:-1
        else
            0)
        pw.print("Content-Length: " + pending + "\r\n")
        pw.print("\r\n")
        pw.flush()
        if (requestMethod != Method.HEAD && data != null)
        {
            val BUFFER_SIZE = 16 * 1024
            val buff = ByteArray(BUFFER_SIZE)
            while (pending > 0)
            {
                val read = data?.read(buff, 0, ((if ((pending > BUFFER_SIZE))
                    BUFFER_SIZE
                else
                    pending)))!!
                if (read <= 0)
                {
                    break
                }

                outputStream.write(buff, 0, read)
                pending -= read
            }
        }

    }


}
