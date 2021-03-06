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
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
package kotlin.cfclient

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.conn.ssl.HackedSSLSocketFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.lang.reflect.Type
import org.apache.http.conn.scheme.Scheme
import org.apache.http.client.methods.HttpGet
import kontrol.cfclient.AccessDeniedException
import kontrol.cfclient.ResourceNotFoundException
import kontrol.cfclient.Zone
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.methods.HttpPost
import kontrol.common.readFully
import com.google.gson.JsonParser
import kontrol.cfclient.RequestUnsuccessfulException
import kontrol.cfclient.ZoneRecordResponse
import kontrol.cfclient.DomainRecord
import kontrol.cfclient.DomainRecordsResponse
import kontrol.cfclient.DomainRecordResponse

public class CloudFlareClient(public val email: String, public val apiKey: String) {

    enum class Method {
        GET POST PUT DELETE
    }

    private val LOG: Logger = LoggerFactory.getLogger(javaClass<CloudFlareClient>())!!

    val gson: Gson = Gson()

    public fun JsonElement.asClass(clazz: Class<*>): Any = gson.fromJson(this.toString(), clazz)!!
    public fun JsonElement.asType(`type`: Type): Any = gson.fromJson(this.toString(), `type`)!!

    /**
     * Http client
     */
    private var httpClient: DefaultHttpClient? = null
    /**
     * User's Client ID
     */
    /**
     * DigitalOcean API Host is <code>api.digitalocean.com</code>
     */
    private var base: String = "https://www.cloudflare.com/api_json.html"

    {
        val tmpClient = DefaultHttpClient()
        val ccm = tmpClient.getConnectionManager()
        ccm?.getSchemeRegistry()?.register(Scheme("https", HackedSSLSocketFactory.newInstance(), 443))
        this.httpClient = DefaultHttpClient(ccm, tmpClient.getParams())
    }


    public fun zones(): List<Zone> {
        return (execute(Method.GET, "zones", mapOf("a" to "zone_load_multi")) as ZoneRecordResponse).objs ?: listOf()
    }

    public fun domainRecords(zone: String): List<DomainRecord> {
        return (execute(Method.GET, "recs", mapOf("a" to "rec_load_all", "z" to zone)) as DomainRecordsResponse).objs ?: listOf()
    }

    public fun update(zone: String, record: DomainRecord): DomainRecord {
        return (execute(Method.GET, "rec", (record.toParams()).toMap(hashMapOf<String, Any?>("a" to "rec_edit", "z" to zone))) as DomainRecordResponse).obj!!
    }
    public fun create(zone: String, record: DomainRecord): DomainRecord {
        return (execute(Method.GET, "rec", (record.toParams()).toMap(hashMapOf<String, Any?>("a" to "rec_new", "z" to zone))) as DomainRecordResponse).obj!!
    }

    private fun execute(method: Method, subObject: String, params: Map<String, Any?>): Any? {
        var uriBuilder = URIBuilder(base);
        uriBuilder.addParameter("tkn", apiKey)
        uriBuilder.addParameter("email", email)
        params.entrySet() filter{ it.value != null } forEach { uriBuilder.setParameter(it.key, it.value.toString()) }
        val uri = uriBuilder.build()
        val req = when (method) {
            Method.GET -> {
                HttpGet(uri)
            }
            Method.POST -> {
                HttpPost(uri)
            }
            else -> null
        }
        LOG.debug("CloudFlare uri $uri")
        try {
            val httpResponse = httpClient?.execute(req)
            val result = when(httpResponse?.getStatusLine()?.getStatusCode()) {
                401 -> throw AccessDeniedException("Request failed to authenticate into the CloudFlare API successfully")
                404 -> throw ResourceNotFoundException("Requested resource is not available CloudFlare $uri")
                else -> httpResponse?.getEntity()?.getContent()?.readFully()?:""
            }
            val obj = JsonParser().parse(result)?.getAsJsonObject()!!
            //            println(obj)
            if (obj.get("result")?.getAsString()?.equalsIgnoreCase("success")?:false) {
                LOG.debug("JSON Respose Data: " + obj.toString())
                return obj.getAsJsonObject("response")?.getAsJsonObject(subObject)
            } else {
                println(obj)
                throw RequestUnsuccessfulException("Cloudflare API request unsuccessful, possible reason could be incorrect values [$uri].")
            }
        } finally {
            req?.releaseConnection()
        }
    }
}