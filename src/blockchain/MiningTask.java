package blockchain;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Creates a magic number and hash for the last (current) block in the chain, and
 * returns a @{link MiningTaskRecord} with the calculated data.
 */
public class MiningTask implements Callable<Optional<MiningTaskRecord>> {

    private static final String SECURITY_ALGORITHM = "NativePRNG";
    private static final Random random = new Random(new Date().getTime());

    private final Blockchain blockchain;
    private long magicNumber;
    private String hash;
    private final Entity miner;

    public MiningTask(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.miner = pickRandomMiner();
    }

    @Override
    public Optional<MiningTaskRecord> call() {
        miner.increaseAmountBy(Blockchain.AWARD_AMOUNT);
        long startTime = System.currentTimeMillis();
        createHashWithNumberOfZeros(blockchain);

        if (Thread.currentThread().isInterrupted()) {
            return Optional.empty();
        }

        long timeGenerating = (System.currentTimeMillis() - startTime) / 1000;

        return Optional.of(new MiningTaskRecord(magicNumber, hash, timeGenerating, miner));
    }

    private void createHashWithNumberOfZeros(Blockchain blockchain) {
        String stringToHash = String.format("%s%s%s%s" + Blockchain.MINER_AWARD_FORMAT,
                blockchain.getLastId(), blockchain.getLastTimestamp(),
                blockchain.getLastPreviousHash(), blockchain.getLastTransactions(),
                miner.getName(), Blockchain.AWARD_AMOUNT);
        SecureRandom secureRandom;

        try {
            secureRandom = SecureRandom.getInstance(SECURITY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        do {
            magicNumber = secureRandom.nextLong();
            hash = StringUtil.applySha256(stringToHash + magicNumber);
        } while (!StringUtil.doesStringStartWithNumberOfZeros(hash, blockchain.getNumberOfZeros()) &&
                !Thread.currentThread().isInterrupted());
    }

    private Entity pickRandomMiner() {
        List<Entity> miners = blockchain.getEntities().stream()
                .filter(Entity::isMiner)
                .collect(Collectors.toList());
        int index = random.nextInt(miners.size());

        return miners.get(index);
    }
}
