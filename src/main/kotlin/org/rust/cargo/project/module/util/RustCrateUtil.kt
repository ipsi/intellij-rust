package org.rust.cargo.project.module.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.rust.cargo.CargoProjectDescription
import org.rust.cargo.project.module.persistence.CargoModuleService
import org.rust.cargo.util.getService
import org.rust.lang.core.psi.RustModItem
import org.rust.lang.core.psi.impl.rustMod
import java.io.File

object RustCrateUtil

fun Module.getSourceRoots(includingTestRoots: Boolean = false): Collection<VirtualFile> =
    ModuleRootManager.getInstance(this).getSourceRoots(includingTestRoots).toList()

val Module.crateRoots: Sequence<RustModItem>
    get() = crateRootFiles.asSequence()
        .mapNotNull { PsiManager.getInstance(project).findFile(it)?.rustMod }

fun Module.isCrateRootFile(file: VirtualFile): Boolean =
    crateRootFiles.contains(file)

fun Module.relativise(f: VirtualFile): String? =
    getSourceRoots()
        .find {
            FileUtil.isAncestor(it.path, f.path, /* strict = */ false)
        }
        ?.let {
            FileUtil.getRelativePath(it.canonicalPath!!, f.canonicalPath!!, '/')
        }

val Module.crateRootFiles: Collection<VirtualFile>
    get() = targets.mapNotNull { target ->
        contentRoot.findFileByRelativePath(target.path)
    } + externCrates.mapNotNull { it.virtualFile }

val Module.targets: Collection<CargoProjectDescription.Target> get() =
    getService<CargoModuleService>().targets

data class ExternCrate(
    /**
     * Name of a crate as appears in `extern crate foo;`
     */
    val name: String,

    /**
     * Root module file (typically `src/lib.rs`)
     */
    val virtualFile: VirtualFile
)

/**
 * Searches for the PsiFile of the root mod of the crate.
 */
fun Module.findExternCrateByName(crateName: String): PsiFile? =
    externCrates.find { it.name == crateName }?.let {
        PsiManager.getInstance(project).findFile(it.virtualFile)
    }

/**
 * A set of external crates for the module. External crate can refer
 * to another module or a library or a crate form the sdk.
 */
private val Module.externCrates: Collection<ExternCrate> get() =
    getService<CargoModuleService>().externCrates.mapNotNull { crate ->
        val file = File(crate.path)
        val vFile = if (file.isAbsolute)
            LocalFileSystem.getInstance().findFileByIoFile(file)
        else
            contentRoot.findFileByRelativePath(crate.path)

        vFile?.let { ExternCrate(crate.name, it) }
    } + sdkCrates

object SdkCrates {
    const val std: String = "std"
    const val core: String = "core"
}

private val Module.sdkCrates: Collection<ExternCrate> get() {
    val src = locateRustSources() ?: return emptyList()

    return listOf(
        "std" to "libstd/lib.rs",
        "core" to "libcore/lib.rs",
        "collections" to "libcollections/lib.rs"
    ).mapNotNull {
        val (crateName, srcLocation) = it
        src.findFileByRelativePath(srcLocation)?.let { crateFile ->
            ExternCrate(crateName, crateFile)
        }
    }
}

private fun Module.locateRustSources(): VirtualFile? {
    val sourceRoot = ModuleRootManager.getInstance(this)
        .orderEntries()
        .sdkOnly()
        .classesRoots
        .firstOrNull() ?: return null

    return if (sourceRoot.name == "src")
        sourceRoot
    else
        sourceRoot.findFileByRelativePath("src")
}

private val Module.contentRoot: VirtualFile get() =
    ModuleRootManager.getInstance(this).contentRoots.single()
