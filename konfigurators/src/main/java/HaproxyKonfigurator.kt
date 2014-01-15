/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
package kontrol.konfigurators

import kontrol.api.DownStreamKonfigurator
import kontrol.api.Machine
import kontrol.api.MachineGroup
import org.apache.velocity.VelocityContext
import java.io.StringWriter
import java.io.File
import kontrol.common.scp
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import kontrol.api.UpStreamKonfigurator
import kontrol.api.MachineState
import kontrol.common.onHost


public class HaproxyKonfigurator(val templateName: String) : DownStreamKonfigurator, UpStreamKonfigurator {
    var ve = VelocityEngine();
    {

        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader().javaClass.getName());
        ve.init();

    }

    override fun onDownStreamMachineFail(machine: Machine, machineGroup: MachineGroup, thisGroup: MachineGroup) {
        configureInternal(thisGroup, machine, machineGroup)
    }
    override fun onDownStreamMachineUnfail(machine: Machine, machineGroup: MachineGroup, thisGroup: MachineGroup) {
        configureInternal(thisGroup, machine, machineGroup)
    }
    override fun configureDownStream(thisGroup: MachineGroup) {
        thisGroup.downStreamGroups.forEach { configureInternal(thisGroup, null, it) }
    }

    override fun onGroupMachineFail(machine: Machine, machineGroup: MachineGroup) {
        machineGroup.downStreamGroups.forEach { configureInternal(machineGroup, null, it) }
    }
    override fun onGroupMachineUnfail(machine: Machine, machineGroup: MachineGroup) {
        machineGroup.downStreamGroups.forEach { configureInternal(machineGroup, null, it) }
    }

    fun configureInternal(thisGroup: MachineGroup, downStreamMachine: Machine? = null, downstreamGroup: MachineGroup? = null) {
        val template = ve.getTemplate(templateName)!!
        thisGroup.machines().filter { it.state() != MachineState.STOPPED } forEach {
            try {
                println("Configuring HA Proxy on ${it.name()}")
                val context = VelocityContext()
                context["downstreamMachine"] = downStreamMachine;
                context["downstreamWorkingMachines"] = downstreamGroup?.machines()?.filter { it.state() == MachineState.OK };
                context["downstreamGroup"] = downstreamGroup
                context["thisGroup"] = thisGroup
                context["thisMachine"] = it
                val writer = StringWriter();
                template.merge(context, writer)

                val file = File.createTempFile("haproxy", ".cfg")
                file.writeText(writer.getBuffer().toString())
                file.scp(host = it.hostname(), path = "/etc/haproxy/haproxy.cfg")
                "service haproxy restart".onHost(it.hostname())
                println("Configured HA Proxy on ${it.name()}")
            } catch (e: Exception) {
               println("${e.getMessage()} on ${it.name()}")
            }


        }
    }


    fun main(args: List<String>) {

    }
}

public fun <T> VelocityContext.set(key: String, value: T): T? {
    return this.put(key, value) as T?

}
