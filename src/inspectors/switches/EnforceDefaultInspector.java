package inspectors.switches;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import utils.neswitch.EnforceDefaultVisitor;

public class EnforceDefaultInspector extends LocalInspectionTool {

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
      final boolean isOnTheFly) {
    return new EnforceDefaultVisitor(holder);
  }

}
