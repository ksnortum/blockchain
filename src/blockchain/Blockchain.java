package blockchain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Blockchain {

    private class Block {
        private final long id;
        private final long timestamp;
        private final String previousHash;
        private final String hash;

        Block(String previousHash) {
            id = nextId.getAndIncrement();
            timestamp = new Date().getTime();
            this.previousHash = previousHash;
            hash = StringUtil.applySha256("" + id + timestamp + previousHash);
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
                    "Hash of the previous block:%n" +
                    "%s%n" +
                    "Hash of the block:%n" +
                    "%s%n", id, timestamp, previousHash, hash);
        }
    }

    private final AtomicLong nextId = new AtomicLong(1);
    private final List<Block> blocks = new ArrayList<>();

    void newBlock() {
        String previousHash = blocks.isEmpty() ? "0" : blocks.get(blocks.size() - 1).getHash();
        blocks.add(new Block(previousHash));
    }

    boolean validate() {
        if (blocks.isEmpty()) {
            return true;
        }

        for (int index = blocks.size() - 1; index >= 0; index--) {
            if (index == 0) {
                return "0".equals(blocks.get(0).getPreviousHash());
            } else if (!blocks.get(index - 1).getHash().equals(blocks.get(index).getPreviousHash())) {
                return false;
            }
        }

        return true; // should never get here, non-empty lists have a zero index
    }

    void print(int noOfBlocks) {
        blocks.stream().limit(noOfBlocks).forEach(System.out::println);
    }
}
