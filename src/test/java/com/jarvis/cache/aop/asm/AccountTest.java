package com.jarvis.cache.aop.asm;

import java.io.IOException;

/**
 * 必须使用JDK1.6测试
 * @author: jiayu.qiu
 */
public class AccountTest {

    public static void main(String[] args) throws ClassFormatError, InstantiationException, IllegalAccessException, IOException {
        Account account=SecureAccountGenerator.generateSecureAccount();
        account.operation("test");
    }

}
