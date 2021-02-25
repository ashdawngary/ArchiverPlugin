package utils;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiTypeElement;
import java.util.Arrays;


public class MultiDefintionVisitor extends JavaElementVisitor {

  private final ProblemsHolder report;

  public MultiDefintionVisitor(ProblemsHolder holder) {
    this.report = holder;
  }

  @Override
  public void visitDeclarationStatement(PsiDeclarationStatement statement) {
    // make sure decl has only 1 element.
    if (statement.getDeclaredElements().length > 1) {
      report.registerProblem(statement, "Declaring (%s > 1) variables in a line.",
          ProblemHighlightType.WARNING);
    }
  }

  @Override
  public void visitField(PsiField field) {

    if (!hasModifierOrTypeParamList(field)) {
      report.registerProblem(field, "Declaring multiple variables in a line.",
          ProblemHighlightType.WARNING);
    }

    super.visitField(field);
  }

  private boolean hasModifierOrTypeParamList(PsiField field) {
    PsiElement[] declModifiers = field.getChildren();

    return Arrays.stream(declModifiers)
        .anyMatch(elem -> (elem instanceof PsiModifierList) || (elem instanceof PsiTypeElement));
  }


}
