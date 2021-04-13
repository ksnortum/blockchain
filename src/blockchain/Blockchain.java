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
        private static final long serialVersionUID = 5L;

        private final long id;
        private final long timestamp;
        private long magicNumber;
        private final String previousHash;
        private String hash;
        private long timeGenerating;
        private final List<Transaction> transactions;
        private String changeNMessage;
        private Entity miner;
        private String minerAward;

        Block(long id, long timestamp, String previousHash, List<Transaction> transactions) {
            this.id = id;
            this.timestamp = timestamp;
            this.previousHash = previousHash;
            this.transactions = transactions;
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

        List<Transaction> getTransactions() {
            return transactions;
        }

        Entity getMiner() {
            return miner;
        }

        String getMinerAward() {
            return minerAward;
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

        void setChangeNMessage(String changeNMessage) {
            this.changeNMessage = changeNMessage;
        }

        void setMiner(Entity miner) {
            this.miner = miner;
        }

        void setMinerAward(String minerAward) {
            this.minerAward = minerAward;
        }

        @Override
        public String toString() {
            String transactionString;

            if (transactions.isEmpty()) {
                transactionString = "No transactions";
            } else {
                transactionString = transactions.stream()
                        .map(t -> String.format("%s sent %d VC to %s",
                                t.getSender().getName(), t.getAmount(), t.getReceiver().getName()))
                        .collect(Collectors.joining("\n"));
            }

            return String.format("Block:%n" +
                    "Created by: %s%n" +
                    "%s%n" +
                    "Id: %d%n" +
                    "Timestamp: %d%n" +
                    "Magic number: %d%n" +
                    "Hash of the previous block:%n" +
                    "%s%n" +
                    "Hash of the block:%n" +
                    "%s%n" +
                    "Block data:%n" +
                    "%s%n" +
                    "Block was generating for %d seconds%n" +
                    "%s%n",
                    miner == null ? "no name" : miner.getName(), minerAward, id, timestamp, magicNumber,
                    previousHash, hash, transactionString, timeGenerating, changeNMessage);
        }
    }

    private static final long serialVersionUID = 4L;
    public static final String MINER_AWARD_FORMAT = "%s gets %d VC";
    public static final int AWARD_AMOUNT = 100;

    private final List<Block> chain = new ArrayList<>();
    private int numberOfZeros = 0;
    private transient List<Transaction> pendingTransactions = new ArrayList<>();
    private final AtomicLong nextTransactionId = new AtomicLong(1);
    private transient long currentValidTransactionId = Long.MAX_VALUE;
    private PublicKey publicKey;
    private final List<Entity> entities = loadEntities();

    int getNumberOfZeros() {
        return numberOfZeros;
    }

    synchronized void incrementNumberOfZeros() {
        numberOfZeros++;
    }

    synchronized void decrementNumberOfZeros() {
        numberOfZeros = Math.max(0, --numberOfZeros);
    }

    synchronized List<Transaction> getAndClearPendingTransactions() {
        List<Transaction> transactions = new ArrayList<>(pendingTransactions);
        pendingTransactions.clear();

        return transactions;
    }

    synchronized void addToPendingTransactions(Transaction transaction) {
        pendingTransactions.add(transaction);
    }

    boolean isPendingTransactions() {
        return !pendingTransactions.isEmpty();
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

    synchronized List<Transaction> getLastTransactions() {
        return getLastBlock().getTransactions();
    }

    synchronized void setLastChangeNMessage(String changeNMessage) {
        getLastBlock().setChangeNMessage(changeNMessage);
    }

    void printLastNBlocks(int noOfBlocks) {
        chain.stream().skip(Math.max(0, chain.size() - noOfBlocks)).forEach(System.out::println);
    }

    boolean validate() {
        publicKey = getPublicKeyFromDisk();

        for (int i = chain.size() - 1; i >= 0; i--) {
            Block currentBlock = chain.get(i);
            String stringToHash = String.format("%s%s%s%s%s%d",
                    currentBlock.getId(), currentBlock.getTimestamp(), currentBlock.getPreviousHash(),
                    currentBlock.getTransactions(), currentBlock.getMinerAward(), currentBlock.getMagicNumber());
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

                boolean transactionsValid = validateTransactions(currentBlock.getTransactions());

                if (!transactionsValid) {
                    System.out.println("Transactions did not validate");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean validateTransactions(List<Transaction> transactions) {
        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction transaction = transactions.get(i);

            // check that message ID increases (tricky, cuz we're going backwards)
            if (transaction.getId() >= currentValidTransactionId) {
                System.out.printf("Transaction ID does not increase, this ID = %d, current ID = %d%n",
                        transaction.getId(), currentValidTransactionId);
                return false;
            }

            currentValidTransactionId = transaction.getId();

            // check that signature is valid
            String data = String.format("%d%s%s%d", transaction.getId(), transaction.getSender().getName(),
                    transaction.getReceiver().getName(), transaction.getAmount());
            if (!verifySignature(data, transaction.getSignature())) {
                System.out.println("Transaction signature is not valid");
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

    long getNextTransactionId() {
        return nextTransactionId.getAndIncrement();
    }

    synchronized void updateTransactionId() {
        if (chain.size() < 2) {
            nextTransactionId.set(1);
            return;
        }

        List<Transaction> transactions = getLastBlock().getTransactions();
        Transaction lastTransaction = transactions.get(Math.max(0, transactions.size() - 1));
        nextTransactionId.set(lastTransaction.getId() + 1);
        currentValidTransactionId = Long.MAX_VALUE;
    }

    synchronized void initializePendingTransactions() {
        pendingTransactions = new ArrayList<>();
    }

    private List<Entity> loadEntities() {
        List<Entity> entities = new ArrayList<>();
        entities.add(new Entity("miner1", Entity.Type.MINER, 100));
        entities.add(new Entity("miner2", Entity.Type.MINER, 100));
        entities.add(new Entity("miner3", Entity.Type.MINER, 100));
        entities.add(new Entity("Nick", Entity.Type.PERSON, 0));
        entities.add(new Entity("Ben", Entity.Type.PERSON, 0));
        entities.add(new Entity("Kim", Entity.Type.PERSON, 0));
        entities.add(new Entity("Walmart", Entity.Type.COMPANY, 0));
        entities.add(new Entity("BiMart", Entity.Type.COMPANY, 0));
        entities.add(new Entity("Safeway", Entity.Type.COMPANY, 0));
        entities.add(new Entity("Worker1", Entity.Type.EMPLOYEE, 0));
        entities.add(new Entity("Worker2", Entity.Type.EMPLOYEE, 0));
        entities.add(new Entity("Worker3", Entity.Type.EMPLOYEE, 0));

        return entities;
    }

    public List<Entity> getEntities() {
        return entities;
    }
}
