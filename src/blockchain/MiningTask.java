package blockchain;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Creates a magic number and hash for the last (current) block in the chain, and
 * returns a @{link MiningTaskRecord} with the calculated data.
 */
public class MiningTask implements Callable<Optional<MiningTaskRecord>> {
    private final Blockchain blockchain;
    private long magicNumber;
    private String hash;

    public MiningTask(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public Optional<MiningTaskRecord> call() {

        // Get data from last block needed to create magic number and hash
        long id = blockchain.getNextId();
        long timestamp = blockchain.getLastTimestamp();
        String previousHash = blockchain.getLastHash();

        // Start the process of creating a magic number and hash
        long startTime = System.currentTimeMillis();
        createHashWithNumberOfZeros(id, timestamp, previousHash, blockchain);

        if (Thread.currentThread().isInterrupted()) {
            return Optional.empty();
        }

        long timeGenerating = (System.currentTimeMillis() - startTime) / 1000;

        return Optional.of(new MiningTaskRecord(magicNumber, hash, timeGenerating, Thread.currentThread().getId()));
    }

    private void createHashWithNumberOfZeros(long id, long timestamp, String previousHash, Blockchain blockchain) {
        String stringToHash = "" + id + timestamp + previousHash +
                String.join("", blockchain.getLastBlock().getMessages());
        SecureRandom random = new SecureRandom();

        do {
            magicNumber = random.nextLong();
            hash = StringUtil.applySha256(stringToHash + magicNumber);
        } while (!StringUtil.doesStringStartWithNumberOfZeros(hash, blockchain.getNumberOfZeros()) &&
                !Thread.currentThread().isInterrupted());
    }
}
