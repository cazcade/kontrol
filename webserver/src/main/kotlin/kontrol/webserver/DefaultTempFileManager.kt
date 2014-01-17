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

import java.util.ArrayList

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
 * Default strategy for creating and cleaning up temporary files.
 * <p/>
 * <p></p>This class stores its files in the standard location (that is,
 * wherever <code>java.io.tmpdir</code> points to).  Files are added
 * to an internal list, and deleted when no longer needed (that is,
 * when <code>clear()</code> is invoked at the end of processing a
 * request).</p>
 */
public open class DefaultTempFileManager() : TempFileManager {
    private val tmpdir: String
    private val tempFiles: MutableList<TempFile>
    public override fun createTempFile(): TempFile {
        val tempFile = DefaultTempFile(tmpdir)
        tempFiles.add(tempFile)
        return tempFile
    }
    public override fun clear(): Unit {
        for (file in tempFiles)
        {
            try
            {
                file.delete()
            }
            catch (ignored: Exception) {

            }

        }
        tempFiles.clear()
    }
    {
        tmpdir = System.getProperty("java.io.tmpdir")?:"/tmp"
        tempFiles = ArrayList<TempFile>()
    }

}
