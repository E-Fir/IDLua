/*
* Copyright 2011 Jon S Akhtar (Sylvanaar)
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

/*
* User: anna
* Date: 30-Nov-2009
*/
package com.sylvanaar.idea.Lua.copyright;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.options.LanguageOptions;
import com.maddyhome.idea.copyright.psi.UpdateCopyright;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;

public class UpdateLuaCopyrightsProvider extends UpdateCopyrightsProvider {
    @Override
    public UpdateCopyright createInstance(Project project, Module module, VirtualFile file, FileType base,
                                          CopyrightProfile options) {
        return new UpdateLuaFileCopyright(project, module, file, options);
    }

    @Override
    public LanguageOptions getDefaultOptions() {
        LanguageOptions options = super.getDefaultOptions();

        options.setFiller('=');
        options.setBlock(false);
        options.setPrefixLines(false);
        options.setFileTypeOverride(LanguageOptions.USE_TEXT);

        return options;
    }
}