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

package org.apache.http.conn.ssl;

import org.apache.http.annotation.ThreadSafe;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.scheme.LayeredSchemeSocketFactory;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.*;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


@ThreadSafe
public class HackedSSLSocketFactory implements LayeredSchemeSocketFactory, LayeredSocketFactory {


    public static final String TLS = "TLS";
    public static final String SSL = "SSL";
    public static final String SSLV2 = "SSLv2";
    public static final X509HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER
            = new AllowAllHostnameVerifier();
    public static final X509HostnameVerifier BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
            = new BrowserCompatHostnameVerifier();
    public static final X509HostnameVerifier STRICT_HOSTNAME_VERIFIER
            = new StrictHostnameVerifier();
    private final SSLSocketFactory socketfactory;
    private final HostNameResolver nameResolver;
    // TODO: make final
    private volatile X509HostnameVerifier hostnameVerifier;

    /**
     * @deprecated Use {@link #HackedSSLSocketFactory(String, java.security.KeyStore, String, java.security.KeyStore, java.security.SecureRandom, org.apache.http.conn.ssl.X509HostnameVerifier)}
     */
    @Deprecated
    public HackedSSLSocketFactory(
            final String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final HostNameResolver nameResolver)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(createSSLContext(
                algorithm, keystore, keystorePassword, truststore, random, null),
                nameResolver);
    }

    /**
     * @since 4.1
     */
    public HackedSSLSocketFactory(
            String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final X509HostnameVerifier hostnameVerifier)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(createSSLContext(
                algorithm, keystore, keystorePassword, truststore, random, null),
                hostnameVerifier);
    }

    /**
     * @since 4.1
     */
    public HackedSSLSocketFactory(
            String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final TrustStrategy trustStrategy,
            final X509HostnameVerifier hostnameVerifier)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(createSSLContext(
                algorithm, keystore, keystorePassword, truststore, random, trustStrategy),
                hostnameVerifier);
    }

    public HackedSSLSocketFactory(
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, keystore, keystorePassword, truststore, null, null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    public HackedSSLSocketFactory(
            final KeyStore keystore,
            final String keystorePassword)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, keystore, keystorePassword, null, null, null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    public HackedSSLSocketFactory(
            final KeyStore truststore)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, null, null, truststore, null, null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    /**
     * @since 4.1
     */
    public HackedSSLSocketFactory(
            final TrustStrategy trustStrategy,
            final X509HostnameVerifier hostnameVerifier)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, null, null, null, null, trustStrategy, hostnameVerifier);
    }

    /**
     * @since 4.1
     */
    public HackedSSLSocketFactory(
            final TrustStrategy trustStrategy)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        this(TLS, null, null, null, null, trustStrategy, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    public HackedSSLSocketFactory(final SSLContext sslContext) {
        this(sslContext, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    /**
     * @deprecated Use {@link #HackedSSLSocketFactory(javax.net.ssl.SSLContext)}
     */
    @Deprecated
    public HackedSSLSocketFactory(
            final SSLContext sslContext, final HostNameResolver nameResolver) {
        super();
        this.socketfactory = sslContext.getSocketFactory();
        this.hostnameVerifier = BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        this.nameResolver = nameResolver;
    }

    /**
     * @since 4.1
     */
    public HackedSSLSocketFactory(
            final SSLContext sslContext, final X509HostnameVerifier hostnameVerifier) {
        super();
        this.socketfactory = sslContext.getSocketFactory();
        this.hostnameVerifier = hostnameVerifier;
        this.nameResolver = null;
    }

    private HackedSSLSocketFactory() {
        this(createDefaultSSLContext());
    }

    public static HackedSSLSocketFactory newInstance() throws NoSuchAlgorithmException, KeyManagementException {


        SSLContext ctx = SSLContext.getInstance("TLS");
        X509TrustManager tm = new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        X509HostnameVerifier verifier = new X509HostnameVerifier() {

            @Override
            public void verify(String string, SSLSocket ssls) throws IOException {
            }

            @Override
            public void verify(String string, X509Certificate xc) throws SSLException {
            }

            @Override
            public void verify(String string, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String string, SSLSession ssls) {
                return true;
            }
        };

        ctx.init(null, new TrustManager[]{tm}, null);
        HackedSSLSocketFactory ssf = new HackedSSLSocketFactory(ctx);
        ssf.setHostnameVerifier(verifier);

        return ssf;
    }

    private static SSLContext createSSLContext(
            String algorithm,
            final KeyStore keystore,
            final String keystorePassword,
            final KeyStore truststore,
            final SecureRandom random,
            final TrustStrategy trustStrategy)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        if (algorithm == null) {
            algorithm = TLS;
        }
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, keystorePassword != null ? keystorePassword.toCharArray() : null);
        KeyManager[] keymanagers = kmfactory.getKeyManagers();
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(truststore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        if (trustmanagers != null && trustStrategy != null) {
            for (int i = 0; i < trustmanagers.length; i++) {
                TrustManager tm = trustmanagers[i];
                if (tm instanceof X509TrustManager) {
                    trustmanagers[i] = new TrustManagerDecorator(
                            (X509TrustManager) tm, trustStrategy);
                }
            }
        }

        SSLContext sslcontext = SSLContext.getInstance(algorithm);
        sslcontext.init(keymanagers, trustmanagers, random);
        return sslcontext;
    }

    private static SSLContext createDefaultSSLContext() {
        try {
            return createSSLContext(TLS, null, null, null, null, null);
        } catch (Exception ex) {
            throw new IllegalStateException("Failure initializing default SSL context", ex);
        }
    }

    /**
     * @param params Optional parameters. Parameters passed to this method will have no effect.
     *               This method will create a unconnected instance of {@link java.net.Socket} class.
     * @since 4.1
     */
    public Socket createSocket(final HttpParams params) throws IOException {
        return new Socket();
    }

    @Deprecated
    public Socket createSocket() throws IOException {
        return new Socket();
    }

    /**
     * @since 4.1
     */
    public Socket connectSocket(
            final Socket socket,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Remote address may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        Socket sock = socket != null ? socket : new Socket();
        if (localAddress != null) {
            sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
            sock.bind(localAddress);
        }

        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        try {
            sock.connect(remoteAddress, connTimeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress.getHostName() + "/"
                    + remoteAddress.getAddress() + " timed out");
        }
        sock.setSoTimeout(soTimeout);
        SSLSocket sslsock;
        if (sock instanceof SSLSocket) {
            sslsock = (SSLSocket) sock;
        } else {
            sslsock = (SSLSocket) this.socketfactory.createSocket(
                    sock, remoteAddress.getHostName(), remoteAddress.getPort(), true);
        }
        setCiphers(sslsock);
        return sslsock;
    }

    /**
     * Checks whether a socket connection is secure.
     * This factory creates TLS/SSL socket connections
     * which, by default, are considered secure.
     * <br/>
     * Derived classes may override this method to perform
     * runtime checks, for example based on the cypher suite.
     *
     * @param sock the connected socket
     * @return <code>true</code>
     * @throws IllegalArgumentException if the argument is invalid
     */
    public boolean isSecure(final Socket sock) throws IllegalArgumentException {
        if (sock == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        // This instanceof check is in line with createSocket() above.
        if (!(sock instanceof SSLSocket)) {
            throw new IllegalArgumentException("Socket not created by this factory");
        }
        // This check is performed last since it calls the argument object.
        if (sock.isClosed()) {
            throw new IllegalArgumentException("Socket is closed");
        }
        return true;
    }

    /**
     * @since 4.1
     */
    public Socket createLayeredSocket(
            final Socket socket,
            final String host,
            final int port,
            final boolean autoClose) throws IOException, UnknownHostException {
        SSLSocket sslSocket = (SSLSocket) this.socketfactory.createSocket(
                socket,
                host,
                port,
                autoClose
        );
        setCiphers(sslSocket);
        return sslSocket;
    }

    public X509HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    @Deprecated
    public void setHostnameVerifier(X509HostnameVerifier hostnameVerifier) {
        if (hostnameVerifier == null) {
            throw new IllegalArgumentException("Hostname verifier may not be null");
        }
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * @deprecated Use {@link #connectSocket(java.net.Socket, java.net.InetSocketAddress, java.net.InetSocketAddress, org.apache.http.params.HttpParams)}
     */
    @Deprecated
    public Socket connectSocket(
            final Socket socket,
            final String host, int port,
            final InetAddress localAddress, int localPort,
            final HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        InetSocketAddress local = null;
        if (localAddress != null || localPort > 0) {
            // we need to bind explicitly
            if (localPort < 0) {
                localPort = 0; // indicates "any"
            }
            local = new InetSocketAddress(localAddress, localPort);
        }
        InetAddress remoteAddress;
        if (this.nameResolver != null) {
            remoteAddress = this.nameResolver.resolve(host);
        } else {
            remoteAddress = InetAddress.getByName(host);
        }
        InetSocketAddress remote = new InetSocketAddress(remoteAddress, port);
        return connectSocket(socket, remote, local, params);
    }

    /**
     * @deprecated Use {@link #createLayeredSocket(java.net.Socket, String, int, boolean)}
     */
    @Deprecated
    public Socket createSocket(
            final Socket socket,
            final String host, int port,
            boolean autoClose) throws IOException, UnknownHostException {
        return createLayeredSocket(socket, host, port, autoClose);
    }

    private void setCiphers(SSLSocket newSocket) {
        newSocket.setEnabledCipherSuites(new String[]{
                "SSL_RSA_WITH_RC4_128_MD5",
                "SSL_RSA_WITH_RC4_128_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
//                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
//                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
//                "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
//                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                "SSL_RSA_WITH_DES_CBC_SHA",
//                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
//                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
//                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
//                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"});
    }

}
