/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import kontrol.api.UpStreamKonfigurator
import kontrol.api.Machine
import kontrol.api.MachineGroup
import kontrol.api.MachineGroupState
import kotlin.cfclient.CloudFlareClient
import kontrol.cfclient.DomainRecord

public class CloudFlareKonfigurator(val emailAddress:String,val apiKey:String, val zone:String, val recordName: String) : UpStreamKonfigurator {


    override fun onMachineFail(machine: Machine, machineGroup: MachineGroup) {
        val newMachine = machineGroup.other(machine) ;
        if (newMachine != null) {
            configureCloudFlare(newMachine)
        } else {
            println("No 'other' machine.")
            machineGroup.stateMachine.attemptTransition(MachineGroupState.GROUP_BROKEN)
        }
    }

    override fun onMachineUnfail(machine: Machine, machineGroup: MachineGroup) {
    }

    override fun configureUpStream(machineGroup: MachineGroup) {
        configureCloudFlare(machineGroup.machines().first())
    }

    fun configureCloudFlare(machine: Machine) {
        println("Configuring cloudflare for ${machine.name()}(${machine.id()})")
        val client = CloudFlareClient(emailAddress, apiKey)
        val records = client.domainRecords(zone).filter { it.display_name == recordName}
        client.update(zone, DomainRecord(`type` = "A", name = recordName, content = machine.ip(), ttl = "1", rec_id=records[0].rec_id))
        println("Configured cloudflare for ${machine.name()}(${machine.id()}) - record id was ${records[0].rec_id}")

    }
}