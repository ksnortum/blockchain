package blockchain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final Entity sender;
    private final Entity receiver;
    private final int amount;
    private byte[] signature;

    public Transaction(long id, Entity sender, Entity receiver, int amount) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        signTransaction();
    }

    public long getId() {
        return id;
    }

    public Entity getSender() {
        return sender;
    }

    public Entity getReceiver() {
        return receiver;
    }

    public int getAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return signature;
    }

    private void signTransaction() {
        String data = String.format("%d%s%s%d", id, sender.getName(), receiver.getName(), amount);

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
        return String.format("Transaction{id = %s, sender = %s, receiver = %s, amount = %d}",
                id, sender.getName(), receiver.getName(), amount);
    }
}
