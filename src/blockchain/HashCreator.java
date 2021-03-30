package blockchain;

import java.security.SecureRandom;

public class HashCreator {

    static class MagicNumberAndHash {
        private final long magicNumber;
        private final String hash;

        MagicNumberAndHash(long magicNumber, String hash) {
            this.magicNumber = magicNumber;
            this.hash = hash;
        }

        public long getMagicNumber() {
            return magicNumber;
        }

        public String getHash() {
            return hash;
        }
    }

    static MagicNumberAndHash createHashWithNumberOfZeros(int numberOfZeros, String... hashStrings) {
        String stringToHash = String.join("", hashStrings);
        SecureRandom random = new SecureRandom();
        long magicNumber;
        String hash;

        do {
            magicNumber = random.nextLong();
            hash = StringUtil.applySha256(stringToHash + magicNumber);
        } while (!doesStringStartWithNumberOfZeros(numberOfZeros, hash));

        return new MagicNumberAndHash(magicNumber, hash);
    }

    private static boolean doesStringStartWithNumberOfZeros(int numberOfZeros, String candidate) {
        String zeros = "0".repeat(Math.max(0, numberOfZeros));

        return candidate.startsWith(zeros);
    }

}
