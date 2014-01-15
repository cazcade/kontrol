package kontrol.api

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait DownStreamKonfigurator {

    fun onDownStreamMachineFail(machine: Machine,
                      machineGroup: MachineGroup,
                      thisGroup: MachineGroup) {

    }


    fun onDownStreamMachineUnfail(machine: Machine,
                        machineGroup: MachineGroup,
                        thisGroup: MachineGroup) {

    }

    fun onGroupMachineFail(machine: Machine,
                      machineGroup: MachineGroup) {

    }


    fun onGroupMachineUnfail(machine: Machine,
                        machineGroup: MachineGroup) {

    }

    fun configureDownStream(thisGroup: MachineGroup) {

    }



}