package blockchain;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class Blockchain implements Serializable {

    final class Block implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long id;
        private final long timestamp;
        private long magicNumber;
        private final String previousHash;
        private String hash;
        private final long timeGenerating;

        Block(String previousHash) {
            id = nextId.getAndIncrement();
            timestamp = new Date().getTime();
            this.previousHash = previousHash;
            long startTime = System.currentTimeMillis();
            createHashWithNumberOfZeros();
            timeGenerating = (System.currentTimeMillis() - startTime) / 1000;
        }

        String getHash() {
            return hash;
        }

        String getPreviousHash() {
            return previousHash;
        }

        private void createHashWithNumberOfZeros() {
            String stringToHash = "" + id + timestamp + previousHash;
            SecureRandom random = new SecureRandom();

            do {
                magicNumber = random.nextLong();
                hash = StringUtil.applySha256(stringToHash + magicNumber);
            } while (!doesHashStartWithNumberOfZeros());
        }

        private boolean doesHashStartWithNumberOfZeros() {
            String zeros = "0".repeat(Math.max(0, numberOfZeros));

            return hash.startsWith(zeros);
        }

        @Override
        public String toString() {
            return String.format("Block:%n" +
                    "Id: %d%n" +
                    "Timestamp: %d%n" +
                    "Magic number: %d%n" +
                    "Hash of the previous block:%n" +
                    "%s%n" +
                    "Hash of the block:%n" +
                    "%s%n" +
                    "Block was generating for %d seconds%n",
                    id, timestamp, magicNumber, previousHash, hash, timeGenerating);
        }
    }

    private static final long serialVersionUID = 1L;
    private final AtomicLong nextId = new AtomicLong(1);
    private final List<Block> chain = new ArrayList<>();
    private final int numberOfZeros;

    Blockchain(int numberOfZeros) {
        if (numberOfZeros < 0) {
            System.out.println("Number of zeros cannot be negative; using 0");
            numberOfZeros = 0;
        }

        this.numberOfZeros = numberOfZeros;
    }

    void newBlock() {
        String previousHash = chain.isEmpty() ? "0" : chain.get(chain.size() - 1).getHash();
        Block block = new Block(previousHash);
        chain.add(block);
    }

    boolean validate() {
        if (chain.isEmpty()) {
            return true;
        }

        for (int index = chain.size() - 1; index >= 0; index--) {
            if (index == 0) {
                return "0".equals(chain.get(0).getPreviousHash());
            } else if (!chain.get(index - 1).getHash().equals(chain.get(index).getPreviousHash())) {
                return false;
            }
        }

        return true; // should never get here, non-empty lists have a zero index
    }

    void printFirst(int noOfBlocks) {
        chain.stream().limit(noOfBlocks).forEach(System.out::println);
    }

    void printAll() {
        chain.forEach(System.out::println);
    }

}
