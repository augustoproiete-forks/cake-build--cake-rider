package net.cakebuild.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import net.cakebuild.shared.Constants

@State(
    name = "net.cakebuild.settings.CakeSettings",
    storages = [ Storage(Constants.settingsStorageFile) ]
)
class CakeSettings : PersistentStateComponent<CakeSettings> {
    companion object {
        fun getInstance(project: Project): CakeSettings {
            return ServiceManager.getService(project, CakeSettings::class.java)
        }
    }

    private val log = Logger.getInstance(CakeSettings::class.java)

    var cakeFileExtension = "cake"
    var cakeTaskParsingRegex = "Task\\s*?\\(\\s*?\"(.*?)\"\\s*?\\)"
    var cakeVerbosity = "normal"
    var cakeRunner = "~/.dotnet/tools/dotnet-cake"
    var cakeRunnerOverrides = mapOf(Pair("^.*windows.*$", "\${USERPROFILE}\\.dotnet\\tools\\dotnet-cake.exe"))
    var cakeScriptSearchPaths: Collection<String> = mutableListOf(".")
    var cakeScriptSearchIgnores: Collection<String> = mutableListOf(".*/tools/.*")

    fun getCurrentCakeRunner(): String {
        val os = System.getProperty("os.name")
        var runner = cakeRunner
        cakeRunnerOverrides.forEach forEach@{
            val regex = Regex(it.key, RegexOption.IGNORE_CASE)
            if (regex.matches(os)) {
                log.trace("os $os matches regex ${it.key}")
                runner = it.value
                return@forEach
            }
        }

        val variableRegex = Regex("\\\$\\{([^}]+)}")
        runner = runner.replace(variableRegex) {
            val varName = it.groupValues[1]
            val varValue = System.getenv(varName) ?: ""
            log.trace("resolved environment variable $varName to $varValue")
            varValue
        }

        log.trace("resolved Cake runner to $runner")
        return runner
    }

    override fun getState(): CakeSettings {
        return this
    }

    override fun loadState(state: CakeSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
