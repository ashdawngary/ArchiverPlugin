package utils.javadoc.quickfixes;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class MethodParamMissingQuickFix extends JavadocQuickFixBase {

  private final PsiParameter mparam;
  private final PsiDocComment jd;

  public MethodParamMissingQuickFix(PsiParameter generic, PsiDocComment jdoc) {
    this.mparam = generic;
    this.jd = jdoc;
  }

  @Override
  public @IntentionName
  @NotNull
  String getName() {
    return String.format("Add @param %s to doc", this.mparam.getName());
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
    // creates a new @param annotation in the javadoc for the generic (user to fill in)
    Optional<Editor> currentEditor = Optional
        .ofNullable(FileEditorManager.getInstance(project).getSelectedTextEditor());
    Optional<CaretModel> currentCaret = currentEditor.map(Editor::getCaretModel);

    String exprToAdd = String.format("@param %s  ", this.mparam.getName());
    PsiDocTag genDoc = JavaPsiFacade.getInstance(project).getElementFactory()
        .createDocTagFromText(exprToAdd);

    int gdL = genDoc.getTextLength() + 4;
    PsiElement[] cElements = this.jd.getChildren();
    for (int i = 0; i < cElements.length; i++) {
      if (cElements[i] instanceof PsiDocTag) {
        this.jd.addBefore(genDoc, cElements[i]);
        int finalI = i - 1;
        currentCaret.ifPresent(
            carret -> carret
                .moveToOffset(cElements[finalI].getTextOffset() + genDoc.getTextLength() - 1));
        return;
      }
    }

    int jumpTo = cElements[cElements.length - 2].getTextOffset() + gdL;

    this.jd.add(genDoc);
    currentCaret.ifPresent(carret -> carret
        .moveToOffset(jumpTo));

  }
}
