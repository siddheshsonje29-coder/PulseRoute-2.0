package com.pulseroute.util;

import java.security.SecureRandom;

/**
 * BCrypt password hashing implementation based on the standard OpenBSD Blowfish cipher.
 * Compatible with standard jBCrypt.
 */
public class BCrypt {
    // BCrypt parameters
    private static final int GENSALT_DEFAULT_LOG2_ROUNDS = 10;
    private static final int BCRYPT_SALT_LEN = 16;

    // Blowfish parameters
    private static final int BLOWFISH_NUM_ROUNDS = 16;

    // Initial P-box and S-boxes based on digits of pi
    private static final int P_orig[] = {
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344,
        0xa4093822, 0x299f31d0, 0x082efa98, 0xec4e6c89,
        0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
        0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917,
        0x9216d5d9, 0x8979fb1b
    };

    private static final int S_orig[] = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7,
        0xb8e1afed, 0x6a267e96, 0xba7c9045, 0xf12c7f99,
        0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
        0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e,
        0x0d95748f, 0x728eb658, 0x718bcd58, 0x82154aee,
        0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013,
        0xc5d1b023, 0x286085f0, 0xca417918, 0xb8db38ef,
        0x8e79dcb0, 0x603a180e, 0x6c9e0e8b, 0xb01e8a3e,
        0xd71577c1, 0xbd314b27, 0x78af2fda, 0x55605c60,
        0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440,
        0x55ca396a, 0x2aab10b6, 0xb4cc5c34, 0x1141e8ce,
        0xa15486af, 0x7c72e993, 0xb3ee1411, 0x636fbc2a,
        0x2ba9c55d, 0x741831f6, 0xce5c3e16, 0x9b87931e,
        0xafd6ba33, 0x6c24cf5c, 0x7a325381, 0x28958677,
        0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193,
        0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032,
        0xef845d5d, 0xe98575b1, 0xdc262302, 0xeb651b88,
        0x23893e81, 0xd396acc5, 0x0f6d6ff3, 0x83f44239,
        0x2e0b4482, 0xa4842004, 0x69c8f04a, 0x9e1f9b5e,
        0x21c66842, 0xf6e96c9a, 0x670c9c61, 0xabd388f0,
        0x6a51a0d2, 0xd8542f68, 0x960fa728, 0xab5133a3,
        0x6eef0b6c, 0x137a3be4, 0xba3bf050, 0x7efb2bbe,
        0x9b1147a8, 0x161d7220, 0x3dfb352e, 0xe153c61f,
        0x47b2c0e8, 0x8593c200, 0x82645832, 0x3563b15e,
        0x196720f4, 0x738734e0, 0x47b4d13e, 0x550aa53b,
        0x12a95c96, 0x138e64c2, 0x86737acb, 0x74c9359e,
        0x83e4de19, 0x3c2b8c5e, 0x59902640, 0xd2c6a46b,
        0x6cff3c94, 0x15bfb91d, 0x06fb0c45, 0x4462b32f,
        0x0c0106cd, 0x3d0383b4, 0x71822830, 0x45f8f844,
        0x9f564f52, 0x5e804f32, 0x991f8932, 0x501b44ec,
        0x92518e95, 0x9599540c, 0x9188e7b9, 0x3f5c2253,
        0x8a159932, 0x07dc4c2b, 0x82d9ee99, 0xb044e138,
        0x0e588478, 0x1d360096, 0x15d23313, 0x78abaf34,
        0x09865ecf, 0xb1d6240d, 0x9dc6ff84, 0x1c8b2de6,
        0x93fb99d4, 0x2288339b, 0x5731ffae, 0x9e31d7e2,
        0x2506e788, 0x28637952, 0x7509cf6e, 0x8a923594,
        0x19dfa717, 0x86e6859e, 0xdc00b0f7, 0x3d820847,
        0x16dc8c80, 0x2e0b9688, 0x510959ee, 0xd347e305,
        0x134fa582, 0x8c60010e, 0x8e83344d, 0x6e8a4a58,
        0x323393b4, 0x5776d499, 0xb06fa70e, 0x028c2c7f,
        0xa7ab4376, 0xbbf5e156, 0x16b080b0, 0x10d19de4,
        0xa493f6c8, 0x6001887e, 0x9b3236e7, 0x48df9287,
        0xc48fdb9b, 0x2213e9a4, 0xd5214470, 0x3d00cdcb,
        0x7ea4c3fe, 0x2e118ef8, 0x1a719d1e, 0xa5143b44,
        0x56a12b62, 0x4e6378e9, 0x25f82218, 0x8f77395a,
        0x904a6006, 0x508933fe, 0x86a9f4e6, 0x6b447474,
        0x5ec054f9, 0x2479e471, 0x5223049b, 0xa7d8487b,
        0x539b6f84, 0x7620a604, 0x303ef37b, 0x6198f14f,
        0x203598e2, 0x37841c32, 0x4299fe92, 0xf6f328a6,
        0x17c5307f, 0x5d911b33, 0xb655b32a, 0x4a4a4080,
        0x64243b71, 0xd1eb50f4, 0xa91079d5, 0x55305a59,
        0xc6657e09, 0x1e3a6a9b, 0x5c42f0e0, 0x5c479a31,
        0x140d57e1, 0x7383a1e5, 0x3097f4e9, 0xb48286b2,
        0xa452b474, 0x3cff70c7, 0x75938060, 0x64a90f9d,
        0x0164b326, 0x323a634e, 0x3b664d55, 0x9888c842,
        0x0026e649, 0x924fe5e1, 0x421f2837, 0xa9c0172e,
        0x8a613c01, 0x0ee64b0f, 0x0f2111d1, 0x2b24e880,
        0x09543a03, 0x564de585, 0x45773986, 0x5a47a84f,
        0x2614f65c, 0xf3518387, 0x794f6c42, 0x105d2ce2,
        0xd30048e0, 0x23e444e6, 0x6700c06f, 0x1b88da32,
        0x3e19a99a, 0x2e0229d9, 0x4ac2a8c6, 0xca647c14,
        0x9970eb0e, 0x4f84dc6b, 0x86047970, 0x8f7d652b,
        0x5c744759, 0x922a0322, 0xd50b75da, 0x29da96e4,
        0xd78fae98, 0x52c999e4, 0x43b875c1, 0x683264ff
    };

    // Table for base64 encoding with BCrypt alphabet
    private static final char base64_code[] = {
        '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9'
    };

    private static final byte index_64[] = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
        -1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
        13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
        25, 26, 27, -1, -1, -1, -1, -1, -1, 28, 29, 30,
        31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1,
        -1, -1, -1, -1
    };

    // Blowfish state
    private int P[];
    private int S[];

    public BCrypt() {
        P = new int[18];
        S = new int[1024];
    }

    private void init_key() {
        System.arraycopy(P_orig, 0, P, 0, P_orig.length);
        System.arraycopy(S_orig, 0, S, 0, S_orig.length);
    }

    private int streamtoword(byte[] data, int[] off) {
        int word = 0;
        for (int i = 0; i < 4; i++) {
            word = (word << 8) | (data[off[0]] & 0xff);
            off[0] = (off[0] + 1) % data.length;
        }
        return word;
    }

    private void encipher(int[] lr, int off) {
        int l = lr[off];
        int r = lr[off + 1];

        l ^= P[0];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[1];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[2];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[3];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[4];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[5];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[6];
        r ^= (((S[l >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[7];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[8];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[9];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[10];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[11];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[12];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[13];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[14];
        r ^= (((S[l >>> 24] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)]) ^ P[15];
        l ^= (((S[r >>> 24] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)]) ^ P[16];

        lr[off] = r ^ P[17];
        lr[off + 1] = l;
    }

    private void ekskey(byte[] data, byte[] key) {
        int off[] = { 0 };
        int lr[] = { 0, 0 };

        for (int i = 0; i < P.length; i++) {
            P[i] ^= streamtoword(key, off);
        }

        off[0] = 0;
        for (int i = 0; i < P.length; i += 2) {
            lr[0] ^= streamtoword(data, off);
            lr[1] ^= streamtoword(data, off);
            encipher(lr, 0);
            P[i] = lr[0];
            P[i + 1] = lr[1];
        }

        for (int i = 0; i < S.length; i += 2) {
            lr[0] ^= streamtoword(data, off);
            lr[1] ^= streamtoword(data, off);
            encipher(lr, 0);
            S[i] = lr[0];
            S[i + 1] = lr[1];
        }
    }

    private byte[] crypt_raw(byte[] password, byte[] salt, int log_rounds) {
        int rounds = 1 << log_rounds;
        init_key();
        ekskey(salt, password);

        for (int i = 0; i < rounds; i++) {
            byte[] zero = new byte[salt.length];
            ekskey(zero, password);
            ekskey(zero, salt);
        }

        int ctext[] = {
            0x4f727068, 0x65616e42, 0x65686f6c,
            0x64657253, 0x63727964, 0x6f756274
        };

        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 6; j += 2) {
                encipher(ctext, j);
            }
        }

        byte[] ret = new byte[24];
        for (int i = 0; i < 6; i++) {
            ret[i * 4 + 3] = (byte) (ctext[i] & 0xff);
            ret[i * 4 + 2] = (byte) ((ctext[i] >>> 8) & 0xff);
            ret[i * 4 + 1] = (byte) ((ctext[i] >>> 16) & 0xff);
            ret[i * 4] = (byte) ((ctext[i] >>> 24) & 0xff);
        }
        return ret;
    }

    private static String encode_base64(byte d[], int len) {
        StringBuilder rs = new StringBuilder();
        int off = 0;
        while (off < len) {
            int c1 = d[off++] & 0xff;
            rs.append(base64_code[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            int c2 = d[off++] & 0xff;
            c1 |= (c2 >> 4) & 0x0f;
            rs.append(base64_code[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            int c3 = d[off++] & 0xff;
            c1 |= (c3 >> 6) & 0x03;
            rs.append(base64_code[c1 & 0x3f]);
            rs.append(base64_code[c3 & 0x3f]);
        }
        return rs.toString();
    }

    private static byte[] decode_base64(String s, int maxolen) {
        StringBuilder rs = new StringBuilder();
        int off = 0, slen = s.length(), olen = 0;
        byte[] ret = new byte[maxolen];
        byte c1, c2, c3, c4;

        while (off < slen - 1 && olen < maxolen) {
            c1 = (byte) index_64[s.charAt(off++)];
            c2 = (byte) index_64[s.charAt(off++)];
            ret[olen++] = (byte) ((c1 << 2) | ((c2 & 0x30) >>> 4));
            if (olen >= maxolen || off >= slen) break;
            c3 = (byte) index_64[s.charAt(off++)];
            ret[olen++] = (byte) (((c2 & 0x0f) << 4) | ((c3 & 0x3c) >>> 2));
            if (olen >= maxolen || off >= slen) break;
            c4 = (byte) index_64[s.charAt(off++)];
            ret[olen++] = (byte) (((c3 & 0x03) << 6) | c4);
        }
        return ret;
    }

    public static String gensalt(int log_rounds) {
        if (log_rounds < 4 || log_rounds > 31) {
            throw new IllegalArgumentException("Bad number of rounds");
        }
        SecureRandom random = new SecureRandom();
        byte rnd[] = new byte[BCRYPT_SALT_LEN];
        random.nextBytes(rnd);

        StringBuilder rs = new StringBuilder();
        rs.append("$2a$");
        if (log_rounds < 10) rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        rs.append(encode_base64(rnd, rnd.length));
        return rs.toString();
    }

    public static String gensalt() {
        return gensalt(GENSALT_DEFAULT_LOG2_ROUNDS);
    }

    public static String hashpw(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        if (salt.length() < 28 || salt.charAt(0) != '$' || salt.charAt(1) != '2') {
            throw new IllegalArgumentException("Invalid salt version");
        }

        int minor = salt.charAt(2) == '$' ? 0 : salt.charAt(2) - 'a' + 1;
        int off = salt.charAt(2) == '$' ? 3 : 4;
        if (salt.charAt(off + 2) > '$') {
            throw new IllegalArgumentException("Missing salt rounds");
        }
        int rounds = Integer.parseInt(salt.substring(off, off + 2));
        String real_salt = salt.substring(off + 3, off + 25);
        byte[] salt_bytes = decode_base64(real_salt, BCRYPT_SALT_LEN);

        byte[] pw_bytes;
        try {
            pw_bytes = (password + (minor >= 2 ? "\0" : "")).getBytes("UTF-8");
        } catch (Exception e) {
            pw_bytes = password.getBytes();
        }

        BCrypt b = new BCrypt();
        byte[] hashed = b.crypt_raw(pw_bytes, salt_bytes, rounds);

        StringBuilder res = new StringBuilder();
        res.append("$2a$");
        if (rounds < 10) res.append("0");
        res.append(rounds);
        res.append("$");
        res.append(encode_base64(salt_bytes, salt_bytes.length));
        res.append(encode_base64(hashed, 23));
        return res.toString();
    }

    public static boolean checkpw(String plaintext, String hashed) {
        if (plaintext == null || hashed == null || hashed.length() < 28) {
            return false;
        }
        try {
            String check = hashpw(plaintext, hashed);
            return check.equals(hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
