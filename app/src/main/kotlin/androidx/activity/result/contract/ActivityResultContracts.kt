package androidx.activity.result.contract

import android.content.Context
import android.net.Uri
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

abstract class ActivityResultContract<I, O> {
    abstract fun invokeDesktop(input: I): O
}

class ActivityResultLauncher<I>(private val onLaunch: (I) -> Unit) {
    fun launch(input: I) {
        onLaunch(input)
    }
}

object ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun invokeDesktop(input: String): Boolean = true
    }

    class OpenDocumentTree : ActivityResultContract<Uri?, Uri?>() {
        override fun invokeDesktop(input: Uri?): Uri? {
            var selected: File? = null
            val task = Runnable {
                val chooser = JFileChooser(Context.DesktopInstance.filesDir).apply {
                    dialogTitle = "Select Folder"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selected = chooser.selectedFile
                }
            }
            if (SwingUtilities.isEventDispatchThread()) task.run()
            else SwingUtilities.invokeAndWait(task)
            return selected?.let { Uri.fromFile(it) }
        }
    }

    class CreateDocument(val mimeType: String = "*/*") : ActivityResultContract<String, Uri?>() {
        override fun invokeDesktop(input: String): Uri? {
            var selected: File? = null
            val task = Runnable {
                val defaultDir = Context.DesktopInstance.filesDir
                val chooser = JFileChooser(defaultDir).apply {
                    dialogTitle = "Save File"
                    selectedFile = File(defaultDir, File(input).name)
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selected = chooser.selectedFile
                }
            }
            if (SwingUtilities.isEventDispatchThread()) task.run()
            else SwingUtilities.invokeAndWait(task)
            return selected?.let { file ->
                file.parentFile?.mkdirs()
                if (!file.exists()) file.createNewFile()
                Uri.fromFile(file)
            }
        }
    }

    class GetContent : ActivityResultContract<String, Uri?>() {
        override fun invokeDesktop(input: String): Uri? {
            var selected: File? = null
            val task = Runnable {
                val defaultDir = Context.DesktopInstance.filesDir
                val chooser = JFileChooser(defaultDir).apply {
                    dialogTitle = "Open File"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selected = chooser.selectedFile
                }
            }
            if (SwingUtilities.isEventDispatchThread()) task.run()
            else SwingUtilities.invokeAndWait(task)
            return selected?.let { Uri.fromFile(it) }
        }
    }
}
