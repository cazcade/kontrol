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

package kontrol.common.group.ext

import kontrol.api.MachineGroup.Recheck.*
import kontrol.api.MachineGroupState.*
import kontrol.api.GroupAction.*
import kontrol.api.Action.*
import kontrol.api.MachineState.*
import kontrol.api.Controller
import kontrol.api.MachineGroup
import kontrol.api.PostmortemStore

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public fun MachineGroup.allowDefaultTransitions() {

    this allowMachine (RESTARTING to OK);
    this allowMachine (RESTARTING to BROKEN);
    this allowMachine (RESTARTING to STOPPED);
    this allowMachine (RESTARTING to DEAD);
    this allowMachine (RESTARTING to FAILED);
    this allowMachine (OK to STOPPING);
    this allowMachine (OK to STOPPED);
    this allowMachine (OK to BROKEN);
    this allowMachine (OK to STALE);
    this allowMachine (STOPPING to STOPPED);
    this allowMachine (STOPPING to BROKEN);
    this allowMachine (STOPPING to OK);
    this allowMachine (STOPPED to DEAD);
    this allowMachine (STOPPED to RESTARTING);
    this allowMachine (STOPPED to OK);
    this allowMachine (STOPPED to BROKEN);
    this allowMachine (BROKEN to STALE);
    this allowMachine (BROKEN to RESTARTING);
    this allowMachine (BROKEN to OK);
    this allowMachine (BROKEN to DEAD);
    this allowMachine (BROKEN to FAILED);
    this allowMachine (BROKEN to REBUILDING);
    this allowMachine (RESTARTING to REBUILDING);
    this allowMachine (STOPPED to REBUILDING);
    this allowMachine (STOPPING to REBUILDING);
    this allowMachine (OK to REBUILDING);
    this allowMachine (DEAD to REBUILDING);
    this allowMachine (FAILED to REBUILDING);
    this allowMachine (STALE to REBUILDING);
    this allowMachine (REBUILDING to RESTARTING);
    this allowMachine (REBUILDING to OK);
    this allowMachine (REBUILDING to BROKEN);
    this allowMachine (DEAD to STALE);
    this allowMachine (DEAD to FAILED);
    this allowMachine (DEAD to OK);
    this allowMachine (OK to FAILED);
    this allowMachine (STALE to  STOPPING);

    this allow (QUIET to BUSY);
    this allow (QUIET to NORMAL);
    this allow (QUIET to GROUP_BROKEN);
    this allow (BUSY to NORMAL);
    this allow (BUSY to QUIET);
    this allow (BUSY to  GROUP_BROKEN);
    this allow (NORMAL to GROUP_BROKEN);
    this allow (NORMAL to QUIET);
    this allow (NORMAL to BUSY);
    this allow (GROUP_BROKEN to QUIET);
    this allow (GROUP_BROKEN to BUSY);
    this allow (GROUP_BROKEN to NORMAL);
}


public fun MachineGroup.applyDefaultPolicies(controller: Controller, postmortemStore: PostmortemStore) {

    this whenMachine BROKEN recheck THEN tell controller takeActions listOf(FAILOVER, RESTART_MACHINE);
    this whenMachine OK recheck THEN tell controller takeAction FAILBACK;
    this whenMachine DEAD recheck THEN tell controller takeActions listOf(FAILOVER, REIMAGE_MACHINE) ;
    this whenMachine STALE recheck THEN tell controller takeActions listOf(FAILOVER, UPGRADE);
    this whenMachine FAILED recheck THEN tell controller takeActions listOf(FAILOVER, DESTROY_MACHINE);
    this whenGroup BUSY recheck THEN use controller takeAction EXPAND;
    this whenGroup QUIET recheck THEN use controller takeAction CONTRACT;
    this whenGroup GROUP_BROKEN recheck THEN use controller  takeActions listOf(EMERGENCY_FIX);

    controller will { it.disable();this.reImage(it);it.enable();java.lang.String() } takeAction UPGRADE inGroup this;
    controller will { this.failback(it); java.lang.String() } takeAction FAILBACK inGroup this;
    controller will { this.failover(it); java.lang.String() } takeAction FAILOVER inGroup this;
    controller will { it.disable();postmortemStore.addAll(this.postmortem(it));this.reImage(it);it.enable()  ;java.lang.String() } takeAction REIMAGE_MACHINE inGroup this;
    controller will { it.disable(); postmortemStore.addAll(this.postmortem(it));this.restart(it) ;it.enable();java.lang.String() } takeAction RESTART_MACHINE inGroup this;
    controller will { this.expand();it.disable(); postmortemStore.addAll(this.postmortem(it));this.destroy(it); it.enable();java.lang.String() } takeAction DESTROY_MACHINE inGroup this;
    controller use { this.expand(); this.configure();java.lang.String() } to EXPAND  IF { this.workingSize() < this.max }  group this;
    controller use { this.contract(); this.configure();java.lang.String() } to CONTRACT IF { this.workingSize() > this.min || this.machines().size() > this.hardMax } group this;
    controller use { it.machines().sortBy { it.state().toString() }.forEach { this.failAction(it) { this.reImage(it) } }; this.configure();java.lang.String() } to EMERGENCY_FIX group this;
}

