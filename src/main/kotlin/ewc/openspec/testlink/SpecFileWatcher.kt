package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile

class SpecFileWatcher : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.messageBus.connect(project).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file: VirtualFile = event.newFile ?: return
                    if (file.extension == "kt" || file.extension == "java") {
                        DaemonCodeAnalyzer.getInstance(project).restart()
                    }
                }
            }
        )
    }
}
