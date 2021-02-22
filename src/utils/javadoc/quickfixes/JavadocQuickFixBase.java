package utils.javadoc.quickfixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.util.IntentionFamilyName;
import org.jetbrains.annotations.NotNull;

public abstract class JavadocQuickFixBase implements LocalQuickFix {

  @Override
  public @IntentionFamilyName
  @NotNull
  String getFamilyName() {
    return "Javadoc-fix";
  }

}
