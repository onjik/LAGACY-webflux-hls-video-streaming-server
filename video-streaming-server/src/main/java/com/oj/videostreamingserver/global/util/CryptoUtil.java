package com.oj.videostreamingserver.global.util;

public interface CryptoUtil {
    String encrypt(String value);

    String decrypt(String encrypted);
}
