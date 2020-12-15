package net.cakebuild.toolwindow

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.Tree
import net.cakebuild.shared.CakeDataKeys
import net.cakebuild.shared.CakeProject
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class CakeTasksWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val tree: Tree = Tree()

    init {
        val scrollPane = ScrollPaneFactory.createScrollPane(tree)
        setContent(scrollPane)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        refreshTree()
        initToolbar()
        tree.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (e?.clickCount == 2) {
                        runTask()
                    }
                    super.mouseClicked(e)
                }
            }
        )
    }

    private fun initToolbar() {
        val actionManager = ActionManager.getInstance()
        val actionToolbar =
            actionManager.createActionToolbar(
                "Cake Tasks Toolbar",
                actionManager.getAction("CakeTasksWindow") as ActionGroup,
                true
            )
        actionToolbar.setTargetComponent(this)
        toolbar = actionToolbar.component
    }

    fun expandAll() {
        expandCollapse(true)
    }

    fun collapseAll() {
        expandCollapse(false)
    }

    private fun expandCollapse(expand: Boolean, path: TreePath? = null) {
        if (path == null) {
            expandCollapse(expand, TreePath(tree.model.root))
            return
        }

        val node = path.lastPathComponent as TreeNode
        for (it in node.children()) {
            val nextPath = path.pathByAddingChild(it)
            expandCollapse(expand, nextPath)
        }

        if (expand) {
            if (!tree.isExpanded(path)) {
                tree.expandPath(path)
            }
        } else {
            if (!tree.isCollapsed(path)) {
                tree.collapsePath(path)
            }
        }
    }

    private fun getSelectedTask(): CakeProject.CakeTask? {
        val selected = tree.getSelectedNodes(DefaultMutableTreeNode::class.java) { it.isLeaf }.firstOrNull()
            ?: return null
        return selected.userObject as CakeProject.CakeTask
    }

    fun isTaskSelected(): Boolean {
        return getSelectedTask() != null
    }

    fun runTask() {
        val task = getSelectedTask() ?: return
        task.run(CakeProject.CakeTaskRunMode.Run)
    }

    fun createRunConfig() {
        val task = getSelectedTask() ?: return
        task.run(CakeProject.CakeTaskRunMode.SaveConfigOnly)
    }

    fun refreshTree() {
        val rootNode = DefaultMutableTreeNode(project.name)
        val cakeProject = CakeProject(project)

        for (cakeFile in cakeProject.getCakeFiles()) {
            val fileNode = DefaultMutableTreeNode(cakeFile.file.name)
            rootNode.add(fileNode)

            for (task in cakeFile.getTasks()) {
                val taskNode = DefaultMutableTreeNode(task)
                fileNode.add(taskNode)
            }
        }

        tree.model = DefaultTreeModel(rootNode)
    }

    override fun getData(dataId: String): Any? {
        if (CakeDataKeys.TASKS_WINDOW.`is`(dataId)) return this
        return super.getData(dataId)
    }
}
