package org.rust.lang.core.psi.impl

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import org.rust.lang.RustFileType
import org.rust.lang.RustLanguage
import org.rust.lang.core.psi.RustFileModItem
import org.rust.lang.core.psi.util.RustModules

class RustFile(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, RustLanguage) {

    override fun getFileType(): FileType = RustFileType

    val mod: RustFileModItem?
        get() = findChildByClass(RustFileModItem::class.java)

}

/**
 * Prepends directory name to this file, if it is `mod.rs`
 */
val PsiFile.usefulName: String get() = when (name) {
    RustModules.MOD_RS -> containingDirectory?.let { dir ->
        FileUtil.join(dir.name, name)
    } ?: name
    else -> name
}

val PsiFile.rustMod: RustFileModItem? get() =
    (this as? RustFile)?.mod
