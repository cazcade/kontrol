package kontrol.digitalocean

import kontrol.api.MachineState
import kontrol.api.Monitor
import kontrol.api.StateMachine
import java.util.Timer
import kotlin.concurrent.*;
import kontrol.api.Machine
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public  class DigitalOceanMachineMonitor(val clientFactory: DigitalOceanClientFactory) : Monitor<MachineState, Machine>{
    val timer = Timer("DOGroupMon", true);

    override fun start(target: Machine, stateMachine: StateMachine<MachineState, Machine>, monitorRules: List<MonitorRule<MachineState, Machine>>) {
        //        println(stateMachine.rules)
        timer.schedule(500, 5000) {

            stateMachine.attemptTransition(
                    when ((target as DigitalOceanMachine).droplet.status?.toLowerCase()) {
                        "active" -> MachineState.STARTING
                        "off" -> MachineState.STOPPED
                        "new" -> MachineState.STARTING
                        "archive" -> MachineState.STOPPING
                        else -> null

                    })

            monitorRules.forEach { it.evaluate(target) }

        }

    }


    override fun stop() {
        timer.cancel();
    }
}