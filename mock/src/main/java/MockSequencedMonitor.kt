package kontrol.mock

import kontrol.api.Monitor
import kontrol.api.StateMachine
import kontrol.api.Machine
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import kontrol.api.MachineState
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockSequencedMonitor(val states: List<MachineState?>) : Monitor<MachineState, Machine> {


    val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    override fun start(target: Machine, stateMachine: StateMachine<MachineState, Machine>, rules: List<MonitorRule<MachineState, Machine>>) {
        println("Sequenced Transition")
        for (count in 0..states.size - 1) {
            stateMachine.transition(states[count])
        }
    }

    override fun stop() {
        timer.shutdown();
    }


}