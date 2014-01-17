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
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import java.nio.MappedByteBuffer

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
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public class HTTPSession(var nanoHTTPD: NanoHTTPD, var tempFileManager: TempFileManager, var inputStream: InputStream, var outputStream: OutputStream, var inetAddress: InetAddress? = null) : IHTTPSession {
    private var splitbyte: Int = 0
    private var rlen: Int = 0
    override public var uri: String = ""
    override public var method: Method = Method.GET
    override public var parms: MutableMap<String, String> = hashMapOf()
    override public var headers: MutableMap<String, String> = hashMapOf()
    override public var cookies: CookieHandler? = null
    override public var queryParameterString: String = ""

    public override fun execute(): Unit {
        try
        {
            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header at once!
            val buf = ByteArray(BUFSIZE)
            splitbyte = 0
            rlen = 0

            var read = -1
            try
            {
                read = inputStream.read(buf, 0, BUFSIZE)!!
            }
            catch (e: Exception) {
                IOUtils.closeQuietly(inputStream)
                IOUtils.closeQuietly(outputStream)
                throw SocketException("NanoHttpd Shutdown")
            }

            if (read == -1)
            {
                // socket was been closed
                IOUtils.closeQuietly(inputStream)
                IOUtils.closeQuietly(outputStream)
                throw SocketException("NanoHttpd Shutdown")
            }

            while (read > 0)
            {
                rlen += read
                splitbyte = findHeaderEnd(buf, rlen)
                if (splitbyte > 0)
                    break

                read = inputStream.read(buf, rlen, BUFSIZE - rlen)!!
            }

            if (splitbyte < rlen)
            {
                val splitInputStream = ByteArrayInputStream(buf, splitbyte, rlen - splitbyte)
                val sequenceInputStream = SequenceInputStream(splitInputStream, inputStream)
                inputStream = sequenceInputStream
            }

            parms = HashMap<String, String>()
            if (null == headers)
            {
                headers = HashMap<String, String>()
            }

            // Create a BufferedReader for parsing the header.
            val hin = BufferedReader(InputStreamReader(ByteArrayInputStream(buf, 0, rlen)))
            // Decode the header into parms and header java properties
            val pre = HashMap<String, String>()
            decodeHeader(hin, pre, parms, headers)
            val methodStr = pre["method"]
            if (methodStr == null) {
                println(pre)
            }
            method = lookupMethod(methodStr)?: throw ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error.")
            uri = pre.get("uri")?:""
            cookies = CookieHandler(headers)
            // Ok, now do the serve()
            val r = nanoHTTPD.serve(this)
            cookies?.unloadQueue(r)
            r.requestMethod = method
            r.send(outputStream)
        }
        catch (e: SocketException) {
            // throw it out to close socket object (finalAccept)
            throw e
        }
        catch (ste: SocketTimeoutException) {
            throw ste
        }
        catch (ioe: IOException) {
            val r = Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage())
            r.send(outputStream)
            IOUtils.closeQuietly(outputStream)
        }
        catch (re: ResponseException) {
            val r = Response(re.status, MIME_PLAINTEXT, re.getMessage()?:"")
            r.send(outputStream)
            IOUtils.closeQuietly(outputStream)
        }
        finally
        {
            tempFileManager.clear()
        }
    }
    public override fun parseBody(files: MutableMap<String, String>): Unit {

        val randomAccessFile = getTmpBucket()
        randomAccessFile.use {
            var size: Long
            if (headers?.containsKey("content-length")?:false)
            {
                size = headers?.get("content-length")?.toLong()?:-1
            }
            else
                if (splitbyte < rlen)
                {
                    size = rlen - splitbyte.toLong()
                }
                else
                {
                    size = 0
                }
            // Now read all the body and write it to f
            val buf = ByteArray(512)
            while (rlen >= 0 && size > 0)
            {
                rlen = inputStream.read(buf, 0, 512)
                size -= rlen.toLong()
                if (rlen > 0)
                {
                    randomAccessFile.write(buf, 0, rlen)
                }

            }
            // Get the raw body as a byte []
            val fbuf: MappedByteBuffer = randomAccessFile.getChannel()!!.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length())!!
            randomAccessFile.seek(0)
            // Create a BufferedReader for easily reading it as string.
            val bin: InputStream = FileInputStream(randomAccessFile.getFD()!!)
            BufferedReader(InputStreamReader(bin)).use { ins ->
                if (Method.POST.equals(method))
                {
                    var contentType = ""
                    val contentTypeHeader = headers?.get("content-type")?:"text/plain"
                    var st: StringTokenizer = StringTokenizer(contentTypeHeader, ",; ")
                    if (st.hasMoreTokens())
                    {
                        contentType = st.nextToken()
                    }


                    if ("multipart/form-data".equalsIgnoreCase(contentType))
                    {
                        // Handle multipart/form-data
                        if (!st.hasMoreTokens())
                        {
                            throw ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html")
                        }

                        val boundaryStartString = "boundary="
                        val boundaryContentStart = contentTypeHeader.indexOf(boundaryStartString) + boundaryStartString.length()
                        var boundary = contentTypeHeader.substring(boundaryContentStart, contentTypeHeader.length())
                        if (boundary.startsWith("\"") && boundary.endsWith("\""))
                        {
                            boundary = boundary.substring(1, boundary.length() - 1)
                        }

                        decodeMultipartData(boundary, fbuf, ins, parms, files)
                    }
                    else
                    {
                        // Handle application/x-www-form-urlencoded
                        var postLine = ""
                        val pbuf = CharArray(512)
                        var read = ins.read(pbuf)
                        while (read >= 0 && !postLine.endsWith("\r\n"))
                        {
                            postLine += String(pbuf)
                            read = ins.read(pbuf)
                        }
                        postLine = postLine.trim()
                        decodeParms(postLine, parms)
                    }
                }
                else
                    if (Method.PUT.equals(method))
                    {
                        files.put("content", saveTmpFile(fbuf, 0, fbuf.limit()))
                    }
            }
            // If the method is POST, there may be parameters
            // in data section, too, read it:

        }


    }
    /**
     * Decodes the sent headers and loads the data into Key/value pairs
     */
    private fun decodeHeader(ins: BufferedReader, pre: MutableMap<String, String>, parms: MutableMap<String, String>, headers: MutableMap<String, String>?): Unit {
        try
        {
            // Read the request line
            val inLine = ins.readLine()
            if (inLine == null)
            {
                return
            }

            val st = StringTokenizer(inLine)
            if (!st.hasMoreTokens())
            {
                throw ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html")
            }

            pre.put("method", st.nextToken())
            if (!st.hasMoreTokens())
            {
                throw ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html")
            }

            var uri = st.nextToken()
            // Decode parameters from the URI
            val qmi = uri.indexOf('?')
            if (qmi >= 0)
            {
                decodeParms(uri.substring(qmi + 1), parms)
                uri = nanoHTTPD.decodePercent(uri.substring(0, qmi))
            }
            else
            {
                uri = nanoHTTPD.decodePercent(uri)
            }
            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens())
            {
                var line = ins.readLine()
                while (line?.trim()?.length()?:-1 > 0)
                {
                    val p = line!!.indexOf(':')
                    if (p >= 0)
                        headers?.put(line!!.substring(0, p).trim().toLowerCase(Locale.US), line!!.substring(p + 1).trim())

                    line = ins.readLine()
                }
            }

            pre.put("uri", uri)
        }
        catch (ioe: IOException) {
            throw ResponseException(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe)
        }

    }
    /**
     * Decodes the Multipart Body data and put it into Key/Value pairs.
     */
    private fun decodeMultipartData(boundary: String, fbuf: ByteBuffer, ins: BufferedReader, parms: MutableMap<String, String>, files: MutableMap<String, String>): Unit {
        try
        {
            val bpositions = getBoundaryPositions(fbuf, boundary.getBytes())
            var boundarycount = 1
            var mpline = ins.readLine()
            while (mpline != null)
            {
                if (!mpline?.contains(boundary)!!)
                {
                    throw ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html")
                }

                boundarycount++
                val item = HashMap<String, String>()
                mpline = ins.readLine()
                while (mpline?.trim()?.length()?:-1 > 0)
                {
                    val p = mpline?.indexOf(':')!!
                    if (p != -1)
                    {
                        item.put(mpline!!.substring(0, p).trim().toLowerCase(Locale.US), mpline!!.substring(p + 1).trim())
                    }

                    mpline = ins.readLine()
                }
                if (mpline != null)
                {
                    val contentDisposition = item.get("content-disposition")
                    if (contentDisposition == null)
                    {
                        throw ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html")
                    }

                    val st = StringTokenizer(contentDisposition, "; ")
                    val disposition = HashMap<String, String>()
                    while (st.hasMoreTokens())
                    {
                        val token = st.nextToken()
                        val p = token.indexOf('=')
                        if (p != -1)
                        {
                            disposition.put(token.substring(0, p).trim().toLowerCase(Locale.US), token.substring(p + 1).trim())
                        }

                    }
                    var pname: String = disposition.get("name")?:""
                    pname = pname.substring(1, pname.length() - 1)
                    var value = ""
                    if (item.get("content-type") == null)
                    {
                        while (!(mpline?.contains(boundary)?:false))
                        {
                            mpline = ins.readLine()
                            if (mpline != null)
                            {
                                val d = mpline!!.indexOf(boundary)
                                if (d == -1)
                                {
                                    value += mpline
                                }
                                else
                                {
                                    value += mpline!!.substring(0, d - 2)
                                }
                            }

                        }
                    }
                    else
                    {
                        if (boundarycount > bpositions.size)
                        {
                            throw ResponseException(Status.INTERNAL_ERROR, "Error processing request")
                        }

                        val offset = stripMultipartHeaders(fbuf, bpositions[boundarycount - 2])
                        val path = saveTmpFile(fbuf, offset, bpositions[boundarycount - 1] - offset - 4)
                        files.put(pname, path)
                        value = disposition.get("filename")?:""
                        value = value.substring(1, value.length() - 1)
                        do
                        {
                            mpline = ins.readLine()
                        }
                        while (!(mpline?.contains(boundary)?:false))
                    }
                    parms.put(pname, value)
                }

            }
        }
        catch (ioe: IOException) {
            throw ResponseException(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe)
        }

    }
    /**
     * Find byte index separating header from body. It must be the last byte of the first two sequential new lines.
     */
    private fun findHeaderEnd(buf: ByteArray, rlen: Int): Int {
        var splitbyte = 0
        while (splitbyte + 3 < rlen)
        {
            if (buf[splitbyte].toChar() == '\r' && buf[splitbyte + 1].toChar() == '\n' && buf[splitbyte + 2].toChar() == '\r' && buf[splitbyte + 3].toChar() == '\n')
            {
                return splitbyte + 4
            }

            splitbyte++
        }
        return 0
    }
    /**
     * Find the byte positions where multipart boundaries start.
     */
    private fun getBoundaryPositions(b: ByteBuffer, boundary: ByteArray): IntArray {
        var matchcount = 0
        var matchbyte = -1
        val matchbytes = ArrayList<Int>()
        var i = 0
        while (i < b.limit())
        {
            if (b[i] == boundary[matchcount])
            {
                if (matchcount == 0)
                    matchbyte = i

                matchcount++
                if (matchcount == boundary.size)
                {
                    matchbytes.add(matchbyte)
                    matchcount = 0
                    matchbyte = -1
                }

            }
            else
            {
                i -= matchcount
                matchcount = 0
                matchbyte = -1
            }

            i++
        }
        val ret = IntArray(matchbytes.size())
        for (j in 0..ret.size - 1) {
            ret[j] = matchbytes[j]
        }
        return ret
    }
    /**
     * Retrieves the content of a sent file and saves it to a temporary file. The full path to the saved file is returned.
     */
    private fun saveTmpFile(b: ByteBuffer, offset: Int, len: Int): String {

        if (len > 0)
        {
            try
            {
                val tempFile = tempFileManager.createTempFile()!!
                val src = b.duplicate()

                return FileOutputStream(tempFile.getName()!!) use  { fos ->
                    val dest = fos.getChannel()
                    src.position(offset).limit(offset + len)
                    dest.write(src.slice())
                    tempFile.getName()!!
                }

            }
            catch (e: Exception) {
                // Catch exception if any
                System.err.println("Error: " + e.getMessage())
                return ""
            }

        } else {
            return ""
        }

    }

    private fun getTmpBucket(): RandomAccessFile {
        try
        {
            val tempFile = tempFileManager.createTempFile()!!
            return RandomAccessFile(tempFile.getName()!!, "rw")
        }
        catch (e: Exception) {
            System.err.println("Error: " + e.getMessage())
            throw e
        }

    }

    /**
     * It returns the offset separating multipart file headers from the file's data.
     */
    private fun stripMultipartHeaders(b: ByteBuffer, offset: Int): Int {
        var i: Int = offset
        while (i < b.limit())
        {
            if (b.get(i).toChar() == '\r' && b.get(++i).toChar() == '\n' && b.get(++i).toChar() == '\r' && b.get(++i).toChar() == '\n')
            {
                break
            }

            i++
        }

        return i + 1
    }
    /**
     * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
     * adds them to given Map. NOTE: this doesn't support multiple identical keys due to the simplicity of Map.
     */
    private fun decodeParms(parms: String?, p: MutableMap<String, String>): Unit {
        if (parms == null)
        {
            queryParameterString = ""
            return
        }

        queryParameterString = parms
        val st = StringTokenizer(parms, "&")
        while (st.hasMoreTokens()!!)
        {
            val e = st.nextToken()
            val sep = e.indexOf('=')!!
            if (sep >= 0)
            {
                p.put(nanoHTTPD.decodePercent(e.substring(0, sep)).trim(), nanoHTTPD.decodePercent(e.substring(sep + 1)))
            }
            else
            {
                p.put(nanoHTTPD.decodePercent(e).trim(), "")
            }
        }
    }


    {
        val remoteIp = (if (inetAddress?.isLoopbackAddress()!! || inetAddress?.isAnyLocalAddress()!!)
            "127.0.0.1"
        else
            inetAddress?.getHostAddress().toString())
        headers = HashMap<String, String>()
        headers?.put("remote-addr", remoteIp)
        headers?.put("http-client-ip", remoteIp)
    }

    class object {
        public val BUFSIZE: Int = 8192
    }
}
