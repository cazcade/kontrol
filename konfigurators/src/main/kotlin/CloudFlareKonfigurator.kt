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

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
import kontrol.api.UpStreamKonfigurator
import kontrol.api.Machine
import kontrol.api.MachineGroup
import kotlin.cfclient.CloudFlareClient
import kontrol.cfclient.DomainRecord

public class CloudFlareKonfigurator(val emailAddress: String, val apiKey: String, val zone: String, val recordName: String) : UpStreamKonfigurator {


    override fun onMachineFail(machine: Machine, machineGroup: MachineGroup) {
        val newMachine = machineGroup.other(machine) ;
        if (newMachine != null) {
            configureCloudFlare(newMachine)
        } else {
            println("No 'other' machine.")

        }
    }

    override fun onMachineUnfail(machine: Machine, machineGroup: MachineGroup) {
    }

    override fun configureUpStream(machineGroup: MachineGroup) {
        val machines = machineGroup.machines()
        if (machines.size() > 0) configureCloudFlare(machines.first())
    }

    fun configureCloudFlare(machine: Machine) {
        println("Configuring cloudflare for ${machine.name()}(${machine.id()})")
        val client = CloudFlareClient(emailAddress, apiKey)
        val records = client.domainRecords(zone).filter { it.display_name == recordName }
        client.update(zone, DomainRecord(`type` = "A", name = recordName, content = machine.ip(), ttl = "1", rec_id = records[0].rec_id))
        println("Configured cloudflare for ${machine.name()}(${machine.id()}) - record id was ${records[0].rec_id}")

    }
}