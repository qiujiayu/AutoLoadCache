package com.jarvis.cache.aop.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author: jiayu.qiu
 */
public class ChangeToChildConstructorMethodAdapter extends MethodVisitor implements Opcodes {

    private String superClassName;

    private String desc;

    public ChangeToChildConstructorMethodAdapter(MethodVisitor mv, String superClassName, String desc, int access) {
        super(ASM6, mv);
        this.superClassName=superClassName;
        this.desc=desc;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", desc, false);
        mv.visitInsn(RETURN);
        mv.visitEnd();
    }

}
