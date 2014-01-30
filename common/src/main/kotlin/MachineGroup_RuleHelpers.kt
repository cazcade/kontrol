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

    this allowMachine (STARTING to OK);
    this allowMachine (STARTING to BROKEN);
    this allowMachine (STARTING to STOPPED);
    this allowMachine (STARTING to DEAD);
    this allowMachine (STARTING to FAILED);
    this allowMachine (OK to STOPPING);
    this allowMachine (OK to STOPPED);
    this allowMachine (OK to BROKEN);
    this allowMachine (OK to STALE);
    this allowMachine (STOPPING to STOPPED);
    this allowMachine (STOPPING to BROKEN);
    this allowMachine (STOPPING to STARTING);
    this allowMachine (STOPPING to OK);
    this allowMachine (STOPPED to DEAD);
    this allowMachine (STOPPED to STARTING);
    this allowMachine (STOPPED to OK);
    this allowMachine (STOPPED to BROKEN);
    this allowMachine (BROKEN to STARTING);
    this allowMachine (BROKEN to OK);
    this allowMachine (BROKEN to DEAD);
    this allowMachine (BROKEN to FAILED);
    this allowMachine (BROKEN to REBUILDING);
    this allowMachine (STARTING to REBUILDING);
    this allowMachine (STOPPED to REBUILDING);
    this allowMachine (STOPPING to REBUILDING);
    this allowMachine (OK to REBUILDING);
    this allowMachine (DEAD to REBUILDING);
    this allowMachine (FAILED to REBUILDING);
    this allowMachine (STALE to REBUILDING);
    this allowMachine (REBUILDING to STARTING);
    this allowMachine (REBUILDING to OK);
    this allowMachine (REBUILDING to BROKEN);
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

    this whenMachine BROKEN recheck THEN tell controller takeAction RESTART_MACHINE;
    this whenMachine DEAD recheck THEN tell controller takeAction REIMAGE_MACHINE ;
    this whenMachine STALE recheck THEN tell controller takeAction REIMAGE_MACHINE;
    this whenMachine FAILED recheck THEN tell controller takeAction DESTROY_MACHINE;
    this whenGroup BUSY recheck THEN use controller takeAction EXPAND;
    this whenGroup QUIET recheck THEN use controller takeAction CONTRACT;
    this whenGroup GROUP_BROKEN recheck THEN use controller  takeActions listOf(EXPAND, EMERGENCY_FIX);

    controller will { this.failAction(it) { postmortemStore.addAll(this.postmortem(it));this.reImage(it) } ;java.lang.String() } takeAction REIMAGE_MACHINE inGroup this;
    controller will { this.failAction(it) { postmortemStore.addAll(this.postmortem(it));this.restart(it) };java.lang.String() } takeAction RESTART_MACHINE inGroup this;
    controller will { this.failAction(it) { this.expand(); postmortemStore.addAll(this.postmortem(it));this.destroy(it) };java.lang.String() } takeAction DESTROY_MACHINE inGroup this;
    controller use { this.expand(); this.configure();java.lang.String() } to EXPAND  IF { this.workingSize() < this.max }  group this;
    controller use { this.contract(); this.configure();java.lang.String() } to CONTRACT IF { this.workingSize() > this.min } group this;
    controller use { this.configure();it.machines().sortBy { it.state().toString() }.forEach { postmortemStore.addAll(this.postmortem(it)); this.reImage(it);this.failback(it) }; this.configure();java.lang.String() } to EMERGENCY_FIX group this;
}

public fun MachineGroup.applyDefaultRules() {
    this memberIs BROKEN ifStateIn listOf(BROKEN, null, STARTING, STOPPING, STOPPED) after 120 seconds "bad-now-broken"
    this memberIs DEAD ifStateIn listOf(DEAD, BROKEN, REBUILDING) after 180 seconds "escalate-broken-to-dead"
    val escalateDuration: Long = 30 * 60 * 1000
    this memberIs DEAD ifStateIn listOf(DEAD, BROKEN) andTest { it.fsm.history.percentageInWindow(listOf(BROKEN, REBUILDING, STARTING), (escalateDuration / 2)..escalateDuration) > 60.0 } after 60 seconds "flap-now-escalate-to-dead"
    this memberIs FAILED ifStateIn listOf(FAILED, DEAD) andTest { it.fsm.history.percentageInWindow(listOf(DEAD, REBUILDING), (escalateDuration / 2)..escalateDuration) > 60.0 } after 60 seconds "flap-now-escalate-to-failed"

}

public fun MachineGroup.addMachineSensorRules(vararg rules: Pair<String, Double>) {

    this memberIs OK ifStateIn listOf(DEAD, BROKEN, REBUILDING, STARTING, null) andTest { machine -> rules.all { machine[it.first]?.D()?:(it.second + 1) < it.second } } after 30 seconds "machine-ok"

    this memberIs BROKEN ifStateIn listOf(OK, BROKEN, REBUILDING, STARTING, STOPPING, STOPPED, STALE, null) andTest { machine -> rules.any { machine[it.first]?.D()?:(it.second - 1) > it.second } } after 240 seconds "machine-broken"
}


fun MachineGroup.addGroupSensorRules(vararg ranges: Pair<String, Range<Double>>) {

    this becomes GROUP_BROKEN ifStateIn  listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest { it.workingSize() == 0 || it.activeSize() == 0 } after 60 seconds "group-size-dangerously-low"

    this becomes BUSY ifStateIn  listOf(QUIET, BUSY, NORMAL, null) andTest { it.workingSize() < it.min } after 180 seconds "not-enough-working-machines-in-group"

    this becomes BUSY ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest {
        ranges.any { this[it.first]?:it.second.end > it.second.end }
    }  after 180 seconds "overload"

    this becomes QUIET ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest { this.workingSize() > this.max }  after 60 seconds "too-many-machines"


    this becomes QUIET ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, NORMAL, null) andTest {
        !ranges.any { this[it.first]?:it.second.start > it.second.end } && ranges.any { this[it.first]?:it.second.start < it.second.start }
    }  after 300 seconds "underload"

    this becomes NORMAL ifStateIn listOf(GROUP_BROKEN, QUIET, BUSY, null) andTest {
        ranges.all { this[it.first]?:it.second.start in it.second }
        && this.workingSize() in this.min..this.max
    }  after 30 seconds "group-ok"

}