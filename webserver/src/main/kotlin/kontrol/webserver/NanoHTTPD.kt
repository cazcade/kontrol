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

import java.io.*
import java.net.*
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
 * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
 * This is required as the Keep-Alive HTTP connections would otherwise
 * block the socket reading thread forever (or as long the browser is open).
 */
public val SOCKET_READ_TIMEOUT: Int = 5000
/**
 * Common mime type for dynamic content: plain text
 */
public val MIME_PLAINTEXT: String = "text/plain"
/**
 * Common mime type for dynamic content: html
 */
public val MIME_HTML: String = "text/html"
/**
 * Pseudo-Parameter to use to store the actual query string in the parameters map for later re-processing.
 */
private val QUERY_STRING_PARAMETER: String = "NanoHttpd.QUERY_STRING"


public open class NanoHTTPD(val hostname: String?, val myPort: Int = 8080, var runner: AsyncRunner = DefaultAsyncRunner(), var tempFileManagerFactory: TempFileManagerFactory = DefaultTempFileManagerFactory()) {
    private var myServerSocket: ServerSocket? = null
    private var openConnections: MutableSet<Socket> = HashSet<Socket>()
    private var myThread: Thread? = null

    /**
     * Start the server.
     *
     * @throws IOException if the socket is in use.
     */
    public  fun startServer(): NanoHTTPD {
        myServerSocket = ServerSocket()
        myServerSocket?.bind((if (hostname != null) InetSocketAddress(hostname, myPort) else InetSocketAddress(myPort)))
        myThread = Thread {
            do
            {
                try
                {
                    val finalAccept = myServerSocket?.accept()!!
                    registerConnection(finalAccept)
                    finalAccept.setSoTimeout(SOCKET_READ_TIMEOUT)
                    val inputStream = finalAccept.getInputStream()
                    if (inputStream == null) {
                        safeCloseSocket(finalAccept)
                        unRegisterConnection(finalAccept)
                    } else {
                        runner.exec {
                            try
                            {
                                val outputStream = finalAccept.getOutputStream()!!
                                outputStream.use {
                                    val tempFileManager = tempFileManagerFactory.create()!!
                                    val session = HTTPSession(this@NanoHTTPD, tempFileManager, inputStream, outputStream, finalAccept.getInetAddress())
                                    while (!finalAccept.isClosed())
                                    {
                                        session.execute()
                                    }
                                }
                            }
                            catch (e: Exception) {
                                // When the socket is closed by the client, we throw our own SocketException
                                // to break the  "keep alive" loop above.
                                if (!(e is SocketException && "NanoHttpd Shutdown".equals(e.getMessage())))
                                {
                                    e.printStackTrace()
                                }

                            } finally {
                                safeCloseCloseable(inputStream)
                                safeCloseSocket(finalAccept)
                                unRegisterConnection(finalAccept)
                            }
                        }


                    }
                }
                catch (e: IOException) {

                }

            }
            while (!myServerSocket?.isClosed()!!)


        }
        myThread?.setDaemon(true)
        myThread?.setName("NanoHttpd Main Listener")
        myThread?.start()
        return this;
    }
    /**
     * Stop the server.
     */
    public  fun stopServer(): Unit {
        try
        {
            safeCloseServerSocket(myServerSocket)
            closeAllConnections()
            myThread?.join()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

    }
    /**
     * Registers that a new connection has been set up.
     *
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public  fun registerConnection(socket: Socket): Unit {
        openConnections.add(socket)
    }
    /**
     * Registers that a connection has been closed
     *
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public  fun unRegisterConnection(socket: Socket): Unit {
        openConnections.remove(socket)
    }
    /**
     * Forcibly closes all connections that are open.
     */
    public  fun closeAllConnections(): Unit {
        for (socket in openConnections)
        {
            safeCloseSocket(socket)
        }
    }
    public fun getListeningPort(): Int = myServerSocket?.getLocalPort()?:-1

    public fun wasStarted(): Boolean = myServerSocket != null && myThread != null
    public fun isAlive(): Boolean = wasStarted() && !(myServerSocket?.isClosed()?:true) && myThread?.isAlive()?:false
    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param uri     Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method  "GET", "POST" etc.
     * @param parms   Parsed, percent decoded parameters from URI and, in case of POST, data.
     * @param headers Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    public open fun serve(uri: String, method: Method, headers: Map<String, String>, parms: Map<String, String>, files: Map<String, String>): Response {
        return Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }
    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param session The HTTP session
     * @return HTTP response, see class Response for details
     */
    public open fun serve(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        val method = session.method
        if (Method.PUT == method || Method.POST == method)
        {
            try
            {
                session.parseBody(files)
            }
            catch (ioe: IOException) {
                return Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage())
            }
            catch (re: ResponseException) {
                return Response(re.status, MIME_PLAINTEXT, re.getMessage()!!)
            }

        }

        session.parms.put(QUERY_STRING_PARAMETER, session.queryParameterString)
        return serve(session.uri, method, session.headers, session.parms, files)
    }
    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes "foo bar"
     */
    public  fun decodePercent(str: String): String = URLDecoder.decode(str, "UTF8")

    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been
     * supplied several times, by return lists of values.  In general these lists will contain a single
     * element.
     *
     * @param parms original <b>NanoHttpd</b> parameters values, as passed to the <code>serve()</code> method.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected  fun decodeParameters(parms: Map<String, String>): Map<String, MutableList<String>> = decodeParameters(parms.get(QUERY_STRING_PARAMETER))
    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been
     * supplied several times, by return lists of values.  In general these lists will contain a single
     * element.
     *
     * @param queryString a query string pulled from the URL.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected  fun decodeParameters(queryString: String?): Map<String, MutableList<String>> {
        val parms = HashMap<String, MutableList<String>>()
        if (queryString != null)
        {
            val st = StringTokenizer(queryString, "&")
            while (st.hasMoreTokens())
            {
                val e = st.nextToken()
                val sep = e.indexOf('=')
                val propertyName = (if ((sep >= 0))
                    decodePercent(e.substring(0, sep)).trim()
                else
                    decodePercent(e).trim())
                if (!parms.containsKey(propertyName))
                {
                    parms.put(propertyName, ArrayList<String>())
                }

                val propertyValue = (if ((sep >= 0))
                    decodePercent(e.substring(sep + 1))
                else
                    null)
                if (propertyValue != null) {
                    parms.get(propertyName)?.add(propertyValue)
                }

            }
        }

        return parms
    }



    // ------------------------------------------------------------------------------- //
    {

    }

    //        public open fun init(port: Int): NanoHTTPD {
    //            val __ = NanoHTTPD(null, port)
    //            return __
    //        }


    private fun safeCloseServerSocket(serverSocket: ServerSocket?): Unit {
        if (serverSocket != null)
        {
            try
            {
                serverSocket.close()
            }
            catch (e: IOException) {

            }

        }

    }
    private fun safeCloseSocket(socket: Socket?): Unit {
        if (socket != null)
        {
            try
            {
                socket.close()
            }
            catch (e: IOException) {

            }

        }

    }
    fun safeCloseCloseable(closeable: Closeable?): Unit {
        if (closeable != null)
        {
            try
            {
                closeable.close()
            }
            catch (e: IOException) {

            }

        }

    }


}
