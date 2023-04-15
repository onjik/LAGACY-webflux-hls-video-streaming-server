package com.oj.videostreamingserver.global.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    public void test(){
        CryptoUtil cryptoUtil = new AesCryptoUtil();
        String originalString = "Hello, World!";
        String encryptedString = cryptoUtil.encrypt(originalString);
        String decryptedString = cryptoUtil.decrypt(encryptedString);

        assertNotEquals(originalString,encryptedString);
        assertEquals(originalString,decryptedString);
    }

}