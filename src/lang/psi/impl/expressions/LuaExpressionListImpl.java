/*
 * Copyright 2010 Jon S Akhtar (Sylvanaar)
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

package com.sylvanaar.idea.Lua.lang.psi.impl.expressions;

import com.intellij.lang.ASTNode;
import com.sylvanaar.idea.Lua.lang.parser.LuaElementTypes;
import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaExpression;
import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaExpressionList;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jon S Akhtar
 * Date: Jun 13, 2010
 * Time: 7:18:16 AM
 */
public class LuaExpressionListImpl extends LuaExpressionImpl implements LuaExpressionList {
    public LuaExpressionListImpl(ASTNode node) {
        super(node);
    }

    @Override
    public int count() {
        return getLuaExpressions().size();
    }


    public List<LuaExpression> getLuaExpressions() {
        return findChildrenByType(LuaElementTypes.EXPR);
    }

//    @NotNull
//    @Override
//    public PsiExpression[] getExpressions() {
//        List<?> l = getLuaExpressions();
//        return l.toArray(new LuaExpression[l.size()]);
//    }
//
//    @NotNull
//    @Override
//    public PsiType[] getExpressionTypes() {
//        return new PsiType[0];
//    }
}