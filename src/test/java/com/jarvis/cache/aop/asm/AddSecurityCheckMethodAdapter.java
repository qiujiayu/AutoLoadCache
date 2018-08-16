package com.jarvis.cache.aop.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author: jiayu.qiu
 */
public class AddSecurityCheckMethodAdapter extends MethodVisitor implements Opcodes {

    public AddSecurityCheckMethodAdapter(MethodVisitor mv) {
        super(ASM6, mv);
    }

    // 此方法在访问方法的头部时被访问到，仅被访问一次
    @Override
    public void visitCode() {
        int opcode=Opcodes.INVOKESTATIC;
        visitMethodInsn(opcode, SecurityChecker.class.getName().replace('.', '/'), "checkSecurity", "()V", false);
        super.visitCode();
    }
}
