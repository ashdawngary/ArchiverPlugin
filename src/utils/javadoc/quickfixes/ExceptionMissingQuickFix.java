package utils.javadoc.quickfixes;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ExceptionMissingQuickFix extends JavadocQuickFixBase {

  private final PsiJavaCodeReferenceElement exceptionType;
  private final PsiDocComment jd;

  public ExceptionMissingQuickFix(PsiJavaCodeReferenceElement exceptionType, PsiDocComment jdoc) {
    this.exceptionType = exceptionType;
    this.jd = jdoc;
  }


  @Override
  public @IntentionName
  @NotNull
  String getName() {
    return String.format("Add @throws %s to javadoc", this.exceptionType.getText());
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
    // creates a new @param annotation in the javadoc for the generic (user to fill in)
    Optional<Editor> currentEditor = Optional
        .ofNullable(FileEditorManager.getInstance(project).getSelectedTextEditor());
    Optional<CaretModel> currentCaret = currentEditor.map(Editor::getCaretModel);

    String exprToAdd = String.format("@throws %s  ", this.exceptionType.getText());
    PsiDocTag genDoc = JavaPsiFacade.getInstance(project).getElementFactory()
        .createDocTagFromText(exprToAdd);

    int gdL = genDoc.getTextLength() + 4;
    PsiElement[] cElements = this.jd.getChildren();

    int jumpTo = cElements[cElements.length - 2].getTextOffset() + gdL;

    this.jd.add(genDoc);
    currentCaret.ifPresent(carret -> carret
        .moveToOffset(jumpTo));

  }
}
