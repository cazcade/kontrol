package kontrol.api


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Topology {

    val members: Map<String, MachineGroup>

    fun members(): Map<String, MachineGroup> {
        return members;
    }

    fun start() {
        println("Started Toplogy")
        each { it.startMonitoring() }
    }

    fun stop() {
        each { it.stopMonitoring() }
    }

    fun each(action: (MachineGroup) -> Unit): Topology {
        members().values().forEach { action(it) };
        return this;
    }

    fun eachMachine(action: (Machine) -> Unit): Topology {
        members().values().forEach { it.machines().forEach { action(it) } };
        return this;
    }

    fun get(name: String): MachineGroup {
        val machineGroup = members().get(name)
        return if (machineGroup != null) {
            machineGroup
        } else {
            throw  IllegalArgumentException("No such group $name")
        }
    }

    fun toString(): String {
        var string: String = "Toplogy: \n";
        for (member in members) {
            string += "${member.value}\n"
        }
        return string;
    }


}
