package kontrol.impl.mock

import kontrol.api.MachineGroup
import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.impl.DefaultStateMachineRules
import kontrol.impl.MockMachineMonitor
import kontrol.api.MachineGroupState
import kontrol.impl.DefaultStateMachine
import kontrol.api.Monitor
import kontrol.api.sensors.SensorArray
import kontrol.impl.sensor.DefaultSensorArray
import java.util.ArrayList
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockMachineGroup(val name: String, val machines: MutableList<MockMachine>, override val monitor: Monitor<MachineGroupState, MachineGroup>) : MachineGroup{
    override val minSize: Int = 0
    override val maxSize: Int = 100000

    override val machineMonitorRules: MutableList<MonitorRule<MachineState, Machine>> = ArrayList();
    override val groupMonitorRules: MutableList<MonitorRule<MachineGroupState, MachineGroup>> = ArrayList();
    override val sensorArray: SensorArray<Any?> = DefaultSensorArray<Any?>(ArrayList())
    override val stateMachine = DefaultStateMachine<MachineGroupState, MachineGroup>(this);
    override val defaultMachineRules = DefaultStateMachineRules<MachineState, Machine>();

    {
        machines.forEach { it.stateMachine.rules = defaultMachineRules }
        stateMachine.rules = DefaultStateMachineRules<MachineGroupState, MachineGroup>();
    }

    override fun name(): String {
        return name;
    }

    override fun machines(): List<Machine> {
        return machines;
    }

    override fun expand(): MachineGroup {
        println("**** Expand $name");
        val mockMachine = MockMachine("10.10.10." + (Math.random() * 256));
        mockMachine.monitor = MockMachineMonitor();
        mockMachine.stateMachine.rules = defaultMachineRules;
        machines.add(mockMachine);
        return this;
    }

    override fun contract(): MachineGroup {
        println("**** Contract $name");
        if (machines.size() > 0) {
            machines.remove(machines[0]);
        }
        return this;
    }


    override fun destroy(machine: Machine): MachineGroup {
        machines.remove(machine);
        return this
    }
}