package kontrol.impl.ocean

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
            val targetStatus = (target as DigitalOceanMachine).droplet.getStatus()?.toLowerCase();
            when (targetStatus) {
                "active" -> {
                    stateMachine.attemptTransition(MachineState.MACHINE_STARTING)
                }
                "off" -> {
                    stateMachine.attemptTransition(MachineState.MACHINE_STOPPED)
                }
                "new" -> {
                    stateMachine.attemptTransition(MachineState.MACHINE_STARTING)
                }
                "archive" -> {
                    stateMachine.attemptTransition(MachineState.MACHINE_STOPPING)
                }
                else -> println("-> ${targetStatus}")

            }
            monitorRules.forEach { it.evaluate(target) }

        }

    }


    override fun stop() {
        timer.cancel();
    }
}