import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Iterate through all class' n methods and make sure either its @Override or its javadoc'd
 */
public class JavadocCheck extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    // Using the event, evaluate the context, and enable or disable the action.
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null);
  }


  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project currentProject = Objects.requireNonNull(event.getProject());
    VirtualFile[] toplevel = ProjectRootManager.getInstance(currentProject).getContentRoots();
    try{
      Assignment assign = new Assignment(toplevel[0], currentProject);
      assign.checkJavaDoc();
    }catch(NullPointerException e){
      System.out.println(e.getMessage());
    }
  }
}
