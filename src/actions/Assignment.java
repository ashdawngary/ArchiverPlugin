package actions;

import static utils.ZipperUtils.compressToAccumulate;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
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


  public String toZip() throws IOException {
    return toZip(null);
  }

  /**
   * Compresses actions.Assignment to Zip
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
    compressToAccumulate(zout, this.src.getPath(), rootPath, this.indicate);
    compressToAccumulate(zout, this.test.getPath(), rootPath, this.indicate);
    zout.close();

    this.indicate.ifPresent(indicator -> indicator.setText2(""));
    this.indicate.ifPresent(indicator -> indicator.setText("Done!"));

    return zipFileName;
  }


}
