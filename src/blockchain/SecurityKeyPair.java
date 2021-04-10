package blockchain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

public class SecurityKeyPair {

    private static final int KEY_LENGTH = 1024;

    public static final String PATH_TO_PUBLIC_KEY = "KeyPair/publicKey";
    public static final String PATH_TO_PRIVATE_KEY = "KeyPair/privateKey";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public SecurityKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
            generator.initialize(KEY_LENGTH);
            KeyPair keyPair = generator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeKeyPairToFiles() {
        writeToFile(PATH_TO_PUBLIC_KEY, publicKey.getEncoded());
        writeToFile(PATH_TO_PRIVATE_KEY, privateKey.getEncoded());
    }

    private void writeToFile(String path, byte[] key) {
        File file = new File(path);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
