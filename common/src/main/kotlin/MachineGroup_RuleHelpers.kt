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

import kontrol.api.MachineGroup
import kontrol.api.MachineGroupState.*
import kontrol.api.MachineState.*

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
    this allowMachine (BROKEN to STOPPING);
    this allowMachine (BROKEN to STOPPED);
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