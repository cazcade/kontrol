package kontrol.mock

import kontrol.api.Monitor
import kontrol.api.StateMachine
import kontrol.api.Machine
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kontrol.api.MachineState
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockMachineMonitor() : Monitor<MachineState, Machine> {
    val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    override fun start(target: Machine, stateMachine: StateMachine<MachineState, Machine>, rules: List<MonitorRule<MachineState, Machine>>) {
        timer.scheduleWithFixedDelay({
            println("Random Transition")
            try {
                stateMachine.transition(MachineState.values()[(Math.random() * MachineState.values().size).toInt()])
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    override fun stop() {
        timer.shutdown();
    }


}