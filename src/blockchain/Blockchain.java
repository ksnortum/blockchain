package blockchain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class Blockchain implements Serializable {

    static final class Block implements Serializable {
        private static final long serialVersionUID = 4L;

        private final long id;
        private final long timestamp;
        private long magicNumber;
        private final String previousHash;
        private String hash;
        private long timeGenerating;
        private long minerNumber;
        private final List<Message> messages;
        private String changeNMessage;

        Block(long id, long timestamp, String previousHash, List<Message> messages) {
            this.id = id;
            this.timestamp = timestamp;
            this.previousHash = previousHash;
            this.messages = messages;
        }

        long getId() {
            return id;
        }

        long getTimestamp() {
            return timestamp;
        }

        public long getMagicNumber() {
            return magicNumber;
        }

        String getHash() {
            return hash;
        }

        String getPreviousHash() {
            return previousHash;
        }

        List<Message> getMessages() {
            return messages;
        }

        void setHash(String hash) {
            this.hash = hash;
        }

        void setTimeGenerating(long timeGenerating) {
            this.timeGenerating = timeGenerating;
        }

        void setMagicNumber(long magicNumber) {
            this.magicNumber = magicNumber;
        }

        void setMinerNumber(long minerNumber) {
            this.minerNumber = minerNumber;
        }

        void setChangeNMessage(String changeNMessage) {
            this.changeNMessage = changeNMessage;
        }

        @Override
        public String toString() {
            String blockData;

            if (messages.isEmpty()) {
                blockData = "Block data: no messages";
            } else {
                blockData = "Block data:\n" + messages.stream()
                        .map(Message::getText)
                        .collect(Collectors.joining("\n"));
            }

            return String.format("Block:%n" +
                    "Created by miner # %d%n" +
                    "Id: %d%n" +
                    "Timestamp: %d%n" +
                    "Magic number: %d%n" +
                    "Hash of the previous block:%n" +
                    "%s%n" +
                    "Hash of the block:%n" +
                    "%s%n" +
                    "%s%n" +
                    "Block was generating for %d seconds%n" +
                    "%s%n",
                    minerNumber, id, timestamp, magicNumber, previousHash, hash, blockData, timeGenerating,
                    changeNMessage);
        }
    }

    private static final long serialVersionUID = 4L;
    private final List<Block> chain = new ArrayList<>();
    private int numberOfZeros = 0;
    private final List<Message> pendingMessages = new ArrayList<>();
    private final AtomicLong newMessageId = new AtomicLong(1);
    private long currentValidMessageId = Long.MAX_VALUE;
    private PublicKey publicKey;

    int getNumberOfZeros() {
        return numberOfZeros;
    }

    synchronized void incrementNumberOfZeros() {
        numberOfZeros++;
    }

    synchronized void decrementNumberOfZeros() {
        numberOfZeros = Math.max(0, --numberOfZeros);
    }

    synchronized List<Message> getAndClearPendingMessages() {
        List<Message> messages = new ArrayList<>(pendingMessages);
        pendingMessages.clear();

        return messages;
    }

    synchronized void addToPendingMessages(Message message) {
        pendingMessages.add(message);
    }

    boolean isPendingMessages() {
        return !pendingMessages.isEmpty();
    }

    int getSize() {
        return chain.size();
    }

    synchronized void addBlockToChain(Block block) {
        chain.add(block);
    }

    synchronized Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    synchronized String getLastHash() {
        return chain.isEmpty() ? "none" : getLastBlock().getHash();
    }

    synchronized String getLastPreviousHash() {
        return chain.isEmpty() ? "none" : getLastBlock().getPreviousHash();
    }

    synchronized long getNextId() {
        return chain.isEmpty() ? 1 : getLastBlock().getId() + 1;
    }

    synchronized long getLastId() {
        return chain.isEmpty() ? 0 : getLastBlock().getId();
    }

    synchronized long getLastTimestamp() {
        return getLastBlock().getTimestamp();
    }

    synchronized List<Message> getLastMessages() {
        return getLastBlock().getMessages();
    }

    synchronized void setLastChangeNMessage(String changeNMessage) {
        getLastBlock().setChangeNMessage(changeNMessage);
    }

    void printFirstNBlocks(int noOfBlocks) {
        chain.stream().limit(noOfBlocks).forEach(System.out::println);
    }

    boolean validate() {
        publicKey = getPublicKeyFromDisk();

        for (int i = chain.size() - 1; i >= 0; i--) {
            Block currentBlock = chain.get(i);
            String stringToHash = String.format("%s%s%s%s%s",
                    currentBlock.getId(), currentBlock.getTimestamp(), currentBlock.getPreviousHash(),
                    currentBlock.getMessages(), currentBlock.getMagicNumber());
            String checkHash = StringUtil.applySha256(stringToHash);

            if (!currentBlock.getHash().equals(checkHash)) {
                System.out.println("Hash did not validate");
                return false;
            }

            if (i == 0) {
                if (!"0".equals(currentBlock.getPreviousHash())) {
                    return false;
                }
            } else {
                Block previousBlock = chain.get(i - 1);

                if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                    System.out.println("Previous hash did not validate");
                    return false;
                }

                boolean messagesValid = validateMessage(currentBlock.getMessages());

                if (!messagesValid) {
                    System.out.println("Message did not validate");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean validateMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);

            // check that message ID increases (tricky, cuz we're going backwards)
            if (message.getId() >= currentValidMessageId) {
                System.out.println("Message ID does not increase");
                return false;
            }

            currentValidMessageId = message.getId();

            // check that signature is valid
            if (!verifySignature(message.getText() + message.getId(), message.getSignature())) {
                System.out.println("Message signature is not valid");
                return false;
            }
        }

        return true;
    }

    private boolean verifySignature(String data, byte[] signature) {
        try {
            Signature sig = Signature.getInstance(SecurityKeyPair.SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data.getBytes());

            return sig.verify(signature);
        } catch(NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey getPublicKeyFromDisk() {
        try {
            byte[] keyBytes = Files.readAllBytes(new File(SecurityKeyPair.PATH_TO_PUBLIC_KEY).toPath());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(SecurityKeyPair.KEY_PAIR_ALGORITHM);

            return kf.generatePublic(spec);
        } catch(IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    long getNextMessageId() {
        return newMessageId.getAndIncrement();
    }
}
