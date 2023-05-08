package com.oj.videostreamingserver.global.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class ConverterUtil {
    public static byte[] convertToByte(UUID source) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(source.getMostSignificantBits());
        bb.putLong(source.getLeastSignificantBits());
        return bb.array();
    }
}
