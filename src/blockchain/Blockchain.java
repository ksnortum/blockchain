package blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class Blockchain implements Serializable {

    final class Block implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long id;
        private final long timestamp;
        private final long magicNumber;
        private final String previousHash;
        private final String hash;

        Block(String previousHash) {
            id = nextId.getAndIncrement();
            timestamp = new Date().getTime();
            this.previousHash = previousHash;
            HashCreator.MagicNumberAndHash magicNumberAndHash =
                    HashCreator.createHashWithNumberOfZeros(numberOfZeros, String.valueOf(id),
                            String.valueOf(timestamp), previousHash);
            magicNumber = magicNumberAndHash.getMagicNumber();
            hash = magicNumberAndHash.getHash();
        }

        String getHash() {
            return hash;
        }

        String getPreviousHash() {
            return previousHash;
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
                    "%s%n", id, timestamp, magicNumber, previousHash, hash);
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

    Block newBlock() {
        String previousHash = chain.isEmpty() ? "0" : chain.get(chain.size() - 1).getHash();
        Block block = new Block(previousHash);
        chain.add(block);

        return block;
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
