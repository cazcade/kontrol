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


public open class TextPart(val text: String) : PostmortemPart {

    public override fun toString(): String {
        return text;
    }


}

public class LogPart(text: String) : TextPart(text) {
    override fun toHTML(): String {
        val stringBuilder = StringBuilder("<ul>")
        var count = 1;
        text.split("\n").map { "<li><emp>${count++}</emp><pre>$it</pre></li>" }.appendString(stringBuilder)
        stringBuilder.append("</ul>")
        return stringBuilder.toString()
    }
}