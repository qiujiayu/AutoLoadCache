package com.jarvis.cache.aop.asm;

import java.io.IOException;

public class AccountTest {

    public static void main(String[] args) throws ClassFormatError, InstantiationException, IllegalAccessException, IOException {
        Account account=SecureAccountGenerator.generateSecureAccount();
        account.operation("test");
    }

}
