package kontrol.digitalocean

import kontrol.api.Infrastructure
import kontrol.api.Topology
import kontrol.digitalocean.DigitalOceanTopology
import kontrol.api.MachineGroup
import kontrol.digitalocean.DigitalOceanClientFactory

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DigitalOceanCloud(val client: DigitalOceanClientFactory, val members: Map<String, MachineGroup>) : Infrastructure{


    override fun topology(): Topology {
        return  DigitalOceanTopology(members);
    }

    public fun toString(): String {
        return topology().toString();
    }

}