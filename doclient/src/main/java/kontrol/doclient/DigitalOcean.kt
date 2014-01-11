package kontrol.doclient

import kontrol.doclient.AccessDeniedException
import kontrol.doclient.RequestUnsuccessfulException
import kontrol.doclient.ResourceNotFoundException
import kontrol.doclient.*

/* Original  MIT License from  Java version: https://github.com/jeevatkm/digitalocean-api-java
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
 * <strong>DigitalOcean API client written in Kotlin based on https://github.com/jeevatkm/digitalocean-api-java </strong>
 *
 * <p>
 * A simple and meaningful wrapper methods for <a href="https://api.digitalocean.com/"title="DigitalOcean's API">DigitalOcean's API</a>.
 * All of the RESTful that you find in DigitalOcean API's will be made available via simple Kotlin methods.
 * </p>
 *
 * <p>
 *
 * @author Neil Ellis (neiL@cazcade.com)  - Derived Work
 * @author Jeevanandam M. (jeeva@myjeeva.com)
 */
public trait DigitalOcean {
    /*
	 * Droplet manipulation methods
	 */
    /**
     * Method returns all active droplets that are currently running in your
     * account. All available API information is presented for each droplet.
     *
     * @return List&lt;Droplet>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun getAvailableDroplets(): List<Droplet>
    /**
     * <p>
     * Method allows you to create a new droplet. See the required parameters
     * section below for an explanation of the variables that are needed to
     * create a new droplet.
     * </p>
     *
     * <p>
     * Create a instance of {@link kontrol.doclient.Droplet} object and populate following values
     * </p>
     * <ul>
     * <li>Name Required, String, this is the name of the droplet must be
     * formatted by hostname rules</li>
     * <li>Side Id Required, Numeric, this is the id of the size you would like
     * the droplet created at</li>
     * <li>Image Id Required, Numeric, this is the id of the image you would
     * like the droplet created with</li>
     * <li>Region Id Required, Numeric, this is the id of the region you would
     * like your server in</li>
     * </ul>
     *
     * @param droplet
     *            the id of the droplet
     * @return {@link kontrol.doclient.Droplet}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun createDroplet(droplet: Droplet): Droplet
    /**
     * <p>
     * Method allows you to create a new droplet. See the required parameters
     * section below for an explanation of the variables that are needed to
     * create a new droplet.
     * </p>
     *
     * <p>
     * Create a instance of {@link kontrol.doclient.Droplet} object and populate following values
     * </p>
     * <ul>
     * <li>Name Required, String, this is the name of the droplet must be
     * formatted by hostname rules</li>
     * <li>Side Id Required, Numeric, this is the id of the size you would like
     * the droplet created at</li>
     * <li>Image Id Required, Numeric, this is the id of the image you would
     * like the droplet created with</li>
     * <li>Region Id Required, Numeric, this is the id of the region you would
     * like your server in</li>
     * </ul>
     *
     * @param droplet
     *            the id of the droplet
     * @param sshKeyIds
     *            Numeric CSV, comma separated list of ssh_key_ids that you
     *            would like to be added to the server
     * @return {@link kontrol.doclient.Droplet}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun createDroplet(droplet: Droplet, sshKeyIds: String?): Droplet
    /**
     * Method returns full information for a specific droplet ID that is passed
     * in the URL.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Droplet}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun getDropletInfo(dropletId: Int): Droplet
    /**
     * Method allows you to reboot a droplet. This is the preferred method to
     * use if a server is not responding.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun rebootDroplet(dropletId: Int): Response
    /**
     * Method allows you to power cycle a droplet. This will turn off the
     * droplet and then turn it back on.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun powerCyleDroplet(dropletId: Int): Response
    /**
     * Method allows you to shutdown a running droplet. The droplet will remain
     * in your account.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun shutdownDroplet(dropletId: Int): Response
    /**
     * Method allows you to poweroff a running droplet. The droplet will remain
     * in your account.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun powerOffDroplet(dropletId: Int): Response
    /**
     * Method allows you to poweron a powered off droplet.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun powerOnDroplet(dropletId: Int): Response
    /**
     * Method will reset the root password for a droplet. Please be aware that
     * this will reboot the droplet to allow resetting the password.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun resetDropletPassword(dropletId: Int): Response
    /**
     * Method allows you to resize a specific droplet to a different size. This
     * will affect the number of processors and memory allocated to the droplet.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun resizeDroplet(dropletId: Int, sizeId: Int): Response
    /**
     * Method allows you to take a snapshot of the running droplet, which can
     * later be restored or used to create a new droplet from the same image.
     * Please be aware this may cause a reboot.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun takeDropletSnapshot(dropletId: Int): Response
    /**
     * Method allows you to take a snapshot of the running droplet, which can
     * later be restored or used to create a new droplet from the same image.
     * Please be aware this may cause a reboot.
     *
     * @param dropletId
     *            the id of the droplet
     * @param snapshotName
     *            the name the snapshot to be created
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun takeDropletSnapshot(dropletId: Int, snapshotName: String?): Response
    /**
     * Method allows you to restore a droplet with a previous image or snapshot.
     * This will be a mirror copy of the image or snapshot to your droplet. Be
     * sure you have backed up any necessary information prior to restore.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun restoreDroplet(dropletId: Int, imageId: Int): Response
    /**
     * Method allows you to reinstall a droplet with a default image. This is
     * useful if you want to start again but retain the same IP address for your
     * droplet.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun rebuildDroplet(dropletId: Int, imageId: Int): Response
    /**
     * Method enables automatic backups which run in the background daily to
     * backup your droplet's data.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun enableDropletBackups(dropletId: Int): Response
    /**
     * Method disables automatic backups from running to backup your droplet's
     * data.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun disableDropletBackups(dropletId: Int): Response
    /**
     * Method renames the droplet to the specified name.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun renameDroplet(dropletId: Int, name: String): Response
    /**
     * Method destroys one of your droplets this is irreversible.
     *
     * @param dropletId
     *            the id of the droplet
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun deleteDroplet(dropletId: Int): Response
    /*
	 * Regions (aka Data Centers) methods
	 */
    /**
     * Method will return all the available regions within the DigitalOcean
     * cloud.
     *
     * @return List&ltRegion>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun getAvailableRegions(): List<Region>
    /*
	 * Images manipulation (aka Distribution) methods
	 */
    /**
     * Method returns all the available images that can be accessed by your
     * client ID. You will have access to all public images by default, and any
     * snapshots or backups that you have created in your own account.
     *
     * @return List&ltDropletImage>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun getAvailableImages(): List<DropletImage>
    /**
     * Method retrieves the attributes of an image.
     *
     * @param imageId
     *            the image Id of the droplet/snapshot/backup images
     * @return {@link kontrol.doclient.DropletImage}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun getImageInfo(imageId: Int): DropletImage
    /**
     * Method allows you to deletes an image. There is no way to restore a
     * deleted image so be careful and ensure your data is properly backed up.
     *
     * @param imageId
     *            the image Id of the droplet/snapshot/backup images
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun deleteImage(imageId: Int): Response
    /**
     * Method allows you to transfer an image to a specified region.
     *
     * @param imageId
     *            the image Id of the droplet/snapshot/backup images
     * @param regionId
     *            the region Id of the digitalocean data centers
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun transferImage(imageId: Int, regionId: Int): Response
    /*
	 * SSH Key manipulation methods
	 */
    /**
     * Method lists all the available public SSH keys in your account that can
     * be added to a droplet.
     *
     * @return <code>List&lt;SshKey></code>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.2
     */
    fun getAvailableSshKeys(): List<SshKey>
    /**
     * Method shows a specific public SSH key in your account that can be added
     * to a droplet.
     *
     * @param sshKeyId
     *            the SSH key Id
     * @return {@link kontrol.doclient.SshKey}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.2
     */
    fun getSshKeyInfo(sshKeyId: Int): SshKey
    /**
     * Method allows you to add a new public SSH key to your account
     *
     * @param sshKeyName
     *            the name you want to give this SSH key
     * @param sshPublicKey
     *            the actual public SSH key
     * @return {@link kontrol.doclient.SshKey}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.2
     */
    fun addSshKey(keyName: String, publicKey: String): SshKey
    /**
     * Method allows you to modify an existing public SSH key in your account.
     *
     * @param sshKeyId
     *            the SSH key Id, you would like to edit
     * @param newSshPublicKey
     *            the new public SSH key
     * @return {@link kontrol.doclient.SshKey}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.2
     */
    fun editSshKey(sshKeyId: Int, newKey: String): SshKey
    /**
     * Method will delete the SSH key from your account.
     *
     * @param sshKeyId
     *            the SSH key Id, you would like to delete
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.RequestUnsuccessfulException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.AccessDeniedException
     *
     * @since v1.2
     */
    fun deleteSshKey(sshKeyId: Int): Response
    /*
	 * Sizes (aka Available Droplet Plans) methods
	 */
    /**
     * Method returns all the available sizes that can be used to create a
     * droplet.
     *
     * @return List&lt;DropletSize>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.0
     */
    fun getAvailableSizes(): List<DropletSize>
    /*
	 * Domain manipulation methods
	 */
    /**
     * Method returns all of your available domains from DNS control panel
     *
     * @return <code>List&lt;Domain></code>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun getAvailableDomains(): List<Domain>
    /**
     * Method creates a new domain name with an A record for the specified
     * [ip_address].
     *
     * @param domainName
     *            the name of the domain
     * @param ipAddress
     *            the IP Address for the domain
     * @return {@link kontrol.doclient.Domain}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun createDomain(domainName: String, ipAddress: String): Domain
    /**
     * Method returns the specified domain attributes and zone file info.
     *
     * @param domainId
     *            the Id of the domain
     * @return {@link kontrol.doclient.Domain}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun getDomainInfo(domainId: Int): Domain
    /**
     * Method deletes the specified domain from DNS control panel
     *
     * @param domainId
     *            the Id of the domain
     * @return {@link kontrol.doclient.Response}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun deleteDomain(domainId: Int): Response
    /**
     * Method returns all of your current domain records from DNS control panel
     * for given domain.
     *
     * @param domainId
     *            the Id of the domain
     * @return <code>List&lt;DomainRecord></code>
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun getDomainRecords(domainId: Int): List<DomainRecord>
    /**
     * Method creates a new domain record name with an given domain record
     * values
     *
     * @param domainRecord
     *            the domain record values domain Id, record type, data, name,
     *            priority, port, weight
     * @return {@link kontrol.doclient.DomainRecord}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun createDomainRecord(domainRecord: DomainRecord): DomainRecord
    /**
     * Method returns the specified domain record.
     *
     * @param domainId
     *            the Id of the domain
     * @param recordId
     *            the record Id of the domain
     * @return {@link kontrol.doclient.DomainRecord}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun getDomainRecordInfo(domainId: Int, recordId: Int): DomainRecord
    /**
     * method edits an existing domain record of the given domain.
     *
     * @param domainRecord
     *            the domain record values domain Id, record type, data, name,
     *            priority, port, weight
     * @return {@link kontrol.doclient.DomainRecord}
     * @throws kontrol.doclient.AccessDeniedException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.RequestUnsuccessfulException
     *
     * @since v1.1
     */
    fun editDomainRecord(domainRecord: DomainRecord): DomainRecord
    /**
     * Method deletes the specified domain record from domain.
     *
     * @throws kontrol.doclient.RequestUnsuccessfulException
     * @throws kontrol.doclient.ResourceNotFoundException
     * @throws kontrol.doclient.AccessDeniedException
     *
     * @since v1.1
     */
    fun deleteDomainRecord(domainId: Int, recordId: Int): Response


}
