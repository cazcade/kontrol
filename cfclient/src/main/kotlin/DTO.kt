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
package kontrol.cfclient

import java.util.ArrayList
import java.util.HashMap


public trait Response<T> {
    public var result: String?
    public var msg: String?
}

public trait ListResponse<T> : Response<T> {
    public var has_more: Boolean?
    public var count: Int?
    public override var result: String?
    public override var msg: String?
    public var objs: MutableList<T>?
}

public trait SingleResponse<T> : Response<T> {
    public override var result: String?
    public override var msg: String?
    public var obj: T?
}

public data class ZoneRecordResponse(public override var has_more: Boolean? = null
                                     , public override var count: Int? = null
                                     , public override var result: String? = null
                                     , public override var msg: String? = null
                                     , public override var objs: MutableList<Zone>? = null) : ListResponse<Zone>{

}

public data class DomainRecordsResponse(public override var has_more: Boolean? = null
                                        , public override var count: Int? = null
                                        , public override var result: String? = null
                                        , public override var msg: String? = null
                                        , public override var objs: MutableList<DomainRecord>? = null) : ListResponse<DomainRecord> {

}

public data class DomainRecordResponse(public override var result: String? = null
                                       , public override var msg: String? = null
                                       , public override var obj: DomainRecord? = null) : SingleResponse<DomainRecord>{

}

public data class Zone(
        public var zone_id: String? = null
        , public var user_id: String? = null
        , public var zone_name: String? = null
        , public var display_name: String? = null
        , public var zone_status: String? = null
        , public var zone_mode: String? = null
        , public var host_id: String? = null
        , public var zone_type: String? = null
        , public var host_pubname: String? = null
        , public var host_website: String? = null
        , public var vtxt: Any? = null
        , public var fqdns: Any? = null
        , public var step: String? = null
        , public var zone_status_class: String? = null
        , public var zone_status_desc: String? = null
        , public var ns_vanity_map: MutableList<Any>? = ArrayList<Any>()
        , public var orig_registrar: Any? = null
        , public var orig_dnshost: Any? = null
        , public var orig_ns_names: Any? = null
        , public var confirm_code: Confirm_code? = null
        , public var allow: MutableList<String>? = ArrayList<String>()
        , public var props: MutableMap<String, Any>? = HashMap()

) {

}

public data class DomainRecord(
        public  var rec_id: String? = null
        , public  var rec_tag: String? = null
        , public  var zone_name: String? = null
        , public  var name: String? = null
        , public  var display_name: String? = null
        , public  var `type`: String? = null
        , public  var prio: Any? = null
        , public  var content: String? = null
        , public  var display_content: String? = null
        , public  var ttl: String? = null
        , public  var ttl_ceil: Int? = null
        , public  var ssl_id: Any? = null
        , public  var ssl_status: Any? = null
        , public  var ssl_expires_on: Any? = null
        , public  var auto_ttl: Int? = null
        , public  var service_mode: String? = null
        , public var props: MutableMap<String, Any>? = HashMap()

){

    public fun toParams(): MutableMap<String, Any?> {
        return hashMapOf("id" to rec_id, "name" to name, "display_name" to display_name, "type" to `type`, "content" to content, "display_content" to display_content, "ttl" to ttl, "ttl_ceil" to ttl_ceil, "service_mode" to service_mode, "prio" to prio)
    }
}

public data class Confirm_code(
        public var zone_deactivate: String? = null,
        public var zone_dev_mode1: String? = null

) {
}


