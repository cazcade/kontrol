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

package kontrol.examples.docean

import kontrol.api.UpStreamKonfigurator
import kontrol.api.Machine
import kontrol.api.MachineGroup
import kontrol.ext.string.ssh.onHost

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class WorkerKonfigurator : UpStreamKonfigurator {


    override fun onMachineFail(machine: Machine, machineGroup: MachineGroup) {
        "touch /tmp/snapito-disable" onHost machine.hostname()
        Thread.sleep(30 * 1000)
    }


    override fun onMachineUnfail(machine: Machine, machineGroup: MachineGroup) {
        "rm /tmp/snapito-disable" onHost machine.hostname()
    }


}