public fun MachineGroup.applyDefaultRules(timeFactor: Int = 60) {
    this memberIs STALE ifStateIn listOf(STALE, OK) andTest { Math.random() < 0.4 && this.other(it) != null } after 60 * timeFactor seconds "stale"
    this memberIs BROKEN ifStateIn listOf(BROKEN, null, RESTARTING, STOPPING, STOPPED) after timeFactor * 3 seconds "bad-now-broken"
    this memberIs DEAD ifStateIn listOf(DEAD, BROKEN, REBUILDING, RESTARTING, STOPPING, STOPPED) after timeFactor * 10 seconds "escalate-broken-to-dead"
    val escalateDuration: Long = 60L * timeFactor * 1000L
    this memberIs DEAD ifStateIn listOf(DEAD, BROKEN, REBUILDING, RESTARTING, STOPPING, STOPPED) andTest { it.fsm.history.percentageInWindow(listOf(BROKEN, REBUILDING, RESTARTING), (escalateDuration / 2)..escalateDuration) > 60.0 } after timeFactor seconds "flap-now-escalate-to-dead"
    this memberIs FAILED ifStateIn listOf(FAILED, DEAD, REBUILDING, RESTARTING, STOPPING, STOPPED) andTest { it.fsm.history.percentageInWindow(listOf(DEAD, RESTARTING, REBUILDING), (escalateDuration / 2)..escalateDuration) > 60.0 } after timeFactor seconds "flap-now-escalate-to-failed"
}

public fun MachineGroup.addMachineSensorRules(vararg rules: Pair<String, Double>, timeFactor: Int = 60) {

    this memberIs OK ifStateIn listOf(DEAD, BROKEN, REBUILDING, RESTARTING, null) andTest { machine -> rules.all { machine[it.first]?.D()?:(it.second + 1) < it.second } } after timeFactor / 2 seconds "machine-ok"

    this memberIs BROKEN ifStateIn listOf(OK, BROKEN, REBUILDING, RESTARTING, STOPPING, STOPPED, STALE, null) andTest { machine -> rules.any { machine[it.first]?.D()?:(it.second - 1) > it.second } } after timeFactor * 4 seconds "machine-broken"
}


fun MachineGroup.addGroupSensorRules(vararg ranges: Pair<String, Range<Double>>, timeFactor: Int = 60) {

    this becomes GROUP_BROKEN ifStateIn  listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest { it.workingSize() == 0 || it.activeSize() == 0 } after timeFactor seconds "group-size-dangerously-low"

    this becomes BUSY ifStateIn  listOf(QUIET, BUSY, NORMAL, null) andTest { it.workingSize() < it.min } after timeFactor * 3 seconds "not-enough-working-machines-in-group"

    this becomes BUSY ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest {
        ranges.any { this[it.first]?:it.second.end > it.second.end }
    }  after timeFactor * 3 seconds "overload"

    this becomes QUIET ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest { this.workingSize() > this.max || this.machines().size() > this.hardMax }  after timeFactor seconds "too-many-machines"


    this becomes QUIET ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest {
        !ranges.any { this[it.first]?:it.second.start > it.second.end } && ranges.any { this[it.first]?:it.second.start < it.second.start }
    }  after timeFactor * 5 seconds "underload"

    this becomes NORMAL ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, null) andTest {
        ranges.all { this[it.first]?:it.second.start in it.second }
        && this.workingSize() in this.min..this.max
    }  after timeFactor / 2 seconds "group-ok"

}