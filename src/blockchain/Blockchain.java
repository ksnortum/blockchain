package blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Blockchain implements Serializable {

    static final class Block implements Serializable {
        private static final long serialVersionUID = 3L;

        private final long id;
        private final long timestamp;
        private long magicNumber;
        private final String previousHash;
        private final String hash;
        private long timeGenerating;
        private long minerNumber;
        private final List<String> messages;
        private String changeNMessage;

        Block(long id, long timestamp, String previousHash, String hash, List<String> messages) {
            this.id = id;
            this.timestamp = timestamp;
            this.previousHash = previousHash;
            this.hash = hash;
            this.messages = messages;
        }

        long getId() {
            return id;
        }

        long getTimestamp() {
            return timestamp;
        }

        String getHash() {
            return hash;
        }

        String getPreviousHash() {
            return previousHash;
        }

        List<String> getMessages() {
            return messages;
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
                blockData = "Block data:\n" + String.join("\n", messages);
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

    private static final long serialVersionUID = 3L;
    private final List<Block> chain = new ArrayList<>();
    private int numberOfZeros = 0;
    private final List<String> pendingMessages = new ArrayList<>();

    int getNumberOfZeros() {
        return numberOfZeros;
    }

    synchronized void incrementNumberOfZeros() {
        numberOfZeros++;
    }

    synchronized void decrementNumberOfZeros() {
        numberOfZeros = Math.max(0, --numberOfZeros);
    }

    synchronized List<String> getAndClearPendingMessages() {
        List<String> messages = new ArrayList<>(pendingMessages);
        pendingMessages.clear();

        return messages;
    }

    synchronized void addToPendingMessages(String message) {
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
        return chain.isEmpty() ? "0" : getLastBlock().getHash();
    }

    synchronized long getNextId() {
        return chain.isEmpty() ? 1 : getLastBlock().getId() + 1;
    }

    synchronized long getLastTimestamp() {
        return getLastBlock().getTimestamp();
    }

    synchronized void setLastChangeNMessage(String changeNMessage) {
        getLastBlock().setChangeNMessage(changeNMessage);
    }

    void printFirstNBlocks(int noOfBlocks) {
        chain.stream().limit(noOfBlocks).forEach(System.out::println);
    }

    boolean validate() {
        for (int i = chain.size() - 1; i >= 0; i--) {
            Block currentBlock = chain.get(i);
            String checkHash = StringUtil.applySha256(String.format("%s%s%s%s",
                    currentBlock.getId(), currentBlock.getTimestamp(), currentBlock.getPreviousHash(),
                    currentBlock.getMessages()));

            if (!currentBlock.getHash().equals(checkHash)) {
                return false;
            }

            if (i == 0) {
                if (!"0".equals(currentBlock.getPreviousHash())) {
                    return false;
                }
            } else {
                Block previousBlock = chain.get(i - 1);

                if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                    return false;
                }
            }
        }

        return true;
    }
}
