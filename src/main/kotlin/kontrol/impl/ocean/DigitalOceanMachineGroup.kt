package kontrol.impl.ocean

import kontrol.api.MachineGroup
import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.api.MachineGroupState
import kontrol.impl.DefaultStateMachine
import kontrol.impl.DefaultStateMachineRules
import kontrol.api.Monitor
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensors.SensorArray
import kontrol.api.MonitorRule
import com.myjeeva.digitalocean.pojo.Droplet

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DigitalOceanMachineGroup(val apiFactory: DigitalOceanClientFactory, val name: String, override val sensorArray: SensorArray<Any?>, val config: DigitalOceanConfig, public override val minSize: Int, public override val maxSize: Int) : MachineGroup{
    override val machineMonitorRules: MutableList<MonitorRule<MachineState, Machine>> = ArrayList();
    override val groupMonitorRules: MutableList<MonitorRule<MachineGroupState, MachineGroup>> = ArrayList()

    override val stateMachine = DefaultStateMachine<MachineGroupState, MachineGroup>(this);
    override val monitor: Monitor<MachineGroupState, MachineGroup> = DigitalOceanMachineGroupMonitor(this, sensorArray)
    override val defaultMachineRules = DefaultStateMachineRules<MachineState, Machine>();

    val machines = ConcurrentHashMap<String, DigitalOceanMachine>();

    {
        machines().forEach { it.stateMachine.rules = defaultMachineRules }
        stateMachine.rules = DefaultStateMachineRules<MachineGroupState, MachineGroup>();
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
            val id = machines.values().filter { it.droplet.getStatus()?.toLowerCase() == "active" }.sortBy { it.id() }.first().droplet.getId()
            digitalOcean.shutdownDroplet(id)
            while (digitalOcean.getDropletInfo(id)?.getStatus()?.toLowerCase() == "active") {
                println("Awaiting Machine ${id} OFF")
                Thread.sleep(5000);
            }
            println("Machine ${id} is OFF")
            digitalOcean.deleteDroplet(id)
            if (stateMachine.currentState != MachineGroupState.UNDERLOADED) {
                println("CONTRACTED GROUP ${name()}")
            } else {
                Thread.sleep(10000)
            }
        } catch(e: Exception) {
            println("(${name()}) DO: " + e.getMessage())
        }
        return this
    }


    override fun expand(): MachineGroup {
        val droplet = Droplet()
        droplet.setName(config.machinePrefix + name)
        droplet.setSizeId(config.dropletSizeId)
        val instance = apiFactory.instance()
        val availableRegions = instance.getAvailableRegions()
        //        droplet.setRegionId(availableRegions?.get((Math.random() * (availableRegions?.size()?.toDouble()?:0.0)).toInt())?.getId());
        droplet.setRegionId(config.regionId)
        val availableImages = instance.getAvailableImages()
        for (availableImage in availableImages!!) {
            if ((config.templatePrefix + name).equals(availableImage.getName())) {
                droplet.setImageId(availableImage.getId())
            }

        }
        var createdDroplet = instance.createDroplet(droplet, "Neil Laptop")

        println("Created droplet with ID " + createdDroplet?.getId() + " ip address " + createdDroplet?.getIpAddress())
        var count = 0
        while (createdDroplet?.getIpAddress() == null && count++ < 20) {
            try {
                println("Waiting for IP ...")
                createdDroplet = instance.getDropletInfo(createdDroplet?.getId())
            } catch (e: Exception) {
                e.printStackTrace();
            }
            Thread.sleep(5000)
        }
        if (stateMachine.currentState != MachineGroupState.OVERLOADED) {
            println("EXPANDED GROUP ${name()}")
            Thread.sleep(20000);
        } else {
        }
        return this;

    }


    override fun destroy(machine: Machine): MachineGroup {
        println("Destroying $machine")
        val digitalOcean = apiFactory.instance();
        digitalOcean.deleteDroplet(machine.id().toInt());
        return this;
    }
}