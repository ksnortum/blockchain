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

        // creating a magic number and hash
        long startTime = System.currentTimeMillis();
        createHashWithNumberOfZeros(blockchain);

        if (Thread.currentThread().isInterrupted()) {
            return Optional.empty();
        }

        long timeGenerating = (System.currentTimeMillis() - startTime) / 1000;

        return Optional.of(new MiningTaskRecord(magicNumber, hash, timeGenerating, Thread.currentThread().getId()));
    }

    private void createHashWithNumberOfZeros(Blockchain blockchain) {
        String stringToHash = String.format("%s%s%s%s", blockchain.getLastId(), blockchain.getLastTimestamp(),
                blockchain.getLastPreviousHash(), blockchain.getLastMessages());
        SecureRandom random = new SecureRandom();

        do {
            magicNumber = random.nextLong();
            hash = StringUtil.applySha256(stringToHash + magicNumber);
        } while (!StringUtil.doesStringStartWithNumberOfZeros(hash, blockchain.getNumberOfZeros()) &&
                !Thread.currentThread().isInterrupted());
    }
}
