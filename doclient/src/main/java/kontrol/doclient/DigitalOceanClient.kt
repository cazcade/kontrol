package kontrol.doclient.impl

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kontrol.doclient.Constants.*
import kontrol.doclient.common.Action
import kontrol.doclient.common.Action.*
import kontrol.doclient.*
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.HackedSSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Type
import java.net.URI
import com.google.gson.JsonElement
import com.google.gson.Gson

/* The MIT License
 *
 * Copyright (c) 2010-2013 Jeevanandam M. (myjeeva.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE. 
 * 
 */
/**
 * DigitalOcean API client wrapper methods Implementation
 *
 * @author Jeevanandam M. (jeeva@myjeeva.com)
 */
public  class DigitalOceanClient(val clientId: String, val apiKey: String) : DigitalOcean, Constants {

    private val LOG: Logger = LoggerFactory.getLogger(javaClass<DigitalOceanClient>())!!

    val gson: Gson = Gson()

    public fun JsonElement.asClass(clazz: Class<*>): Any {
        return gson.fromJson(this.toString(), clazz)!!
    }
    public fun JsonElement.asType(`type`: Type): Any {
        return gson.fromJson(this.toString(), `type`)!!
    }

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
    private var apiHost: String = "api.digitalocean.com"

    public override fun getAvailableDroplets(): MutableList<Droplet> {
        return processByScope(AVAILABLE_DROPLETS, TYPE_DROPLET_LIST) as MutableList<Droplet>
    }

    public override fun createDroplet(droplet: Droplet): Droplet {
        return createDroplet(droplet, null)
    }

