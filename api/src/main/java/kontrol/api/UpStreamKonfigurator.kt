package kontrol.api

/**
 * This defines how the configuration of 'upstream' machines  should change
 * when a machine fails over or the group is reconfigured.
 *
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait UpStreamKonfigurator {

    fun onMachineFail(machine: Machine,
                      machineGroup: MachineGroup) {

    }


    fun onMachineUnfail(machine: Machine,
                        machineGroup: MachineGroup) {

    }


    fun configureUpStream(machineGroup: MachineGroup) {

    }
}