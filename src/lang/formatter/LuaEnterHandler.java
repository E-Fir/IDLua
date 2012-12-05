package com.sylvanaar.idea.Lua.lang.formatter;

import com.intellij.codeInsight.editorActions.EnterHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

public class LuaEnterHandler extends EnterHandler {

  public LuaEnterHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  public void executeWriteAction(Editor editor, DataContext dataContext) {
    int offset = editor.getCaretModel().getOffset();
    Document document = editor.getDocument();
    CharSequence documentCharsSequence = document.getCharsSequence();
    CharSequence prevChar = documentCharsSequence.subSequence(offset - 1, offset);
    CharSequence nextChar = documentCharsSequence.subSequence(offset, offset + 1);
    CharSequence whitespace = null;
    if(prevChar.equals(",") && nextChar.equals("\n")) {
      int lineNumber = document.getLineNumber(offset);
      int lineStartOffset = document.getLineStartOffset(lineNumber);
      CharSequence line = documentCharsSequence.subSequence(lineStartOffset, offset);
      int whitespaceLength = line.length() - line.toString().trim().length();
      whitespace = line.subSequence(0, whitespaceLength);
    }

    super.executeWriteAction(editor, dataContext);

    if(whitespace != null) {
      offset = editor.getCaretModel().getOffset();
      document.replaceString(offset, offset, "a");
    }
  }
}
