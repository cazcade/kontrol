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
package kontrol.postmortem

import kontrol.api.PostmortemPart
import javax.persistence.Entity as entity
import javax.persistence.Id as id
import javax.persistence.GeneratedValue as generated


public open entity  class TextPart(key: String? = null, text: String? = null) : PostmortemPart(key, text) {

    public override fun toString(): String {
        return v?:"";
    }

    override fun toHTML(): String {
        return "<pre>$v</pre>"
    }


}

public entity class LogPart(key: String? = null, text: String? = null) : TextPart(key, text) {

    override fun toHTML(): String {
        val stringBuilder = StringBuilder("<div>")
        var count = 1;
        v?.split("\n")?.map { "<div class='row ${when {it.contains("WARN") -> "warning";it.contains("ERROR") -> "error";it.contains("INFO") -> "success" else -> ""}}'><div class='col-md-1'>${count++}</div><div class='col-md-11'>$it</div></div>" }?.appendString(stringBuilder, separator = "")
        stringBuilder.append("</div>")
        return stringBuilder.toString()
    }
}