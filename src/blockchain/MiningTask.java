package blockchain;

import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.Callable;

public class MiningTask implements Callable<Blockchain.Block> {
    private final Blockchain blockchain;
    private long magicNumber;
    private String hash;

    MiningTask(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public Blockchain.Block call() {
        String previousHash = blockchain.getLastHash();
        long id = blockchain.getNextId();
        long timestamp = new Date().getTime();
        long startTime = System.currentTimeMillis();
        createHashWithNumberOfZeros(id, timestamp, previousHash, blockchain.getNumberOfZeros());
        long timeGenerating = (System.currentTimeMillis() - startTime) / 1000;

        return new Blockchain.Block(id, timestamp, magicNumber, previousHash, hash, timeGenerating,
                Thread.currentThread().getId());
    }

    private void createHashWithNumberOfZeros(long id, long timestamp, String previousHash, int numberOfZeros) {
        String stringToHash = "" + id + timestamp + previousHash;
        SecureRandom random = new SecureRandom();

        do {
            magicNumber = random.nextLong();
            hash = StringUtil.applySha256(stringToHash + magicNumber);
        } while (!StringUtil.doesStringStartWithNumberOfZeros(hash, numberOfZeros));
    }

}
