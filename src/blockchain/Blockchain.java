package blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Blockchain implements Serializable {

    static final class Block implements Serializable {
        private static final long serialVersionUID = 2L;

        private final long id;
        private final long timestamp;
        private final long magicNumber;
        private final String previousHash;
        private final String hash;
        private final long timeGenerating;
        private final long minerNumber;

        Block(long id, long timestamp, long magicNumber, String previousHash, String hash, long timeGenerating,
              long minerNumber) {
            this.id = id;
            this.timestamp = timestamp;
            this.magicNumber = magicNumber;
            this.previousHash = previousHash;
            this.hash = hash;
            this.timeGenerating = timeGenerating;
            this.minerNumber = minerNumber;
        }

        long getId() {
            return id;
        }

        String getHash() {
            return hash;
        }

        String getPreviousHash() {
            return previousHash;
        }

        long getTimeGenerating() {
            return timeGenerating;
        }

        @Override
        public String toString() {
            return String.format("Block:%n" +
                    "Created by miner # %d%n" +
                    "Id: %d%n" +
                    "Timestamp: %d%n" +
                    "Magic number: %d%n" +
                    "Hash of the previous block:%n" +
                    "%s%n" +
                    "Hash of the block:%n" +
                    "%s%n" +
                    "Block was generating for %d seconds",
                    minerNumber, id, timestamp, magicNumber, previousHash, hash, timeGenerating);
        }
    }

    private static final long serialVersionUID = 2L;
    private final List<Block> chain = new ArrayList<>();
    private int numberOfZeros = 0;

    synchronized int getNumberOfZeros() {
        return numberOfZeros;
    }

    void setNumberOfZeros(int numberOfZeros) {
        if (numberOfZeros < 0) {
            System.out.println("Number of zeros cannot be negative; using 0");
            numberOfZeros = 0;
        }

        this.numberOfZeros = numberOfZeros;
    }

    void incrementNumberOfZeros() {
        numberOfZeros++;
    }

    void decrementNumberOfZeros() {
        numberOfZeros = Math.max(0, --numberOfZeros);
    }

    int getSize() {
        return chain.size();
    }

    void addBlockToChain(Block block) {
        chain.add(block);
    }

    Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    synchronized String getLastHash() {
        return chain.isEmpty() ? "0" : getLastBlock().getHash();
    }

    synchronized long getNextId() {
        return chain.isEmpty() ? 1 : getLastBlock().getId() + 1;
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
