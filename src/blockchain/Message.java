package blockchain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String text;
    private final long id;
    private byte[] signature;

    public Message(String text, long id) {
        this.text = text;
        this.id = id;
        signMessage();
    }

    public String getText() {
        return text;
    }

    public long getId() {
        return id;
    }

    public byte[] getSignature() {
        return signature;
    }

    private void signMessage() {
        String data = text + id;

        try {
            Signature rsa = Signature.getInstance(SecurityKeyPair.SIGNATURE_ALGORITHM);
            rsa.initSign(getPrivateKeyFromFile());
            rsa.update(data.getBytes());
            signature = rsa.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private PrivateKey getPrivateKeyFromFile() {
        try {
            byte[] keyBytes = Files.readAllBytes(new File(SecurityKeyPair.PATH_TO_PRIVATE_KEY).toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(SecurityKeyPair.KEY_PAIR_ALGORITHM);

            return kf.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return String.format("Message{text = %s, id = %d}", text, id);
    }
}
