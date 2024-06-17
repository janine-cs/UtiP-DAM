package com.utipdam.mobility;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;

public class KeyUtil {
    public static String createLicenseKey(String userName, String orderRefNo) {
        final String s = userName + "|" + orderRefNo;
        final HashFunction hashFunction = Hashing.sha256();
        final HashCode hashCode = hashFunction.hashString(s, Charset.defaultCharset());
        final String upper = hashCode.toString().toUpperCase();
        return group(upper);
    }

    private static String group(String s) {
        String result = "";
        for (int i=0; i < s.length(); i++) {
            if (i%6==0 && i > 0) {
                result += '-';
            }
            result += s.charAt(i);
        }
        return result;
    }
}
