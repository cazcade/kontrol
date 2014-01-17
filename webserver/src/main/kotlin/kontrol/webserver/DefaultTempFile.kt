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

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

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
 * <p></p></[>By default, files are created by <code>File.createTempFile()</code> in
 * the directory specified.</p>
 */
public open class DefaultTempFile(tempdir: String) : TempFile {
    private var file: File
    private var fstream: OutputStream

    public override fun open(): OutputStream {
        return fstream
    }
    public override fun delete(): Unit {
        if (fstream != null)
            fstream.close()

        file.delete()
    }
    public override fun getName(): String {
        return file.getAbsolutePath()
    }
    {
        file = File.createTempFile("NanoHTTPD-", "", File(tempdir))
        fstream = FileOutputStream(file)
    }

}
