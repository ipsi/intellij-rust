package org.rust.lang.core.psi.impl.mixin

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.core.psi.RustForeignModItemElement
import org.rust.lang.core.psi.RustOuterAttrElement
import org.rust.lang.core.psi.impl.RustStubbedElementImpl
import org.rust.lang.core.stubs.elements.RustForeignModItemElementStub

abstract class RustForeignModItemImplMixin  : RustStubbedElementImpl<RustForeignModItemElementStub>
                                            , RustForeignModItemElement {

    constructor(node: ASTNode): super(node)

    constructor(stub: RustForeignModItemElementStub, elementType: IStubElementType<*, *>): super(stub, elementType)

    override val outerAttrList: List<RustOuterAttrElement>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, RustOuterAttrElement::class.java)

    override val isPublic: Boolean get() = false // visibility does not affect foreign mods
}
