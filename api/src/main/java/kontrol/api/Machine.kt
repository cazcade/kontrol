package kontrol.api

import java.util.concurrent.ConcurrentMap
import kontrol.api.sensor.SensorValue


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Machine : HasStateMachine<MachineState, Machine> {

    var data: ConcurrentMap<String, SensorValue<Any?>>;

    fun ip(): String?


    /**
     * Override this.
     */
    override fun name(): String {
        return ip() ?: "";
    }


    /**
     * Override this.
     */
    fun hostname(): String {
        return ip() ?: "";
    }


    /**
     * Override this.
     */
    fun id(): String {
        return ip() ?: "";
    }

    fun get(s:String) : SensorValue<Any?>? {
        return data[s];
    }


    fun state(): MachineState? {
        return stateMachine.state();
    }

    var monitor: Monitor<MachineState, Machine>;

    fun startMonitoring(rules: List<MonitorRule<MachineState, Machine>>) {
        println("Started monitoring ${ip()}");
        if (stateMachine.rules == null) {
            throw  IllegalArgumentException("Cannot monitor ${name()} without state machine rules.")
        }
        monitor.start(this, stateMachine, rules);
    }

    fun stopMonitoring() {
        println("Stopped monitoring ${ip()}");
        monitor.stop();
    }

    override val stateMachine: StateMachine<MachineState, Machine>;

    fun toString(): String {
        return "${name()}@${ip()} (${id()}) [${state()}]  - ${data}";
    }


}
