package kontrol.impl.ocean

import kontrol.api.Machine
import kontrol.doclient.Droplet
import kontrol.api.MachineState
import kontrol.api.Monitor
import kontrol.api.StateMachine
import kontrol.impl.DefaultStateMachine
import java.util.Timer
import kotlin.concurrent.*;
import kontrol.api.sensors.SensorArray
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensor.SensorValue
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public final class DigitalOceanMachine(var droplet: Droplet,
                                       val clientFactory: DigitalOceanClientFactory,

                                       val sensorArray: SensorArray<Any?>) : Machine {
    override var data: ConcurrentMap<String, SensorValue<Any?>> = ConcurrentHashMap();

    override var monitor: Monitor<MachineState, Machine> = DigitalOceanMachineMonitor(clientFactory);
    override val stateMachine: StateMachine<MachineState, Machine> = DefaultStateMachine<MachineState, Machine>(this);

    val timer = Timer("DO-" + id(), true);

    override fun id(): String {
        return droplet.id.toString();
    }


    override fun startMonitoring(rules: List<MonitorRule<MachineState, Machine>>) {
        super<Machine>.startMonitoring(rules);
        timer.schedule((5000 * Math.random()).toLong(), 20000) {
            //            println("Updating ${droplet.getId()}")
            val doa = clientFactory.instance();
            droplet = doa.getDropletInfo(droplet.id?:-1) ?: droplet;
            if (state() in listOf(MachineState.BROKEN, MachineState.OK, MachineState.STARTING, MachineState.STALE)) {
                data.putAll(sensorArray.values(this@DigitalOceanMachine))
            } else {
                data.clear()
            }
        }

    }


    override fun stopMonitoring() {
        timer.cancel();
        super<Machine>.stopMonitoring()
    }

    override fun ip(): String? {
        val ip = droplet.ip_address
        return ip;
    }


    override fun name(): String {
        return "${droplet.name} (${droplet.id})"
    }


}
