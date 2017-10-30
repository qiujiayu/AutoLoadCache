package com.jarvis.cache.aop.asm;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * @author: jiayu.qiu
 */
public class SecureAccountGenerator {

    private static AccountGeneratorClassLoader classLoader=new AccountGeneratorClassLoader();

    private static Class<?> secureAccountClass;

    public static Account generateSecureAccount() throws ClassFormatError, InstantiationException, IllegalAccessException, IOException {
        if(null == secureAccountClass) {
            String className=Account.class.getName();
            ClassReader cr=new ClassReader(className);
            ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor classAdapter=new AddSecurityCheckClassAdapter(cw);
            cr.accept(classAdapter, ClassReader.SKIP_DEBUG);
            byte[] data=cw.toByteArray();
            secureAccountClass=classLoader.defineClassFromClassFile(className + "$EnhancedByASM", data);
        }
        return (Account)secureAccountClass.newInstance();
    }

    private static class AccountGeneratorClassLoader extends ClassLoader {

        public Class<?> defineClassFromClassFile(String className, byte[] classFile) throws ClassFormatError {
            return defineClass(className, classFile, 0, classFile.length);
        }
    }
}