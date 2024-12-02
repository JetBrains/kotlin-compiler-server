import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirMutableDeclarationContainer
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.utils.SimpleUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import java.nio.file.Path

/**
 * Translate public API of the given [sourceFile] to Swift.
 * [stdlibPath] is a path to stdlib.klib which is required to properly resolve references from [sourceFile].
 */
fun runSwiftExport(
    sourceFile: Path,
    stdlibPath: Path?
): String {
    val (ktModule, sources) = collectModuleAndSources(sourceFile, "Playground", stdlibPath)

    return analyze(ktModule) {
        val pkgModule = buildModule {
            name = "pkg"
        }
        val unsupportedDeclarationReporter = SimpleUnsupportedDeclarationReporter()
        val sirSession = PlaygroundSirSession(ktModule, pkgModule, unsupportedDeclarationReporter, targetPackageFqName = null)
        val sirModule: SirModule = with(sirSession) {
            ktModule.sirModule().also {
                sources.flatMap { file ->
                    file.symbol.fileScope.extractDeclarations(useSiteSession)
                }.forEach { topLevelDeclaration ->
                    val parent = topLevelDeclaration.parent as? SirMutableDeclarationContainer
                        ?: error("top level declaration can contain only module or extension to package as a parent")
                    parent.addChild { topLevelDeclaration }
                }
            }
        }
        SirAsSwiftSourcesPrinter.print(
            sirModule,
            stableDeclarationsOrder = true,
            renderDocComments = true,
            emptyBodyStub = SirFunctionBody(
                listOf("stub()")
            )
        )
    }
}

private fun collectModuleAndSources(
    sourceRoot: Path,
    kotlinModuleName: String,
    stdlibPath: Path?,
): Pair<KaModule, List<KtFile>> {
    val analysisAPISession = buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = NativePlatforms.unspecifiedNativePlatform

            val stdlib = stdlibPath?.let {
                addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(it)
                        platform = NativePlatforms.unspecifiedNativePlatform
                        libraryName = "stdlib"
                    }
                )
            }

            addModule(
                buildKtSourceModule {
                    addSourceRoot(sourceRoot)
                    platform = NativePlatforms.unspecifiedNativePlatform
                    moduleName = kotlinModuleName
                    if (stdlib != null) {
                        addRegularDependency(stdlib)
                    }
                }
            )
        }
    }

    val (sourceModule, rawFiles) = analysisAPISession.modulesWithFiles.entries.single()
    return sourceModule to rawFiles.filterIsInstance<KtFile>()
}