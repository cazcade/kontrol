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

package kontrol.digitalocean

import kontrol.api.MachineGroup
import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.api.MachineGroupState
import kontrol.api.Monitor
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensors.SensorArray
import kontrol.api.MonitorRule
import kontrol.doclient.Droplet
import kontrol.api.DownStreamKonfigurator
import kontrol.api.UpStreamKonfigurator
import java.util.SortedSet
import java.util.TreeSet
import kontrol.api.Postmortem
import kontrol.api.Controller
import kontrol.common.*
import kontrol.ext.string.ssh.onHost

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DigitalOceanMachineGroup(val apiFactory: DigitalOceanClientFactory,
                                      public override val controller: Controller,
                                      val name: String,
                                      override val sensors: SensorArray,
                                      val config: DigitalOceanConfig,
                                      val sshKeys: String,
                                      public override val min: Int,
                                      public override val max: Int,
                                      public override val hardMax: Int,
                                      override val upstreamGroups: List<MachineGroup>,
                                      override val postmortems: List<Postmortem>,
                                      override val downStreamKonfigurator: DownStreamKonfigurator? = null,
                                      override val upStreamKonfigurator: UpStreamKonfigurator? = null) : MachineGroup{


    override val downStreamGroups: MutableList<MachineGroup> = ArrayList()
    override var enabled: Boolean = true
    override val machineMonitorRules: SortedSet<MonitorRule<MachineState, Machine>> = TreeSet();
    override val groupMonitorRules: SortedSet<MonitorRule<MachineGroupState, MachineGroup>> = TreeSet()

    override val stateMachine = DefaultStateMachine<MachineGroupState>(this);
    override val monitor: Monitor<MachineGroupState, MachineGroup> = DigitalOceanMachineGroupMonitor(this, sensors, controller)
    override val defaultMachineRules = DefaultStateMachineRules<MachineState>();

    val machines = ConcurrentHashMap<String, DigitalOceanMachine>();

    {
        machines().forEach { it.fsm.rules = defaultMachineRules }
        stateMachine.rules = DefaultStateMachineRules<MachineGroupState>();
        upstreamGroups.forEach { (it as DigitalOceanMachineGroup).downStreamGroups.add(this) }

    }

    override fun costPerHourInDollars(): Double = machines.map { it.getValue().costPerHourInDollars() }.sum()
    override fun groupName(): String = name;
    override fun name(): String = name;

    override fun machines(): List<Machine> {
        val arrayList: ArrayList<DigitalOceanMachine> = ArrayList();
        synchronized(machines) {
            arrayList.addAll(machines.values())
        }
        return arrayList;
    }

    override fun contract(): MachineGroup {
        val digitalOcean = apiFactory.instance()
        println("CONTRACTING GROUP ${name()} REQUESTED")
        try {

            println("Destroying m/c")
            var machs = machines.values().filter { it.state() !in listOf(MachineState.OK, null) }
            if (machs.size() == 0) {
                machs = machines.values().filter { it.droplet.status?.toLowerCase() == "active" }.sortBy { it.id() };
            }
            val machine = machs.first()
            machine.disable()
            failover(machine)
            val id = machine.droplet.id!!
            digitalOcean.deleteDroplet(id)
            while (digitalOcean.getDropletInfo(id).status?.toLowerCase() == "active") {
                println("Awaiting Machine ${id} OFF")
                Thread.sleep(5000);
            }
            println("Machine ${id} is OFF")
            if (stateMachine.currentState != MachineGroupState.QUIET) {
                println("CONTRACTED GROUP ${name()}")
            } else {
                Thread.sleep(10000)
            }
        } catch(e: Exception) {
            println("(${name()}) DO: " + e.getMessage())
        }
        configure()
        Thread.sleep(60 * 1000)
        return this
    }


    override fun expand(): MachineGroup {
        val droplet = Droplet()
        droplet.name = (config.machinePrefix + name)
        droplet.size_id = (config.dropletSizeId)

        val instance = apiFactory.instance()
        val availableRegions = instance.getAvailableRegions()
        //        droplet.setRegionId(availableRegions?.get((Math.random() * (availableRegions?.size()?.toDouble()?:0.0)).toInt())?.getId());
        droplet.region_id = (config.regionId)
        val images = instance.getAvailableImages()
        for (image in images) {
            if ((config.templatePrefix + name) == image.name) {
                droplet.image_id = image.id
            }

        }
        var createdDroplet = instance.createDroplet(droplet, sshKeys, privateNetworking = true)

        println("Created droplet with ID " + createdDroplet.id + " ip address " + createdDroplet.ip_address)
        var count = 0
        while (createdDroplet.ip_address == null && count++ < 20) {
            try {
                println("Waiting for IP ...")
                createdDroplet = instance.getDropletInfo(createdDroplet.id!!)
            } catch (e: Exception) {
                e.printStackTrace();
            }
            Thread.sleep(5000)
        }
        if (stateMachine.currentState != MachineGroupState.BUSY) {
            println("EXPANDED GROUP ${name()}")
            Thread.sleep(20000);
        } else {
        }
        configure()
        return this;

    }


    override fun destroy(machine: Machine): MachineGroup {
        val id = machine.id().toInt()
        try {
            println("Destroying $machine")
            val digitalOcean = apiFactory.instance();
            digitalOcean.deleteDroplet(id);
        } catch (e: Exception) {
            println("Failed to destroy ${id} due to ${e.getMessage()}")
        }

        return this;
    }


    fun waitForRestart(id: Int) {
        var count1: Int = 0;
        val instance = apiFactory.instance()
        while (instance.getDropletInfo(id).status == "active" && count1++ < 60) {
            println("Waiting for machine ${id} to stop being active")
            Thread.sleep(5000);
        }
        var count2: Int = 0;
        while (instance.getDropletInfo(id).status != "active" && count2++ < 60) {
            println("Waiting for machine ${id} to become active")
            Thread.sleep(5000);
        }
        Thread.sleep(60000);

    }

    override fun reImage(machine: Machine): MachineGroup {
        super.reImage(machine)
        val id = machine.id().toInt()
        try {

            val instance = apiFactory.instance()
            val images = instance.getAvailableImages()
            var imageId: Int? = null;
            for (image in images) {
                if ((config.templatePrefix + name) == image.name) {
                    imageId = image.id;
                    break;
                }

            }
            println("Rebuilding ${machine.id()} with ${imageId}")
            if (imageId != null) {
                instance.rebuildDroplet(id, imageId!!)
                waitForRestart(id)
                println("Rebuilt ${machine.id()}")
            } else {
                println("No valid image to rebuild ${machine.id()}")
            }
        } catch (e: Exception) {
            println("Failed to reImage ${id} due to ${e.getMessage()}")
        }
        return this;
    }


    override fun restart(machine: Machine): MachineGroup {
        val id = machine.id().toInt()
        try {
            println("Rebooting $machine")
            "reboot".onHost(machine.ip())
            waitForRestart(id)
            println("Rebuilt ${id}")
        } catch (e: Exception) {
            println("Failed to restart ${id} due to ${e.getMessage()}")
            machine.fsm.attemptTransition(MachineState.DEAD)
        }

        return this;
    }
}