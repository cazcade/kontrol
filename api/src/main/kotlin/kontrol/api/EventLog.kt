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

package kontrol.api

import javax.persistence.Entity as entity
import javax.persistence.Id as id
import javax.persistence.GeneratedValue as generated
import javax.persistence.Transient as transient
import javax.persistence.*
import java.io.Serializable
import java.util.UUID
import java.util.Date


/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

public enum class LogContextualState {
    DURING
    START
    END
}


public entity data class EventLogEntry(public var targetName: String? = null,
                                       state: Any? = null,
                                       public var contextualState: LogContextualState? = null,
                                       public var message: String? = null,
                                       public var created: Date? = Date()) : Serializable {

    id var id: String? = UUID.randomUUID().toString()

    public var state: String? = state.toString()

}

public trait EventLog {

    fun  log(targetName: String, state: Any?, contextState: LogContextualState = LogContextualState.DURING, message: String = "")

    fun last(n: Int = 100): List<EventLogEntry>

}