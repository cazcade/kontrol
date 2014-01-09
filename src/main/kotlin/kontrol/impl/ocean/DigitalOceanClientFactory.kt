package kontrol.impl.ocean

import com.myjeeva.digitalocean.DigitalOcean
import com.myjeeva.digitalocean.impl.DigitalOceanClient


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public  class DigitalOceanClientFactory(val clientId: String, val apiKey: String) {

    fun instance(): DigitalOcean {
        return DigitalOceanClient(clientId, apiKey);
    }

}
