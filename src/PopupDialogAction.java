import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.search.FilenameIndex;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;


public class PopupDialogAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    // Using the event, evaluate the context, and enable or disable the action.
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    System.out.println("action invoked");
    Project currentProject = Objects.requireNonNull(event.getProject());

    VirtualFile[] toplevel = ProjectRootManager.getInstance(currentProject).getContentRoots();

    if (toplevel.length > 1) {
      // too many files
      return;
    }

    Assignment handinsSubmit = new Assignment(toplevel[0], currentProject);


  }

}
