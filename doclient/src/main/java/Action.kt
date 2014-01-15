package kontrol.doclient


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

public enum class Action(public var mapPath: String, public var element: String?=null) {
    AVAILABLE_DROPLETS : Action("/droplets/", "droplets")
    CREATE_DROPLET : Action("/droplets/new/", "droplet")
    GET_DROPLET_INFO : Action("/droplets/%s/", "droplet")
    REBOOT_DROPLET : Action("/droplets/%s/reboot/")
    POWER_CYCLE_DROPLET : Action("/droplets/%s/power_cycle/")
    SHUTDOWN_DROPLET : Action("/droplets/%s/shutdown/")
    POWER_OFF_DROPLET : Action("/droplets/%s/power_off/")
    POWER_ON_DROPLET : Action("/droplets/%s/power_on/")
    RESET_PASSWORD_DROPLET : Action("/droplets/%s/password_reset/")
    RESIZE_DROPLET : Action("/droplets/%s/resize/", "")
    TAKE_DROPLET_SNAPSHOT : Action("/droplets/%s/snapshot/")
    RESTORE_DROPLET : Action("/droplets/%s/resize/")
    REBUILD_DROPLET : Action("/droplets/%s/rebuild/")
    ENABLE_AUTOMATIC_BACKUPS : Action("/droplets/%s/enable_backups/")
    DISABLE_AUTOMATIC_BACKUPS : Action("/droplets/%s/disable_backups/")
    RENAME_DROPLET : Action("/droplets/%s/rename/")
    DELETE_DROPLET : Action("/droplets/%s/destroy/")
    AVAILABLE_REGIONS : Action("/regions/", "regions")
    AVAILABLE_IMAGES : Action("/images/", "images")
    GET_IMAGE_INFO : Action("/images/%s/", "image")
    DELETE_IMAGE : Action("/images/%s/destroy/")
    TRANSFER_IMAGE : Action("/images/%s/transfer/")
    AVAILABLE_SSH_KEYS : Action("/ssh_keys/", "ssh_keys")
    CREATE_SSH_KEY : Action("/ssh_keys/new/", "ssh_key")
    GET_SSH_KEY : Action("/ssh_keys/%s/", "ssh_key")
    EDIT_SSH_KEY : Action("/ssh_keys/%s/edit/", "ssh_key")
    DELETE_SSH_KEY : Action("/ssh_keys/%s/destroy/")
    AVAILABLE_SIZES : Action("/sizes/", "sizes")
    AVAILABLE_DOMAINS : Action("/domains/", "domains")
    CREATE_DOMAIN : Action("/domains/new", "domain")
    GET_DOMAIN_INFO : Action("/domains/%s/", "domain")
    DELETE_DOMAIN : Action("/domains/%s/destroy/")
    GET_DOMAIN_RECORDS : Action("/domains/%s/records/", "records")
    CREATE_DOMAIN_RECORD : Action("/domains/%s/records/new/", "record")
    GET_DOMAIN_RECORD_INFO : Action("/domains/%s/records/%s/", "record")
    EDIT_DOMAIN_RECORD : Action("/domains/%s/records/%s/edit/", "record")
    DELETE_DOMAIN_RECORD : Action("/domains/%s/records/%s/destroy/")




}
