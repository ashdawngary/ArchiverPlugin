package utils.javadoc;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public class JDVisitor extends JavaElementVisitor {


  private final @NotNull
  ProblemsHolder problemReport;

  public JDVisitor(
      @NotNull ProblemsHolder problemReport) {
    this.problemReport = problemReport;
  }

  @Override
  public void visitClass(PsiClass jc) {
    if (jc.getParent() instanceof PsiJavaFile) {
      JDLinterUtils.checkJavadoc(jc, problemReport);
    }
  }

  @Override
  public void visitMethod(PsiMethod mth) {
    if (mth.getParent() instanceof PsiClass) {
      JDLinterUtils.checkJavadoc(mth, problemReport);
    }
  }
}