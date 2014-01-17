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

package kontrol.webserver

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import kontrol.common.*
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import java.io.StringWriter
import org.apache.velocity.exception.ResourceNotFoundException

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class VelocityWebServer(port: Int = 8080, hostname: String? = null, val prefix: String = "", val action: (IHTTPSession, VelocityContext) -> Unit) : NanoHTTPD(hostname, port) {

    var ve = VelocityEngine();
    {

        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader().javaClass.getName());
        ve.init();

    }

    override fun serve(session: IHTTPSession): Response {
        println()
        try {

            val template = ve.getTemplate(prefix + session.uri + ".vm")
            if (template == null) {
                return Response(Status.NOT_FOUND)
            } else {
                val context = VelocityContext()
                context["uri"] = session.uri;
                context["headers"] = session.headers
                context["method"] = session.method
                context["params"] = session.parms
                context["query"] = session.queryParameterString
                context["cookies"] = session.cookies
                val writer = StringWriter();
                action(session, context)
                template.merge(context, writer)
                return Response(Status.OK, text = writer.getBuffer().toString())
            }
        } catch (rnfe: ResourceNotFoundException) {
            return Response(Status.NOT_FOUND)
        } catch (e: Exception) {
            return Response(Status.INTERNAL_ERROR, text = "$e ${e.getMessage()}")
        }
    }


}



