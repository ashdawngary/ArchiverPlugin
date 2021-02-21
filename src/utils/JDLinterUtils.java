package utils;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class JDLinterUtils {

  public static List<Issues> checkJavadoc(PsiDirectory cd) {
    List<Issues> is = new LinkedList<>();
    for (PsiDirectory subdir : cd.getSubdirectories()) {
      is.addAll(checkJavadoc(subdir));
    }

    is.addAll(Arrays.stream(cd.getFiles()).filter(fi -> fi instanceof PsiJavaFile)
        .map(jfi -> checkJavadoc((PsiJavaFile) jfi)).flatMap(Collection::stream).collect(
            Collectors.toList()));
    return is;
  }


  /**
   * Check Javadoc over a PsiJavaFile for issues.
   *
   * @param jf psijavafile to analyze.
   */
  private static List<Issues> checkJavadoc(PsiJavaFile jf) {
    PsiElement[] fileElements = jf.getChildren();
    return Arrays.stream(fileElements).filter(elem -> elem instanceof PsiClass)
        .map(jclass -> checkJavadoc((PsiClass) jclass)).flatMap(
            Collection::stream).collect(
            Collectors.toList());
  }

  /**
   * Check Javadoc over a PsiClass for issues.
   *
   * @param cl PsiClass to analyze.
   */
  private static List<Issues> checkJavadoc(PsiClass cl) {
    PsiElement[] c = cl.getChildren();
    List<Issues> visibleProblems = new LinkedList<Issues>();

    if (c[0] instanceof PsiDocComment) {
      visibleProblems.addAll(checkClassJavaDoc(cl.getQualifiedName(), (PsiDocComment) c[0]));
    } else {
      visibleProblems
          .add(new Issues(String.format("Missing javadoc class descr for: %s", cl.getName())));
    }

    visibleProblems.addAll(Arrays.stream(c).filter(elem -> elem instanceof PsiMethod)
        .map(method -> checkJavadoc(cl.getQualifiedName(), (PsiMethod) method)).flatMap(Collection::stream)
        .collect(Collectors.toList()));
    return visibleProblems;
  }

  private static List<Issues> checkJavadoc(String className, PsiMethod mth) {

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
      return new LinkedList<>();
    } else if (!(children[0] instanceof PsiDocComment)) {
      LinkedList<Issues> noJD = new LinkedList<>();
      noJD.add(new Issues("unable to find javadoc for: " + mth.getName()));
      return noJD;
    }

    List<Issues> initProblems = checkMethodJavaDoc(className, mth.getName(), (PsiDocComment) children[0]);

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

    List<PsiJavaCodeReferenceElement> exeThrown = Arrays.stream(children)
        .filter(child -> child instanceof PsiReferenceList).map(PsiElement::getChildren)
        .flatMap(Arrays::stream).filter(child -> child instanceof PsiReferenceList)
        .map(child -> (PsiJavaCodeReferenceElement) child)
        .collect(Collectors.toList());

    initProblems.addAll( checkMethodJavaDoc((PsiDocComment) children[0], methodParams, typebound, exeThrown));
    return initProblems;

  }

  private static List<Issues> checkMethodJavaDoc(PsiDocComment jdele, List<PsiParameter> methodParams,
      List<PsiTypeParameter> typebound, List<PsiJavaCodeReferenceElement> exeThrown) {
    //TODO: cross-check jdele against signature for all elements.

    LinkedList<Issues> methodIssues = new LinkedList<>();

    List<PsiDocTag> jdTags = Arrays.stream(jdele.getChildren())
        .filter(child -> child instanceof PsiDocTag)
        .map(child -> (PsiDocTag) child).collect(Collectors.toList());

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

      String foundDesc = "";
      if (tAnnotate.equals("@param")) {
        if (p.getChildren().length == 3) {
          // its a type param
          foundGeneric.add(p.getChildren()[1].getText());
          foundDesc = "Typebound-generic " + p.getChildren()[1].getText();
        } else if (p.getChildren().length == 1) {
          foundParams.add(p.getChildren()[0].getText());
          foundDesc = "method param " + p.getChildren()[0].getText();
        }
      } else if (tAnnotate.equals("@throws")) {
        String exceptionName = p.getChildren()[0].getText();
        foundExcept.add(p.getChildren()[0].getText());
        foundDesc = "exception type " + exceptionName;
      } else {
        // unsure
        continue;
      }

      String desc = getCommentFromDocTag(b);
      if (desc.length() < 3) {
        methodIssues.add(new Issues(String.format("No javadoc desc for %s %s", foundDesc, desc)));
      }

    }

    for (PsiParameter mparam : methodParams) {
      if (!foundParams.contains(mparam.getName())) {
        methodIssues
            .add(new Issues("failed to find @param annotation for: " + mparam.getName()));
      }
    }
    for (PsiTypeParameter genericParam : typebound) {
      if (!foundGeneric.contains(genericParam.getName())) {
        methodIssues
            .add(new Issues("failed to find @param annotation for: " + genericParam.getName()));
      }
    }

    for (PsiJavaCodeReferenceElement jcr : exeThrown) {
      if (!foundExcept.contains(jcr.getText())) {
        methodIssues
            .add(new Issues("failed to find @throws annotation for: " + jcr.getText()));
      }
    }
    return methodIssues;
  }

  private static String getCommentFromDocTag(PsiDocTag b) {
    StringBuilder commentBuilder = new StringBuilder();
    PsiElement[] elems = b.getDataElements();
    for (int cix = 1; cix < elems.length; cix++) {
      commentBuilder.append(elems[cix].getText().replace('\n', ' ').replace('\r', ' '));
    }
    return commentBuilder.toString();
  }

  private static boolean isOverrideOrTest(PsiJavaCodeReferenceElement annotate) {
    return Arrays.stream(annotate.getChildren()).filter(child -> child instanceof PsiIdentifier)
        .map(child -> ((PsiIdentifier) child)).map(PsiElement::getText)
        .anyMatch(data -> data.equals("Override") || data.equals("Test"));
  }

  private static List<Issues> checkMethodJavaDoc(String className, String methodName, PsiDocComment psiDocComment) {
    // TODO: Looking for Doc_COMMENT_DATA, and then looking for a period at the end to end the sentence.
    PsiElement[] dc = psiDocComment.getDescriptionElements();
    StringBuilder fulldesc = new StringBuilder("");
    for(PsiElement d : dc){
      fulldesc.append(d.getText());
    }
    String result = fulldesc.toString().replace('\n',' ').replace('\r', ' ');

    boolean isSubstantial = result.length() > 5;
    boolean hasPeriod = result.contains(".") || result.contains("?");
    LinkedList<Issues> cdi =  new LinkedList<>();
    if(!isSubstantial){
      cdi.add(new Issues(String.format("empty javadoc for: %s::%s ", className, methodName)));
    }
    else if(!hasPeriod){
      cdi.add(new Issues(String.format("Not a full sentence: %s::%s (%s)", className, methodName, result)));
    }
    return cdi;
  }

  private static List<Issues> checkClassJavaDoc(String className, PsiDocComment psiDocComment) {
    // TODO: Looking for Doc_COMMENT_DATA, and then looking for a period at the end to end the sentence.
    PsiElement[] dc = psiDocComment.getDescriptionElements();
    StringBuilder fulldesc = new StringBuilder();
    for(PsiElement d : dc){
      fulldesc.append(d.getText());
    }

    String result = fulldesc.toString().replace('\n',' ').replace('\r', ' ');

    boolean isSubstantial = result.length() > 5;
    boolean hasPeriod = result.contains(".") || result.contains("?");
    LinkedList<Issues> cdi =  new LinkedList<>();
    if(!isSubstantial){
      cdi.add(new Issues("empty javadoc for " + className));
    }
    else if(!hasPeriod){
      cdi.add(new Issues("not a full sentence for: " + className + " (" + result+" )"));
    }
    return cdi;
  }
}
