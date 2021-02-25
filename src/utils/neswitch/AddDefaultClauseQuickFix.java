package utils.neswitch;


import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSwitchLabelStatement;
import com.intellij.psi.PsiSwitchStatement;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class AddDefaultClauseQuickFix implements LocalQuickFix {

  @Override
  public @IntentionName
  @NotNull String getName() {
    return "Add default case";
  }

  @Override
  public @IntentionFamilyName
  @NotNull String getFamilyName() {
    return "Switch statement fixes";
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
    PsiSwitchStatement globalSwitch = (PsiSwitchStatement) problemDescriptor.getPsiElement().getParent();
    PsiSwitchLabelStatement defaultcase = (PsiSwitchLabelStatement) JavaPsiFacade.getElementFactory(project).createStatementFromText("default: ", globalSwitch);
    PsiStatement breakstatement = JavaPsiFacade.getElementFactory(project).createStatementFromText("break;", globalSwitch);

    Optional<PsiCodeBlock> switchBody = Optional.ofNullable(globalSwitch.getBody());
    switchBody.ifPresent(body -> body.add(defaultcase));
    switchBody.ifPresent(body -> body.add(breakstatement));
  }
}