    public override fun createDroplet(droplet: Droplet, sshKeyIds: String?): Droplet {
        return processByScopeWithQueryParams(CREATE_DROPLET, javaClass<Droplet>(),
                hashMapOf(
                        P_NAME to droplet.name,
                        P_SIZE_ID to droplet.size_id,
                        P_IMAGE_ID to droplet.image_id,
                        P_REGION_ID to droplet.region_id,
                        P_SSH_KEY_IDS to sshKeyIds
                )
        ) as Droplet
    }
    public override fun getDropletInfo(dropletId: Int): Droplet {
        return (processByScope(GET_DROPLET_INFO, javaClass<Droplet>(), dropletId) as Droplet)
    }
    public override fun rebootDroplet(dropletId: Int): Response {
        return process(REBOOT_DROPLET, dropletId)
    }
    public override fun powerCyleDroplet(dropletId: Int): Response {
        return process(POWER_CYCLE_DROPLET, dropletId)
    }
    public override fun shutdownDroplet(dropletId: Int): Response {
        return process(SHUTDOWN_DROPLET, dropletId)
    }
    public override fun powerOffDroplet(dropletId: Int): Response {
        return process(POWER_OFF_DROPLET, dropletId)
    }
    public override fun powerOnDroplet(dropletId: Int): Response {
        return process(POWER_ON_DROPLET, dropletId)
    }
    public override fun resetDropletPassword(dropletId: Int): Response {
        return process(RESET_PASSWORD_DROPLET, dropletId)
    }
    public override fun resizeDroplet(dropletId: Int, sizeId: Int): Response {
        return processWithQueryParams(RESIZE_DROPLET, hashMapOf(P_SIZE_ID to sizeId), dropletId)
    }
    public override fun takeDropletSnapshot(dropletId: Int): Response {
        return takeDropletSnapshot(dropletId, null)
    }
    public override fun takeDropletSnapshot(dropletId: Int, name: String?): Response {
        return when (name) {
            null -> process(TAKE_DROPLET_SNAPSHOT, dropletId)
            else -> processWithQueryParams(TAKE_DROPLET_SNAPSHOT, hashMapOf(P_NAME to name), dropletId)
        }
    }
    public override fun restoreDroplet(dropletId: Int, imageId: Int): Response {
        return processWithQueryParams(RESTORE_DROPLET, hashMapOf(P_IMAGE_ID to imageId), dropletId)
    }
    public override fun rebuildDroplet(dropletId: Int, imageId: Int): Response {
        return processWithQueryParams(REBUILD_DROPLET, hashMapOf(P_IMAGE_ID to imageId), dropletId)
    }
    public override fun enableDropletBackups(dropletId: Int): Response {
        return process(ENABLE_AUTOMATIC_BACKUPS, dropletId)
    }
    public override fun disableDropletBackups(dropletId: Int): Response {
        return process(DISABLE_AUTOMATIC_BACKUPS, dropletId)
    }
    public override fun renameDroplet(dropletId: Int, name: String): Response {
        return processWithQueryParams(RENAME_DROPLET, hashMapOf(P_NAME to  name), dropletId)
    }
    public override fun deleteDroplet(dropletId: Int): Response {
        return process(DELETE_DROPLET, dropletId)
    }
    public override fun getAvailableRegions(): MutableList<Region> {
        return processByScope(AVAILABLE_REGIONS, TYPE_REGION_LIST) as MutableList<Region>
    }
    public override fun getAvailableImages(): MutableList<DropletImage> {
        return processByScope(AVAILABLE_IMAGES, TYPE_IMAGE_LIST) as MutableList<DropletImage>
    }
    public override fun getImageInfo(imageId: Int): DropletImage {
        return processByScope(GET_IMAGE_INFO, javaClass<DropletImage>(), imageId) as DropletImage
    }
    public override fun deleteImage(imageId: Int): Response {
        return process(DELETE_IMAGE, imageId)
    }
    public override fun transferImage(imageId: Int, regionId: Int): Response {
        return processWithQueryParams(TRANSFER_IMAGE, hashMapOf(P_REGION_ID to regionId), imageId)
    }
    public override fun getAvailableSshKeys(): MutableList<SshKey> {
        return processByScope(AVAILABLE_SSH_KEYS, TYPE_SSH_KEY_LIST) as MutableList<SshKey>
    }
    public override fun getSshKeyInfo(sshKeyId: Int): SshKey {
        return processByScope(GET_SSH_KEY, javaClass<SshKey>(), sshKeyId) as SshKey
    }
    public override fun addSshKey(name: String, publicKey: String): SshKey {
        return processByScope(CREATE_SSH_KEY, javaClass<SshKey>(), hashMapOf(P_NAME to name, P_PUBLIC_KEY to publicKey)) as SshKey
    }
    public override fun editSshKey(sshKeyId: Int, newKey: String): SshKey {
        return processByScope(EDIT_SSH_KEY, javaClass<SshKey>(), hashMapOf(P_PUBLIC_KEY to newKey), sshKeyId) as SshKey
    }
    public override fun deleteSshKey(sshKeyId: Int): Response {
        return process(DELETE_SSH_KEY, sshKeyId)
    }
    public override fun getAvailableSizes(): MutableList<DropletSize> {
        return processByScope(AVAILABLE_SIZES, TYPE_SIZE_LIST) as MutableList<DropletSize>
    }
    public override fun getAvailableDomains(): MutableList<Domain> {
        return processByScope(AVAILABLE_DOMAINS, TYPE_DOMAIN_LIST) as MutableList<Domain>
    }
    public override fun getDomainInfo(domainId: Int): Domain {
        return processByScope(GET_DOMAIN_INFO, javaClass<Domain>(), domainId) as Domain
    }
    public override fun createDomain(domainName: String, ipAddress: String): Domain {
        return processByScope(CREATE_DOMAIN, javaClass<Domain>(), hashMapOf(P_NAME to domainName,
                PARAM_IP_ADDRESS to ipAddress)) as Domain
    }
    public override fun deleteDomain(domainId: Int): Response {
        return process(DELETE_DOMAIN, domainId)
    }
    public override fun getDomainRecords(domainId: Int): MutableList<DomainRecord> {
        return processByScope(GET_DOMAIN_RECORDS, TYPE_DOMAIN_RECORD_LIST, domainId) as MutableList<DomainRecord>
    }
    public override fun getDomainRecordInfo(domainId: Int, recordId: Int): DomainRecord {
        return processByScope(GET_DOMAIN_RECORD_INFO, javaClass<DomainRecord>(), array(domainId, recordId)) as DomainRecord
    }
    public override fun createDomainRecord(domainRecord: DomainRecord): DomainRecord {
        return processByScopeWithQueryParams(CREATE_DOMAIN_RECORD, javaClass<DomainRecord>(), domainRecord.asParams(), domainRecord.domain_id) as DomainRecord
    }
    public override fun editDomainRecord(domainRecord: DomainRecord): DomainRecord {
        val params = array<Any>(domainRecord.domain_id?:-1, domainRecord.id?:-1)
        return (processByScopeWithQueryParams(EDIT_DOMAIN_RECORD, javaClass<DomainRecord>(), domainRecord.asParams(), params) as DomainRecord)
    }
    public override fun deleteDomainRecord(domainId: Int, recordId: Int): Response {
        return process(DELETE_DOMAIN_RECORD, array<Any>(domainId, recordId))
    }
    private fun performAction(action: Action, queryParams: Map<String, Any?>?, vararg pathParams: Any?): JsonObject {
        try {
            val uri = generateUri(action.mapPath, queryParams, *pathParams)
            val obj = JsonParser().parse(execute(uri))?.getAsJsonObject()!!
            if (obj.get(STATUS)?.getAsString()?.equalsIgnoreCase("OK")?:false) {
                LOG.debug("JSON Respose Data: " + obj.toString())
                return obj
            } else {
                throw RequestUnsuccessfulException("DigitalOcean API request unsuccessful, possible reason could be incorrect values [$uri].")
            }
        } catch (e: RequestUnsuccessfulException) {
            throw e
        } catch (e: Exception) {
            throw RequestUnsuccessfulException(e.getMessage(), e)
        }

    }
    private fun generateUri(path: String, queryParams: Map<String, Any?>?, vararg pathParams: Any?): URI {
        val ub = URIBuilder()
                .setScheme(HTTPS_SCHEME) ?.setHost(apiHost) ?.setPath(path.format(*pathParams))
        ?.setParameter(PARAM_CLIENT_ID, this.clientId) ?.setParameter(PARAM_API_KEY, this.apiKey)
        queryParams?.entrySet()?.forEach { if (it.value != null) ub?.setParameter(it.key, it.value.toString()) }
        return  ub?.build()!!

    }
    private fun execute(uri: URI): String {
        val httpGet = HttpGet(uri)
        LOG.debug("DigitalOcean API Endpoint URI: " + uri)
        try {
            val httpResponse = httpClient?.execute(httpGet)
            return  when(httpResponse?.getStatusLine()?.getStatusCode()) {
                401 -> throw AccessDeniedException("Request failed to authenticate into the DigitalOcean API successfully")
                404 -> throw ResourceNotFoundException("Requested resource is not available DigitalOcean $uri")
                else -> httpResponse?.getEntity()?.getContent()?.readFully()?:""
            }
        } finally {
            httpGet.releaseConnection()
        }
    }
    private fun process(action: Action, vararg pathParams: Any?): Response {
        return performAction(action, null, *pathParams).asClass(javaClass<Response>()) as Response
    }
    private fun processWithQueryParams(action: Action, queryParams: Map<String, Any?>?, vararg pathParams: Any?): Response {
        return performAction(action, queryParams, *pathParams).asClass(javaClass<Response>()) as Response
    }
    private fun processByScope(action: Action, clazz: Class<*>, vararg pathParams: Any?): Any? {
        return performAction(action, null, *pathParams)[action.element]?.asClass(clazz)
    }
    private fun processByScopeWithQueryParams(action: Action, clazz: Class<*>, queryParams: Map<String, Any?>?, vararg pathParams: Any?): Any? {
        return performAction(action, queryParams, *pathParams)[action.element]?.asClass(clazz)
    }
    private fun processByScope(action: Action, classType: Type, vararg pathParams: Any?): Any? {
        return performAction(action, null, *pathParams)[action.element]?.asType(classType)
    }

    {
        val base = DefaultHttpClient()
        val ccm = base.getConnectionManager()
        ccm?.getSchemeRegistry()?.register(Scheme("https", HackedSSLSocketFactory.newInstance(), 443))
        this.httpClient = DefaultHttpClient(ccm, base.getParams())
    }

}
