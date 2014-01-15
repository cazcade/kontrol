package kontrol.mock

import kontrol.api.Machine
import kontrol.common.DefaultStateMachine
import kontrol.api.StateMachine
import kontrol.api.Monitor
import kontrol.api.MachineState
import kontrol.mock.MockMachineMonitor
import kontrol.api.sensor.SensorValue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockMachine(val ip: String) : Machine{
    override var disableAction: ((Machine) -> Unit)? = null
    override var enableAction: ((Machine) -> Unit)? = null
    override var data: ConcurrentMap<String, SensorValue<Any?>> = ConcurrentHashMap();

    override var monitor: Monitor<MachineState, Machine> = MockMachineMonitor();
    override val stateMachine: StateMachine<MachineState, Machine> = DefaultStateMachine<MachineState, Machine>(this);
    override var enabled: Boolean= true;

    override fun ip(): String {
        return ip;
    }


}