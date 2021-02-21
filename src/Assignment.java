import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * lmao imagine using javadoc
 */
public class Assignment {

  private final @NotNull
  Project proj;
  private final @NotNull
  VirtualFile src;
  private final @NotNull
  VirtualFile test;
  private final @NotNull
  VirtualFile root;
  private final @NotNull
  PsiManager psiManager;

  private Optional<ProgressIndicator> indicate;

  public Assignment(@NotNull VirtualFile root, @NotNull Project proj) throws NullPointerException {
    this.proj = Objects.requireNonNull(proj, "missing project file");
    this.root = Objects.requireNonNull(root, "missing root file");
    this.src = Objects
        .requireNonNull(root.findFileByRelativePath("./src"), "missing testing directory");
    this.test = Objects
        .requireNonNull(root.findFileByRelativePath("./test"), "missing sources directory");
    this.psiManager = PsiManager.getInstance(this.proj);
  }


  public void remedyJavadoc() {
    PsiDirectory psiSrc = Objects.requireNonNull(psiManager.findDirectory(this.src),
        "Missing physical manifestation of src/");
    PsiDirectory psiTest = Objects.requireNonNull(psiManager.findDirectory(this.test),
        "Missing physical manifestation of test/");
    List<Issues> srcIssues = checkJavadoc(psiSrc);
    List<Issues> tstIssues = checkJavadoc(psiTest);

    for (Issues issue : srcIssues) {
      System.out.println("src/ " + issue.toString());
    }

    for (Issues issue : tstIssues) {
      System.out.println("test/ " + issue.toString());
    }
  }

  private List<Issues> checkJavadoc(PsiDirectory cd) {
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
  private List<Issues> checkJavadoc(PsiJavaFile jf) {
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
  private List<Issues> checkJavadoc(PsiClass cl) {
    PsiElement[] c = cl.getChildren();
    List<Issues> visibleProblems = new LinkedList<Issues>();

    if (c[0] instanceof PsiDocComment) {
      visibleProblems.addAll(checkClassJavaDoc((PsiDocComment) c[0]));
    } else {
      visibleProblems
          .add(new Issues(String.format("Missing javadoc class descr for: %s", cl.getName())));
    }

    visibleProblems.addAll(Arrays.stream(c).filter(elem -> elem instanceof PsiMethod)
        .map(method -> checkJavadoc((PsiMethod) method)).flatMap(Collection::stream)
        .collect(Collectors.toList()));
    return visibleProblems;
  }

  private List<Issues> checkJavadoc(PsiMethod mth) {

    PsiElement[] children = mth.getChildren();
    boolean hasOverrideOrTest = Arrays.stream(children)
        .filter(child -> child instanceof PsiModifierList)
        .map(PsiElement::getChildren).flatMap(Arrays::stream)
        .filter(child -> child instanceof PsiAnnotation)
        .map(PsiElement::getChildren).flatMap(Arrays::stream)
        .filter(child -> child instanceof PsiJavaCodeReferenceElement)
        .map(child -> (PsiJavaCodeReferenceElement) child)
        .anyMatch(this::isOverrideOrTest);

    if (hasOverrideOrTest) { // assume annotation will be impl in the override.
      return new LinkedList<>();
    } else if (!(children[0] instanceof PsiDocComment)) {
      LinkedList<Issues> noJD = new LinkedList<>();
      noJD.add(new Issues("unable to find javadoc for: " + mth.getName()));
      return noJD;
    }

    List<Issues> initProblems = checkClassJavaDoc((PsiDocComment) children[0]);

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

  private List<Issues> checkMethodJavaDoc(PsiDocComment jdele, List<PsiParameter> methodParams,
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

      if (b.getChildren()[2].getText().length() < 3) {
        methodIssues.add(new Issues(String.format("No javadoc desc for %s", foundDesc)));
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

  private boolean isOverrideOrTest(PsiJavaCodeReferenceElement annotate) {
    return Arrays.stream(annotate.getChildren()).filter(child -> child instanceof PsiIdentifier)
        .map(child -> ((PsiIdentifier) child)).map(PsiElement::getText)
        .anyMatch(data -> data.equals("Override") || data.equals("Test"));
  }


  private List<Issues> checkClassJavaDoc(PsiDocComment psiDocComment) {
    // TODO: Looking for Doc_COMMENT_DATA, and then looking for a period at the end to end the sentence.
    return new LinkedList<>();
  }


  public String toZip() throws IOException {
    return toZip(null);
  }

  /**
   * Compresses Assignment to Zip
   *
   * @param providedIndicator progress bar for notifs.
   * @return string path to zip
   * @throws IOException if zipbuffer crashes :(
   */
  public String toZip(@Nullable ProgressIndicator providedIndicator) throws IOException {
    this.indicate = Optional.ofNullable(providedIndicator);

    final Path rootPath = Paths.get(Objects.requireNonNull(this.root.getPath()));
    final Path projectPath = Paths.get(Objects.requireNonNull(this.proj.getBasePath()));
    final String zipFileName = Paths.get(projectPath.toString(), this.proj.getName() + ".zip")
        .toString();

    this.indicate
        .ifPresent(indicator -> indicator.setText("Making " + this.proj.getName() + ".zip"));

    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFileName));
    compressToAccumulate(zout, this.src.getPath(), rootPath);
    compressToAccumulate(zout, this.test.getPath(), rootPath);
    zout.close();

    this.indicate.ifPresent(indicator -> indicator.setText2(""));
    this.indicate.ifPresent(indicator -> indicator.setText("Done!"));

    return zipFileName;
  }

  /**
   * Compresses a specific dir from the target folder
   *
   * @param zf           zip buffer
   * @param localRoot    root to start compressing from
   * @param trueRootPath true root of workspace (to compute relative paths)
   */
  private void compressToAccumulate(ZipOutputStream zf, String localRoot, Path trueRootPath) {
    final Path localRootPath = Paths.get(localRoot);
    final Optional<ProgressIndicator> pi = this.indicate;
    try {
      Files.walkFileTree(localRootPath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes ignored) {
          try {
            Path targetFile = trueRootPath.relativize(file);
            pi.ifPresent(indicator -> indicator.setText2(targetFile.toString()));
            zf.putNextEntry(new ZipEntry(targetFile.toString()));
            byte[] bytes = Files.readAllBytes(file);
            zf.write(bytes, 0, bytes.length);
            zf.closeEntry();
          } catch (IOException e) {
            e.printStackTrace();
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
