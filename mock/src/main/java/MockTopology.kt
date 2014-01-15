package kontrol.mock

import kontrol.api.Topology
import kontrol.api.MachineGroup

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockTopology(override val members: Map<String, MachineGroup>) : Topology{

}