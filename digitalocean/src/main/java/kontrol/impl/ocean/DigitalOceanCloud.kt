package kontrol.impl

import kontrol.api.Infrastructure
import kontrol.api.Topology
import kontrol.impl.mock.DigitalOceanTopology
import kontrol.api.MachineGroup
import kontrol.impl.ocean.DigitalOceanClientFactory

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