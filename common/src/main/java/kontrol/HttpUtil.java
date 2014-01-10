package kontrol;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public class HttpUtil {

    private static final int HTTP_TIMEOUT = 360 * 1000;
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    public static boolean exists(String url) {
        try {
            return getCookielessHttpClient(1000).execute(new HttpHead(new URI(url)), new BasicHttpContext()).getStatusLine().getStatusCode() < 400;
        } catch (URISyntaxException e) {
            log.warn(e.getMessage());
            return false;
        } catch (ClientProtocolException e) {
            log.warn(e.getMessage());
            return false;
        } catch (IOException e) {
            log.warn(e.getMessage());
            return false;
        }
    }




    public static HttpClient getHttpClient() {
        return new DefaultHttpClient();
    }



    public static InputStream getUrlAsStream(URL url, int timeout) throws IOException {
        return getUrlAsStream(URI.create(url.toString()), timeout, Locale.US);
    }

    public static InputStream getUrlAsStream(URI url, int timeout, Locale locale) throws IOException {
        log.debug("Trying to get URL " + url);
        HttpGet httpget = createHttpGet(url, locale);
        HttpContext context = new BasicHttpContext();
        HttpClient httpClient = getCookielessHttpClient(timeout);
        HttpResponse httpResponse = httpClient.execute(httpget, context);
        if (httpResponse.getStatusLine().getStatusCode() < 300) {
            log.debug("Got it, size was {} status was {}", httpResponse.getEntity().getContentLength(), httpResponse.getStatusLine().getStatusCode());
            return httpResponse.getEntity().getContent();
        } else {
            throw new IOException("HTTP Error " + httpResponse.getStatusLine().getStatusCode() + " with reason '" + httpResponse.getStatusLine().getReasonPhrase() + "'");
        }

    }

    public static InputStream getUrlAsStream(URL url, int timeout, Locale locale) throws IOException {
        return getUrlAsStream(URI.create(url.toString()), timeout, locale);
    }

    public static InputStream getUrlAsStream(URL url, Locale locale) throws IOException {
        return getUrlAsStream(URI.create(url.toString()), HTTP_TIMEOUT, locale);
    }

    public static InputStream getUrlAsStream(URL url) throws IOException {
        return getUrlAsStream(URI.create(url.toString()), HTTP_TIMEOUT, Locale.US);
    }

    public static String getUrlAsString(URI url, int timeout) throws IOException {
        return getUrlAsString(url, timeout, Locale.US);
    }

    public static String getUrlAsString(URI url, int timeout, Locale locale) throws IOException {

        HttpGet httpget = createHttpGet(url, locale);
        HttpContext context = new BasicHttpContext();
        HttpClient httpClient = getCookielessHttpClient(timeout);
        HttpResponse httpResponse = httpClient.execute(httpget, context);


        return IOUtils.toString(httpResponse.getEntity().getContent());


    }

    public static HttpClient getCookielessHttpClient(int timeout) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
                CookiePolicy.IGNORE_COOKIES);
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);

        return httpClient;
    }

    public static HttpGet createHttpGet(URI url, Locale locale) {
        HttpGet request = new HttpGet(url);
        request.addHeader("Accept-Language", locale.toLanguageTag());
        return request;
    }

    public static int getStatus(URI url, Locale locale, int timeout) throws IOException {
        HttpGet request = new HttpGet(url);
        request.addHeader("Accept-Language", locale.toLanguageTag());
        return getCookielessHttpClient(timeout).execute(request).getStatusLine().getStatusCode();
    }

    public static String getUrlAsString(URL url, int timeout, Locale locale) throws IOException {
        return getUrlAsString(URI.create(url.toString()), timeout, locale);
    }

    public static String getUrlAsString(URL url, int timeout) throws IOException {
        return getUrlAsString(URI.create(url.toString()), timeout, Locale.US);
    }

    public static String getUrlAsString(URL url, Locale locale) throws IOException {
        return getUrlAsString(URI.create(url.toString()), HTTP_TIMEOUT, locale);
    }
}
