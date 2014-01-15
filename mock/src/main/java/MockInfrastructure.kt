package kontrol.mock

import kontrol.api.Infrastructure
import kontrol.api.Topology
import kontrol.api.MachineGroup

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockInfrastructure(val members: MutableMap<String, MachineGroup>) : Infrastructure{
    val topology = MockTopology(members);


    override fun topology(): Topology {
        return topology;
    }
}