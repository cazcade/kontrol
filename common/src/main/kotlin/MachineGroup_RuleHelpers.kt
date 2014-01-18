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

package kontrol.common

import kontrol.api.MachineGroup.Recheck.*
import kontrol.api.MachineGroupState.*
import kontrol.api.GroupAction.*
import kontrol.api.Action.*
import kontrol.api.MachineState.*
import kontrol.api.Controller
import kontrol.api.MachineGroup
import kontrol.api.PostmortemResult
import java.io.Serializable

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public fun MachineGroup.allowDefaultTranstitions() {

    this allowMachine (STARTING to OK);
    this allowMachine (STARTING to BROKEN);
    this allowMachine (STARTING to STOPPED);
    this allowMachine (OK to STOPPING);
    this allowMachine (OK to STOPPED);
    this allowMachine (OK to BROKEN);
    this allowMachine (OK to STALE);
    this allowMachine (STOPPING to STOPPED);
    this allowMachine (STOPPING to STARTING);
    this allowMachine (STOPPED to DEAD);
    this allowMachine (BROKEN to OK);
    this allowMachine (BROKEN to DEAD);
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


public fun MachineGroup.applyDefaultPolicies(controller: Controller, postmortemAction: (List<PostmortemResult>) -> Serializable) {

    this whenMachine BROKEN recheck THEN tell controller takeActions L(POSTMORTEM, RESTART_MACHINE);
    this whenMachine DEAD recheck THEN tell controller  takeActions L(POSTMORTEM, REIMAGE_MACHINE) ;
    this whenMachine STALE recheck THEN tell controller   takeAction REIMAGE_MACHINE;
    this whenGroup BUSY recheck THEN use controller to EXPAND;
    this whenGroup QUIET recheck THEN use controller  to CONTRACT;
    this whenGroup GROUP_BROKEN recheck THEN use controller  to EMERGENCY_FIX;

    controller will { this.failAction(it) { this.reImage(it) } ;java.lang.String() } to REIMAGE_MACHINE inGroup this;
    controller will { this.failAction(it) { this.restart(it) };java.lang.String() } to RESTART_MACHINE inGroup this;
    controller will { postmortemAction(this.postmortem(it)) } to POSTMORTEM inGroup this;
    controller use { this.expand();;java.lang.String() } to EXPAND  unless { this.activeSize() >= this.max }  group this;
    controller use { this.contract();;java.lang.String() } to CONTRACT unless { this.activeSize() <= this.min } group this;
    controller use { it.machines().forEach { this.reImage(it) };java.lang.String() } to EMERGENCY_FIX group this;
}


fun MachineGroup.selectStateUsingSensorValues(vararg ranges: Pair<String, Range<Double>>) {

    this becomes BUSY ifStateIn L(QUIET, BUSY, NORMAL, null) andTest {
        ranges.any { this[it.first]?:it.second.start > it.second.end }
        this.workingSize() < this.min
    }  after 20 checks "overload"

    this becomes QUIET ifStateIn L(QUIET, BUSY, NORMAL, null) andTest {
        ranges.any { this[it.first]?:it.second.start < it.second.start }
        this.activeSize() > this.max
    }  after 120 checks "underload"

    this becomes NORMAL ifStateIn L(QUIET, BUSY, null) andTest {
        ranges.all { this[it.first]?:it.second.start in it.second }
        && this.activeSize() in this.min..this.max

    }  after 5 checks "group-ok"

}