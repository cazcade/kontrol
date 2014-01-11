package kontrol.doclient

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
        return hashMapOf(PARAM_RECORD_TYPE to this.record_type,
                PARAM_DATA to data,
                P_NAME to name,
                PARAM_PRIORITY to priority,
                PARAM_PORT to port,
                PARAM_WEIGHT to weight)
    }
}






