package kontrol.impl

import kontrol.api.Monitor
import kontrol.api.StateMachine
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kontrol.api.MachineGroupState
import kontrol.api.MachineGroup
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockGroupMonitor() : Monitor<MachineGroupState, MachineGroup> {


    val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    override fun start(target: MachineGroup, stateMachine: StateMachine<MachineGroupState, MachineGroup>, rules: List<MonitorRule<MachineGroupState, MachineGroup>>) {
        timer.scheduleWithFixedDelay({
            println("Random Transition")
            try {
                stateMachine.transition(MachineGroupState.values()[(Math.random() * MachineGroupState.values().size).toInt()])
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    override fun stop() {
        timer.shutdown();
    }


}