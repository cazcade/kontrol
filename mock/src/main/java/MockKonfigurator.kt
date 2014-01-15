package kontrol.mock

import kontrol.api.DownStreamKonfigurator
import kontrol.api.UpStreamKonfigurator
import kontrol.api.Machine
import kontrol.api.MachineGroup

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockKonfigurator : DownStreamKonfigurator, UpStreamKonfigurator{

    public var calls: Int= 0;
    public var downStreamConfigureCalls: Int= 0;

    override fun onDownStreamMachineFail(machine: Machine, machineGroup: MachineGroup, thisGroup: MachineGroup) {
        calls++
        super<DownStreamKonfigurator>.onDownStreamMachineFail(machine, machineGroup, thisGroup)
    }
    override fun onDownStreamMachineUnfail(machine: Machine, machineGroup: MachineGroup, thisGroup: MachineGroup) {
        calls++
        super<DownStreamKonfigurator>.onDownStreamMachineUnfail(machine, machineGroup, thisGroup)
    }
    override fun configureDownStream(thisGroup: MachineGroup) {
        calls++
        downStreamConfigureCalls++
        super<DownStreamKonfigurator>.configureDownStream(thisGroup)
    }
    override fun onMachineFail(machine: Machine, machineGroup: MachineGroup) {
        calls++
        super<UpStreamKonfigurator>.onMachineFail(machine, machineGroup)
    }
    override fun onMachineUnfail(machine: Machine, machineGroup: MachineGroup) {
        calls++
        super<UpStreamKonfigurator>.onMachineUnfail(machine, machineGroup)
    }
    override fun configureUpStream(machineGroup: MachineGroup) {
        calls++
        super<UpStreamKonfigurator>.configureUpStream(machineGroup)
    }
}