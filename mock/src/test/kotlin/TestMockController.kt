/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kontrol.test.mock

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kontrol.mock.MockInfrastructure
import java.util.HashMap
import kontrol.common.DefaultController
import kontrol.mock.MockMachine
import kontrol.mock.MockSequencedMonitor
import kontrol.api.MachineState.*
import java.util.concurrent.CopyOnWriteArrayList
import kontrol.mock.MockMachineGroup
import kontrol.api.MachineGroup
import kontrol.mock.MockSequencedGroupMonitor
import kontrol.api.MachineGroupState
import kontrol.api.MachineState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.ArrayList
import java.util.LinkedHashMap
import kontrol.mock.MockKonfigurator
import kontrol.common.NullEventLog

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
        val map: MutableMap<String, MutableList<MockMachine>> = LinkedHashMap();
        val group1: MutableList<MockMachine> = CopyOnWriteArrayList()
        val mockMachine1 = MockMachine("1.2.3.4")
        val mockMachine2 = MockMachine("1.2.3.5")
        mockMachine1.monitor = MockSequencedMonitor(machine1States);
        mockMachine2.monitor = MockSequencedMonitor(machine2States);
        group1.add(mockMachine1)
        group1.add(mockMachine2)
        val group2: MutableList<MockMachine> = CopyOnWriteArrayList()
        val group3: MutableList<MockMachine> = CopyOnWriteArrayList()
        map.put("lb", group3)
        map.put("gateway", group2)
        map.put("worker", group1)
        return map;
    }

    fun buildSimpleLBToplogy(machine1States: List<MachineState?>, machine2States: List<MachineState?>): MutableMap<String, MutableList<MockMachine>> {
        val map: MutableMap<String, MutableList<MockMachine>> = LinkedHashMap();
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
        var previous: MutableList<MachineGroup> = ArrayList()
        val controller = DefaultController(NullBus(), NullEventLog());
        controller.start(0);
        map.entrySet().forEach {
            val gs: List<MachineGroupState?> = groupStates.get(it.key) ?: listOf();
            val mockMachineGroup = MockMachineGroup(it.key, it.value, MockSequencedGroupMonitor(gs), previous, MockKonfigurator(), MockKonfigurator(), controller)
            members.put(it.key, mockMachineGroup)
            previous = arrayListOf(mockMachineGroup)
        }

        val infra = MockInfrastructure(members) ;
        snapitoPolicy(infra, controller);
        snapitoStrategy(infra, controller);
        infra.start();
        Thread.sleep(4000)
        infra.stop();
        controller.stop();
        return infra;
    }

    test fun test1(): Unit {
        val machine1States = listOf(OK)
        val machine2States = listOf(OK)
        val map = buildSimpleWorkerToplogy(machine1States, machine2States);

        val groupStates = mapOf(Pair("worker", listOf(MachineGroupState.NORMAL, MachineGroupState.QUIET, MachineGroupState.BUSY, MachineGroupState.NORMAL, MachineGroupState.GROUP_BROKEN)));
        val mockController = runTestForScenario(map, groupStates);
        //        assertEquals(2, mockController.topology().get("worker").machines().size);
        ;
    }


    test fun test2(): Unit {
        val machine1States = listOf(OK)
        val machine2States = listOf(OK)
        val map = buildSimpleWorkerToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("worker", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("worker").machines().size);
        assertTrue(mockController.topology().get("worker").machines().all { it.state() == OK })
    }

    test fun test3(): Unit {
        val machine1States = listOf(OK, BROKEN, DEAD)
        val machine2States = listOf(OK, BROKEN, DEAD)
        val map = buildSimpleFullToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("worker", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("worker").machines().size);
        assertEquals(0, (mockController.topology().get("gateway") .downStreamKonfigurator as MockKonfigurator).downStreamConfigureCalls)
    }

    test fun test4(): Unit {
        val machine1States = listOf(OK, BROKEN)
        val machine2States = listOf(OK, BROKEN)
        val map = buildSimpleLBToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("lb", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("lb").machines().size);
    }

    test fun test5(): Unit {
        val machine1States = listOf(OK, BROKEN)
        val machine2States = listOf(OK, BROKEN)
        val map = buildSimpleLBToplogy(machine1States, machine2States);
        val groupStates = mapOf(Pair("lb", listOf(MachineGroupState.NORMAL)));
        val mockController = runTestForScenario(map, groupStates)
        assertEquals(2, mockController.topology().get("lb").machines().size);
        assertTrue(mockController.topology().get("lb").machines().all { it.state() == BROKEN })

    }


}
