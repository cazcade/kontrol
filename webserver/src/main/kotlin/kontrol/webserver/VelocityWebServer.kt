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
import java.io.StringWriter
import org.apache.velocity.exception.ResourceNotFoundException
import org.apache.velocity.runtime.resource.loader.ResourceLoader
import org.apache.commons.collections.ExtendedProperties
import java.io.InputStream
import org.apache.velocity.runtime.resource.Resource
import org.apache.velocity.util.ExceptionUtils

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class VelocityWebServer(port: Int = 8080, hostname: String? = null, val prefix: String = "", val action: (IHTTPSession, VelocityContext) -> String) : NanoHTTPD(hostname, port) {

    var ve = VelocityEngine();
    {

        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader().javaClass.getName());
        ve.setProperty("classpath.resource.loader.prefix", prefix);
        ve.init();

    }

    override fun serve(session: IHTTPSession): Response {
        println()
        try {
            val context = VelocityContext()
            context["uri"] = session.uri;
            context["headers"] = session.headers
            context["method"] = session.method
            context["params"] = session.parms
            context["query"] = session.queryParameterString
            context["cookies"] = session.cookies
            val writer = StringWriter();
            val templateName = action(session, context)
            val template = ve.getTemplate(templateName)
            if (template == null) {
                return Response(Status.NOT_FOUND)
            } else {
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

public open class ClasspathResourceLoader() : ResourceLoader() {
    var configuration: ExtendedProperties? = null
    override fun init(configuration: ExtendedProperties?) {
        this.configuration = configuration
        if (log?.isTraceEnabled()!!) {
            log?.trace("ClasspathResourceLoader : initialization complete.")
        }
    }
    override fun getResourceStream(source: String?): InputStream? {
        if (source == null || source == "" )
        {
            throw ResourceNotFoundException("No template name provided")
        }


        try {

            val name: String = configuration?.getProperty("prefix").toString() + "/" + source;
            return getClass().getResourceAsStream(name)?:throw ResourceNotFoundException("ClasspathResourceLoader Error: cannot find resource " + source)
        } catch (fnfe: Exception) {
            throw (ExceptionUtils.createWithCause(javaClass<ResourceNotFoundException>(), "problem with template: " + source, fnfe) as ResourceNotFoundException)
        }
    }

    override fun isSourceModified(resource: Resource?): Boolean {
        return false;
    }
    override fun getLastModified(resource: Resource?): Long {
        return 0
    }


}




