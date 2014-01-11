/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kontrol.impl.mock.MockInfrastructure
import java.util.HashMap
import kontrol.impl.DefaultController
import kontrol.impl.snapitoPolicy
import kontrol.impl.snapitoStrategy
import kontrol.impl.mock.MockMachine
import kontrol.impl.MockSequencedMonitor
import kontrol.api.MachineState.*
import java.util.concurrent.CopyOnWriteArrayList
import kontrol.impl.mock.MockMachineGroup
import kontrol.api.MachineGroup
import kontrol.impl.MockSequencedGroupMonitor
import kontrol.api.MachineGroupState
import kontrol.api.MachineState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class TestMockController {

    fun buildSimpleWorkerToplogy(machine1States: List<MachineState?>, machine2States: List<MachineState?>): MutableMap<String, MutableList<MockMachine>> {
        val map: MutableMap<String, MutableList<MockMachine>> = HashMap();
        val group1: MutableList<MockMachine> = CopyOnWriteArrayList()
        val mockMachine1 = MockMachine("1.2.3.4")
        val mockMachine2 = MockMachine("1.2.3.5")
        mockMachine1.monitor = MockSequencedMonitor(machine1States);
        mockMachine2.monitor = MockSequencedMonitor(machine2States);
        group1.add(mockMachine1)
        group1.add(mockMachine2)
        val group2: MutableList<MockMachine> = CopyOnWriteArrayList()
        val group3: MutableList<MockMachine> = CopyOnWriteArrayList()
        map.put("worker", group1)
        return map;
    }

    fun buildSimpleFullToplogy(machine1States: List<MachineState?>, machine2States: List<MachineState?>): MutableMap<String, MutableList<MockMachine>> {
        val map: MutableMap<String, MutableList<MockMachine>> = HashMap();
        val group1: MutableList<MockMachine> = CopyOnWriteArrayList()
        val mockMachine1 = MockMachine("1.2.3.4")
        val mockMachine2 = MockMachine("1.2.3.5")
        mockMachine1.monitor = MockSequencedMonitor(machine1States);
        mockMachine2.monitor = MockSequencedMonitor(machine2States);
        group1.add(mockMachine1)
        group1.add(mockMachine2)
        val group2: MutableList<MockMachine> = CopyOnWriteArrayList()
        val group3: MutableList<MockMachine> = CopyOnWriteArrayList()
        map.put("worker", group1)
        map.put("gateway", group2)
        map.put("lb", group3)
        return map;
    }

    fun buildSimpleLBToplogy(machine1States: List<MachineState?>, machine2States: List<MachineState?>): MutableMap<String, MutableList<MockMachine>> {
        val map: MutableMap<String, MutableList<MockMachine>> = HashMap();
        val group1: MutableList<MockMachine> = CopyOnWriteArrayList()
        val mockMachine1 = MockMachine("1.2.3.4")
        val mockMachine2 = MockMachine("1.2.3.5")
        mockMachine1.monitor = MockSequencedMonitor(machine1States);
        mockMachine2.monitor = MockSequencedMonitor(machine2States);
        group1.add(mockMachine1)
        group1.add(mockMachine2)
        map.put("lb", group1)
        return map;
    }

    fun runTestForScenario(map: Map<String, MutableList<MockMachine>>, groupStates: Map<String, List<MachineGroupState?>>): MockInfrastructure {

        val members = HashMap<String, MachineGroup>();
        map.entrySet().forEach {
            val gs: List<MachineGroupState?> = groupStates.get(it.key) ?: listOf();
            members.put(it.key, MockMachineGroup(it.key, it.value, MockSequencedGroupMonitor(gs)))
        }

        val infra = MockInfrastructure(members) ;
        val controller = DefaultController();
        snapitoPolicy(infra, controller);
        snapitoStrategy(infra, controller);
        infra.start();
        infra.stop();
        return infra;
    }

    test fun test1(): Unit {
        val machine1States = listOf(STARTING, OK)
        val machine2States = listOf(STARTING, OK)
        val map = buildSimpleWorkerToplogy(machine1States, machine2States);

        val groupStates = mapOf(Pair("worker", listOf(MachineGroupState.NORMAL, MachineGroupState.QUIET, MachineGroupState.BUSY, MachineGroupState.NORMAL, MachineGroupState.GROUP_BROKEN)));
        val mockController = runTestForScenario(map, groupStates);
//        assertEquals(2, mockController.topology().get("worker").machines().size);
        ;
    }


    test fun test2(): Unit {
        val machine1States = listOf(STARTING, OK)
        val machine2States = listOf(STARTING, OK)
        val map = buildSimpleWorkerToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("worker", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("worker").machines().size);
        assertTrue(mockController.topology().get("worker").machines().all { it.state() == OK })
    }

    test fun test3(): Unit {
        val machine1States = listOf(STARTING, OK, BROKEN)
        val machine2States = listOf(STARTING, OK, BROKEN)
        val map = buildSimpleWorkerToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("worker", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("worker").machines().size);
    }

    test fun test4(): Unit {
        val machine1States = listOf(STARTING, OK, BROKEN, STOPPING, STARTING, OK)
        val machine2States = listOf(STARTING, OK, BROKEN)
        val map = buildSimpleLBToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("lb", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("lb").machines().size);
    }

    test fun test5(): Unit {
        val machine1States = listOf(STARTING, OK, BROKEN)
        val machine2States = listOf(STARTING, OK, BROKEN)
        val map = buildSimpleLBToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("lb", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("lb").machines().size);
        assertTrue(mockController.topology().get("lb").machines().all { it.state() == BROKEN })

    }


}
