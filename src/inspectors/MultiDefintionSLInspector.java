package inspectors;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import utils.MultiDefintionVisitor;
import utils.javadoc.JDVisitor;

public class MultiDefintionSLInspector extends LocalInspectionTool {
  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
      final boolean isOnTheFly) {
    return new MultiDefintionVisitor(holder);
  }
}
