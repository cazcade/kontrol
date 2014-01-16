/*
 * Kotlin version Copyright (c) 2014 Cazcade Limited (http://cazcade.com)
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
 *
 *
 *
 * Original Java codes was MIT License  https://github.com/jeevatkm/digitalocean-api-java
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
 */

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


// HTTPS Scheme
val HTTPS_SCHEME: String = "https"

// HTTP Param name
val PARAM_CLIENT_ID: String = "client_id"
val PARAM_API_KEY: String = "api_key"
val P_NAME: String = "name"
val P_SIZE_ID: String = "size_id"
val P_REGION_ID: String = "region_id"
val P_IMAGE_ID: String = "image_id"
val P_SSH_KEY_IDS: String = "ssh_key_ids"
val PARAM_IP_ADDRESS: String = "ip_address"
val PARAM_RECORD_TYPE: String = "record_type"
val PARAM_DATA: String = "data"
val PARAM_PRIORITY: String = "priority"
val PARAM_PORT: String = "port"
val PARAM_WEIGHT: String = "weight"
val P_PUBLIC_KEY: String = "ssh_pub_key"
val P_PRIVATE_NETWORKING: String = "private_networking"
val P_BACKUPS_ENABLED: String = "backups_enabled"

// JSON Element Name
val STATUS: String = "status"


public  data class Droplet(
        public var id: Int? = null,
        public var name: String? = null,
        public var image_id: Int? = null,
        public var region_id: Int? = null,
        public var size_id: Int? = null,
        public var backups_active: String? = null,
        public var status: String? = null,
        public var event_id: Long? = null,
        public var ip_address: String? = null,
        public var private_ip_address: String? = null,
        public var locked: Boolean? = null
){
}


public data class DropletImage(
        public var id: Int? = null,
        public var name: String? = null,
        public var slug: String? = null,
        public var distribution: String? = null)
{
}


public data class DropletSize(
        public var id: Int? = null,
        public var name: String? = null,
        public var slug: String? = null) {

}

public data class Region(
        public var id: Int? = null,
        public var name: String? = null,
        public var slug: String? = null
) {
}

public data class Response(
        public var status: String? = null,
        public var event_id: Long? = null
){
}

public data class SshKey(
        public var id: Int? = null,
        public var name: String? = null,
        public var ssh_pub_key: String? = null
) {
}


public data class Domain(
        public var id: Int? = null,
        public var name: String? = null,
        public var ttl: Int? = null,
        public var live_zone_file: String? = null,
        public var error: String? = null,
        public var zone_file_with_error: String? = null
)  {
}

public data class DomainRecord(
        public var id: Int? = null,
        public var domain_id: Int? = null,
        public var record_type: String? = null,
        public var data: String? = null,
        public var name: String? = null,
        public var priority: String? = null,
        public var port: Int? = null,
        public var weight: Int? = null
) {
    public fun asParams(): Map<String, Any?> {
        return hashMapOf(
                PARAM_RECORD_TYPE to record_type,
                PARAM_DATA to data,
                P_NAME to name,
                PARAM_PRIORITY to priority,
                PARAM_PORT to port,
                PARAM_WEIGHT to weight
        )
    }
}






