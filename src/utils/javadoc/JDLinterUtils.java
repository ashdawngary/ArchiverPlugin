package utils.javadoc;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import utils.javadoc.quickfixes.ExceptionMissingQuickFix;
import utils.javadoc.quickfixes.GenericParamMissingQuickFix;
import utils.javadoc.quickfixes.MethodParamMissingQuickFix;

public class JDLinterUtils {


  /**
   * Checks Javadoc for A Java class.
   *
   * @param cl     class to analyze
   * @param report reporter
   */
  public static void checkJavadoc(PsiClass cl, ProblemsHolder report) {
    PsiElement[] c = cl.getChildren();
    if (c[0] instanceof PsiDocComment) {
      checkClassJavaDoc((PsiDocComment) c[0], report);
    } else {
      report.registerProblem(Objects.requireNonNull(cl.getIdentifyingElement()),
          "Missing class description javadoc", ProblemHighlightType.WARNING);
    }
  }

  public static void checkJavadoc(PsiMethod mth, ProblemsHolder report) {

    PsiElement[] children = mth.getChildren();
    boolean hasOverrideOrTest = Arrays.stream(children)
        .filter(child -> child instanceof PsiModifierList)
        .map(PsiElement::getChildren).flatMap(Arrays::stream)
        .filter(child -> child instanceof PsiAnnotation)
        .map(PsiElement::getChildren).flatMap(Arrays::stream)
        .filter(child -> child instanceof PsiJavaCodeReferenceElement)
        .map(child -> (PsiJavaCodeReferenceElement) child)
        .anyMatch(JDLinterUtils::isOverrideOrTest);

    if (hasOverrideOrTest) { // assume annotation will be impl in the override.
      return;
    } else if (!(children[0] instanceof PsiDocComment)) {
      report.registerProblem(Objects.requireNonNull(mth.getIdentifyingElement()),
          "Missing method javadoc");
      return;
    }

    checkMethodJavaDocDescription((PsiDocComment) children[0], report);

    List<PsiTypeParameter> typebound = Arrays.stream(children)
        .filter(child -> child instanceof PsiTypeParameterList).map(
            PsiElement::getChildren).flatMap(Arrays::stream)
        .filter(elem -> elem instanceof PsiTypeParameter)
        .map(elem -> (PsiTypeParameter) elem).collect(Collectors.toList());

    List<PsiParameter> methodParams = Arrays.stream(children)
        .filter(child -> child instanceof PsiParameterList).map(PsiElement::getChildren)
        .flatMap(Arrays::stream).filter(child -> child instanceof PsiParameter)
        .map(child -> (PsiParameter) child).collect(
            Collectors.toList());

    PsiReferenceList exeThrown = mth.getThrowsList();

    checkJavadocComplete((PsiDocComment) children[0], methodParams, typebound, exeThrown, report);

  }

  private static void checkJavadocComplete(PsiDocComment jdele,
      List<PsiParameter> methodParams,
      List<PsiTypeParameter> typebound, PsiReferenceList exeThrown, ProblemsHolder report) {

    PsiDocTag[] jdTags = jdele.getTags();
    LinkedList<String> foundParams = new LinkedList<>();
    LinkedList<String> foundGeneric = new LinkedList<>();
    LinkedList<String> foundExcept = new LinkedList<>();

    for (PsiDocTag b : jdTags) {
      String tAnnotate = b.getNameElement().getText();
      PsiElement p = b.getValueElement();
      if (p == null) {
        // no tag attached, we have issues.
        continue;
      }

      if (tAnnotate.equals("@param")) {
        if (p.getChildren().length == 3) {
          foundGeneric.add(p.getChildren()[1].getText());
        } else if (p.getChildren().length == 1) {
          foundParams.add(b.getValueElement().getText());
        }
      } else if (tAnnotate.equals("@throws")) {
        PsiElement maybeException = b.getValueElement().getFirstChild().getFirstChild();
        if (maybeException instanceof PsiJavaCodeReferenceElement) {
          foundExcept.add(((PsiJavaCodeReferenceElement) maybeException).getQualifiedName());
        } else {
          System.out.println("couldnt figure out the value element: " + maybeException.getText());
        }
      }
    }

    for (PsiParameter mparam : methodParams) {
      if (!foundParams.contains(mparam.getName())) {
        report.registerProblem(mparam, "No corresponding @param javadoc",
            new MethodParamMissingQuickFix(mparam, jdele));
      }
    }

    for (PsiTypeParameter genericParam : typebound) {
      if (!foundGeneric.contains(genericParam.getName())) {
        report.registerProblem(genericParam, "No corresponding @param javadoc",
            new GenericParamMissingQuickFix(genericParam, jdele));
      }
    }

    for (PsiJavaCodeReferenceElement jcr : exeThrown.getReferenceElements()) {
      if (!foundExcept.contains(jcr.getQualifiedName())) {
        report.registerProblem(jcr, "No corresponding @throws javadoc",
            new ExceptionMissingQuickFix(jcr, jdele));
      }
    }
  }

  /**
   * Checks if annotation is override or test (which dont need to be javadoc'd)
   *
   * @param annotate annoation AST node
   * @return whether its override or test
   */
  private static boolean isOverrideOrTest(PsiJavaCodeReferenceElement annotate) {
    return Arrays.stream(annotate.getChildren()).filter(child -> child instanceof PsiIdentifier)
        .map(child -> ((PsiIdentifier) child)).map(PsiElement::getText)
        .anyMatch(data -> data.equals("Override") || data.equals("Test"));
  }


  private static void checkMethodJavaDocDescription(PsiDocComment psiDocComment,
      ProblemsHolder report) {

    PsiElement[] dc = psiDocComment.getDescriptionElements();
    StringBuilder fulldesc = new StringBuilder();

    Arrays.stream(dc).forEach(psiElement -> fulldesc.append(psiElement.getText()));

    String result = fulldesc.toString().replace('\n', ' ').replace('\r', ' ');
    boolean isSubstantial = result.length() > 5;
    boolean hasPeriod = result.contains(".") || result.contains("?");

    if (!isSubstantial) {
      report.registerProblem(psiDocComment, "Empty javadoc");
    } else if (!hasPeriod) {
      report.registerProblem(psiDocComment, "Missing punctuation in javadoc.");
    }
  }

  private static void checkClassJavaDoc(PsiDocComment psiDocComment, ProblemsHolder report) {
    PsiElement[] dc = psiDocComment.getDescriptionElements();
    StringBuilder fulldesc = new StringBuilder();
    for (PsiElement d : dc) {
      fulldesc.append(d.getText());
    }

    String result = fulldesc.toString().replace('\n', ' ').replace('\r', ' ');
    boolean isSubstantial = result.length() > 5;
    boolean hasPeriod = result.contains(".") || result.contains("?");
    if (!isSubstantial) {
      report.registerProblem(psiDocComment, "Empty javadoc");
    } else if (!hasPeriod) {
      report.registerProblem(psiDocComment, "Missing punctuation in javadoc.");
    }
  }
}
