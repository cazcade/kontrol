package kontrol.digitalocean

import kontrol.doclient.DigitalOcean
import kontrol.doclient.DigitalOceanClient


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public  class DigitalOceanClientFactory(val clientId: String, val apiKey: String) {

    fun instance(): DigitalOcean {
        return DigitalOceanClient(clientId, apiKey);
    }

}
