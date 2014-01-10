package com.myjeeva.digitalocean.impl

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.myjeeva.digitalocean.Constants
import com.myjeeva.digitalocean.DigitalOcean
import com.myjeeva.digitalocean.Utils
import com.myjeeva.digitalocean.common.Action
import com.myjeeva.digitalocean.exception.AccessDeniedException
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException
import com.myjeeva.digitalocean.exception.ResourceNotFoundException
import com.myjeeva.digitalocean.pojo.*
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.HackedSSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.Type
import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap

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
        return (processByScope(Action.AVAILABLE_DROPLETS, Constants.TYPE_DROPLET_LIST) as MutableList<Droplet>)
    }
    public override fun createDroplet(droplet: Droplet): Droplet {
        return createDroplet(droplet, null)
    }
    public override fun createDroplet(droplet: Droplet, sshKeyIds: String?): Droplet {
        val qp: MutableMap<String, Any?> = hashMapOf(
                Constants.PARAM_NAME to (droplet.getName()?:"unknown") : Any,
                Constants.PARAM_SIDE_ID to droplet.getSizeId(),
                Constants.PARAM_IMAGE_ID to droplet.getImageId(),
                Constants.PARAM_REGION_ID to droplet.getRegionId()
        )
        if (null != sshKeyIds) {
            qp.put(Constants.PARAM_SSH_KEY_IDS, sshKeyIds)
        }

        return (processByScopeWithQueryParams(Action.CREATE_DROPLET, javaClass<Droplet>(), qp) as Droplet)
    }
    public override fun getDropletInfo(dropletId: Int): Droplet {
        return (processByScope(Action.GET_DROPLET_INFO, javaClass<Droplet>(), dropletId) as Droplet)
    }
    public override fun rebootDroplet(dropletId: Int): Response {
        return process(Action.REBOOT_DROPLET, dropletId)
    }
    public override fun powerCyleDroplet(dropletId: Int): Response {
        return process(Action.POWER_CYCLE_DROPLET, dropletId)
    }
    public override fun shutdownDroplet(dropletId: Int): Response {
        return process(Action.SHUTDOWN_DROPLET, dropletId)
    }
    public override fun powerOffDroplet(dropletId: Int): Response {
        return process(Action.POWER_OFF_DROPLET, dropletId)
    }
    public override fun powerOnDroplet(dropletId: Int): Response {
        return process(Action.POWER_ON_DROPLET, dropletId)
    }
    public override fun resetDropletPassword(dropletId: Int): Response {
        return process(Action.RESET_PASSWORD_DROPLET, dropletId)
    }
    public override fun resizeDroplet(dropletId: Int, sizeId: Int): Response {
        return processWithQueryParams(Action.RESIZE_DROPLET, hashMapOf(Constants.PARAM_SIDE_ID to sizeId), dropletId)
    }
    public override fun takeDropletSnapshot(dropletId: Int): Response {
        return takeDropletSnapshot(dropletId, null)
    }
    public override fun takeDropletSnapshot(dropletId: Int, snapshotName: String?): Response {
        var response: Response
        if (null == snapshotName)
        {
            response = process(Action.TAKE_DROPLET_SNAPSHOT, dropletId)
        }
        else
        {
            val qp = HashMap<String, String>()
            qp.put(Constants.PARAM_NAME, snapshotName)
            response = processWithQueryParams(Action.TAKE_DROPLET_SNAPSHOT, qp, dropletId)
        }
        return response
    }
    public override fun restoreDroplet(dropletId: Int, imageId: Int): Response {
        return processWithQueryParams(Action.RESTORE_DROPLET, hashMapOf(Constants.PARAM_IMAGE_ID to imageId), dropletId)
    }
    public override fun rebuildDroplet(dropletId: Int, imageId: Int): Response {
        return processWithQueryParams(Action.REBUILD_DROPLET, hashMapOf(Constants.PARAM_IMAGE_ID to imageId), dropletId)
    }
    public override fun enableDropletBackups(dropletId: Int): Response {
        return process(Action.ENABLE_AUTOMATIC_BACKUPS, dropletId)
    }
    public override fun disableDropletBackups(dropletId: Int): Response {
        return process(Action.DISABLE_AUTOMATIC_BACKUPS, dropletId)
    }
    public override fun renameDroplet(dropletId: Int, name: String): Response {
        return processWithQueryParams(Action.RENAME_DROPLET, hashMapOf(Constants.PARAM_NAME to  name), dropletId)
    }
    public override fun deleteDroplet(dropletId: Int): Response {
        return process(Action.DELETE_DROPLET, dropletId)
    }
    public override fun getAvailableRegions(): MutableList<Region> {
        return (processByScope(Action.AVAILABLE_REGIONS, Constants.TYPE_REGION_LIST) as MutableList<Region>)
    }
    public override fun getAvailableImages(): MutableList<DropletImage> {
        return (processByScope(Action.AVAILABLE_IMAGES, Constants.TYPE_IMAGE_LIST) as MutableList<DropletImage>)
    }
    public override fun getImageInfo(imageId: Int): DropletImage {
        return (processByScope(Action.GET_IMAGE_INFO, javaClass<DropletImage>(), imageId) as DropletImage)
    }
    public override fun deleteImage(imageId: Int): Response {
        return process(Action.DELETE_IMAGE, imageId)
    }
    public override fun transferImage(imageId: Int, regionId: Int): Response {
        return processWithQueryParams(Action.TRANSFER_IMAGE, hashMapOf(Constants.PARAM_REGION_ID to regionId), imageId)
    }
    public override fun getAvailableSshKeys(): MutableList<SshKey> {
        return (processByScope(Action.AVAILABLE_SSH_KEYS, Constants.TYPE_SSH_KEY_LIST) as MutableList<SshKey>)
    }
    public override fun getSshKeyInfo(sshKeyId: Int): SshKey {
        return (processByScope(Action.GET_SSH_KEY, javaClass<SshKey>(), sshKeyId) as SshKey)
    }
    public override fun addSshKey(sshKeyName: String, sshPublicKey: String): SshKey {
        val qp = HashMap<String, String>()
        qp.put(Constants.PARAM_NAME, sshKeyName)
        qp.put(Constants.PARAM_SSH_PUB_KEY, sshPublicKey)
        return (processByScope(Action.CREATE_SSH_KEY, javaClass<SshKey>(), qp) as SshKey)
    }
    public override fun editSshKey(sshKeyId: Int, newSshPublicKey: String): SshKey {
        return processByScope(Action.EDIT_SSH_KEY, javaClass<SshKey>(), hashMapOf(Constants.PARAM_SSH_PUB_KEY to newSshPublicKey), sshKeyId) as SshKey
    }
    public override fun deleteSshKey(sshKeyId: Int): Response {
        return process(Action.DELETE_SSH_KEY, sshKeyId)
    }
    public override fun getAvailableSizes(): MutableList<DropletSize> {
        return processByScope(Action.AVAILABLE_SIZES, Constants.TYPE_SIZE_LIST) as MutableList<DropletSize>
    }
    public override fun getAvailableDomains(): MutableList<Domain> {
        return processByScope(Action.AVAILABLE_DOMAINS, Constants.TYPE_DOMAIN_LIST) as MutableList<Domain>
    }
    public override fun getDomainInfo(domainId: Int): Domain {
        return processByScope(Action.GET_DOMAIN_INFO, javaClass<Domain>(), domainId) as Domain
    }
    public override fun createDomain(domainName: String, ipAddress: String): Domain {
        return (processByScope(Action.CREATE_DOMAIN, javaClass<Domain>(), hashMapOf(Constants.PARAM_NAME to domainName,
                Constants.PARAM_IP_ADDRESS to ipAddress)) as Domain)
    }
    public override fun deleteDomain(domainId: Int): Response {
        return process(Action.DELETE_DOMAIN, domainId)
    }
    public override fun getDomainRecords(domainId: Int): MutableList<DomainRecord> {
        return (processByScope(Action.GET_DOMAIN_RECORDS, Constants.TYPE_DOMAIN_RECORD_LIST, domainId) as MutableList<DomainRecord>)
    }
    public override fun getDomainRecordInfo(domainId: Int, recordId: Int): DomainRecord {
        val params = array<Any>(domainId, recordId)
        return (processByScope(Action.GET_DOMAIN_RECORD_INFO, javaClass<DomainRecord>(), params) as DomainRecord)
    }
    public override fun createDomainRecord(domainRecord: DomainRecord): DomainRecord {
        return processByScopeWithQueryParams(Action.CREATE_DOMAIN_RECORD, javaClass<DomainRecord>(), Utils.prepareDomainRecordParams(domainRecord), domainRecord.getDomainId()!!) as DomainRecord
    }
    public override fun editDomainRecord(domainRecord: DomainRecord): DomainRecord {
        val params = array<Any>(domainRecord.getDomainId()?:-1, domainRecord.getId()?:-1)
        return (processByScopeWithQueryParams(Action.EDIT_DOMAIN_RECORD, javaClass<DomainRecord>(), Utils.prepareDomainRecordParams(domainRecord), params) as DomainRecord)
    }
    public override fun deleteDomainRecord(domainId: Int, recordId: Int): Response {
        val params = array<Any>(domainId, recordId)
        return process(Action.DELETE_DOMAIN_RECORD, params)
    }
    private fun performAction(action: Action, queryParams: Map<String, Any?>?, vararg pathParams: Any?): JsonObject {
        val uri = generateUri(action.getMapPath()?:"", queryParams, *pathParams)
        var response: String
        try
        {
            response = execute(uri)
        }
        catch (cpe: ClientProtocolException) {
            throw RequestUnsuccessfulException(cpe.getMessage(), cpe)
        }
        catch (ioe: IOException) {
            throw RequestUnsuccessfulException(ioe.getMessage(), ioe)
        }

        val element = JsonParser().parse(response)
        val obj = element?.getAsJsonObject()!!
        val status = obj.get(Constants.STATUS)?.getAsString()
        LOG.info("DigitalOcean Response Status: " + status)
        if ("OK".equalsIgnoreCase(status?:""))
        {
            LOG.debug("JSON Respose Data: " + obj.toString())
            return obj
        }
        else
        {
            throw RequestUnsuccessfulException("DigitalOcean API request unsuccessful, possible reason could be incorrect values [" + uri + "].")
        }
    }
    private fun generateUri(path: String, queryParams: Map<String, Any?>?, vararg pathParams: Any?): URI {
        val ub = URIBuilder()
        ub.setScheme(Constants.HTTPS_SCHEME)
        ub.setHost(apiHost)
        ub.setPath(path.format(*pathParams))
        ub.setParameter(Constants.PARAM_CLIENT_ID, this.clientId)
        ub.setParameter(Constants.PARAM_API_KEY, this.apiKey)
        if (null != queryParams)
        {
            for (entry in queryParams.entrySet())
            {
                ub.setParameter(entry.getKey(), entry.getValue().toString())
            }
        }

        var uri: URI
        try
        {
            uri = ub.build() as URI
        }
        catch (use: URISyntaxException) {
            LOG.error(use.getMessage(), use)
            throw use
        }

        return uri
    }
    private fun execute(uri: URI): String {
        val httpGet = HttpGet(uri)
        LOG.debug("DigitalOcean API Endpoint URI: " + uri)
        var response = ""
        try
        {

            val client = httpClient
            if (client != null) {

                val httpResponse = client.execute(httpGet)!!
                if (401 == httpResponse.getStatusLine()?.getStatusCode()!!)
                {
                    throw AccessDeniedException("Request failed to authenticate into the DigitalOcean API successfully")
                }

                if (404 == httpResponse.getStatusLine()?.getStatusCode()!!)
                {
                    throw ResourceNotFoundException("Requested resource is not available DigitalOcean $uri")
                }

                val entity = httpResponse.getEntity()
                if (null != entity)
                {
                    val input = entity.getContent()
                    response = Utils.readInputStream(input)?:""
                    // Let's close the stream
                    try
                    {
                        if (null != input)
                        {
                            input.close()
                        }

                    }
                    catch (ioe: IOException) {
                        LOG.error("Error occured while reading HTTP input stream [" + ioe.getMessage() + "]")
                    }

                    LOG.debug("HTTP Response: " + response)
                }
            }


        }

        finally
        {
            httpGet.releaseConnection()
        }
        return response
    }
    private fun process(action: Action, vararg id: Any): Response {
        return processWithQueryParams(action, null, id)
    }
    private fun processWithQueryParams(action: Action, queryParams: Map<String, Any?>?, vararg id: Any): Response {
        return (Utils.byClass(performAction(action, queryParams, id), javaClass<Response>()) as Response)
    }
    private fun processByScope(action: Action, clazz: Class<*>, vararg pathParams: Any?): Any {
        return processByScopeWithQueryParams(action, clazz, null, *pathParams)
    }
    private fun processByScopeWithQueryParams(action: Action, clazz: Class<*>, queryParams: Map<String, Any?>?, vararg pathParams: Any?): Any {
        return Utils.byClass(performAction(action, queryParams, *pathParams).get(action.getElementName()), clazz)!!
    }
    private fun processByScope(action: Action, `type`: Type, vararg pathParams: Any?): Any {
        return Utils.byType(performAction(action, null, *pathParams).get(action.getElementName()), `type`)!!
    }

    {
        var ssf: HackedSSLSocketFactory? = null
        try
        {
            ssf = HackedSSLSocketFactory.newInstance()
        }
        catch (e: Exception) {
            throw RuntimeException(e)
        }

        val base = DefaultHttpClient()
        val ccm = base.getConnectionManager()
        val sr = ccm?.getSchemeRegistry()
        sr?.register(Scheme("https", ssf, 443))
        this.httpClient = DefaultHttpClient(ccm, base.getParams())
    }

}
