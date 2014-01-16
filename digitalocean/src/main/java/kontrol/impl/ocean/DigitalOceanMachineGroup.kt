package kontrol.digitalocean

import kontrol.api.MachineGroup
import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.api.MachineGroupState
import kontrol.common.DefaultStateMachine
import kontrol.common.DefaultStateMachineRules
import kontrol.api.Monitor
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensors.SensorArray
import kontrol.api.MonitorRule
import kontrol.doclient.Droplet
import kontrol.common.onHost
import kontrol.api.DownStreamKonfigurator
import kontrol.api.UpStreamKonfigurator

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DigitalOceanMachineGroup(val apiFactory: DigitalOceanClientFactory, val name: String, override val sensors: SensorArray<Any?>, val config: DigitalOceanConfig, val sshKeys: String, public override val min: Int, public override val max: Int, override val upstreamGroups: List<MachineGroup>, override val downStreamKonfigurator: DownStreamKonfigurator? = null, override val upStreamKonfigurator: UpStreamKonfigurator? = null) : MachineGroup{
    override val downStreamGroups: MutableList<MachineGroup> = ArrayList()

    override var enabled: Boolean = true
    override val machineMonitorRules: MutableList<MonitorRule<MachineState, Machine>> = ArrayList();
    override val groupMonitorRules: MutableList<MonitorRule<MachineGroupState, MachineGroup>> = ArrayList()

    override val stateMachine = DefaultStateMachine<MachineGroupState, MachineGroup>(this);
    override val monitor: Monitor<MachineGroupState, MachineGroup> = DigitalOceanMachineGroupMonitor(this, sensors)
    override val defaultMachineRules = DefaultStateMachineRules<MachineState, Machine>();

    val machines = ConcurrentHashMap<String, DigitalOceanMachine>();

    {
        machines().forEach { it.stateMachine.rules = defaultMachineRules }
        stateMachine.rules = DefaultStateMachineRules<MachineGroupState, MachineGroup>();
        upstreamGroups.forEach { (it as DigitalOceanMachineGroup).downStreamGroups.add(this) }

    }

    override fun name(): String {
        return name;
    }

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
            val machine = machines.values().filter { it.droplet.status?.toLowerCase() == "active" }.sortBy { it.id() }. first()
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
        var createdDroplet = instance.createDroplet(droplet, sshKeys, privateNetworking=true)

        println("Created droplet with ID " + createdDroplet?.id + " ip address " + createdDroplet?.ip_address)
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
        println("Destroying $machine")
        val digitalOcean = apiFactory.instance();
        digitalOcean.deleteDroplet(machine.id().toInt());
        return this;
    }


    fun waitForRestart(id: Int) {
        var count1:Int= 0;
        val instance = apiFactory.instance()
        while (instance.getDropletInfo(id).status == "active"&& count1++ < 100) {
            println("Waiting for machine ${id} to stop being active")
            Thread.sleep(1000);
        }
        var count2:Int= 0;
        while (instance.getDropletInfo(id).status != "active" && count2++ < 100) {
            println("Waiting for machine ${id} to become active")
            Thread.sleep(1000);
        }
        Thread.sleep(60000);

    }

    override fun reImage(machine: Machine): MachineGroup {
        val instance = apiFactory.instance()
        val images = instance.getAvailableImages()
        var imageId: Int? = null;
        for (image in images) {
            if ((config.templatePrefix + name) == image.name) {
                imageId = image.id;
                break;
            }

        }
        val id = machine.id().toInt()
        println("Rebuilding ${machine.id()} with ${imageId}")
        if (imageId != null) {
            instance.rebuildDroplet(id, imageId!!)
            waitForRestart(id)
            println("Rebuilt ${machine.id()}")
        } else {
            println("No valid image to rebuild ${machine.id()}")
        }
        return this;
    }


    override fun restart(machine: Machine): MachineGroup {
        println("Rebooting $machine")
        "reboot".onHost(machine.ip())
        val instance = apiFactory.instance()
        val id = machine.id().toInt()
        waitForRestart(id)
        println("Rebuilt ${id}")
        return this;
    }
}