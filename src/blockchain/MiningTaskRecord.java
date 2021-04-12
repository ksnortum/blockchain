package blockchain;

public class MiningTaskRecord {
    private final long magicNumber;
    private final String hash;
    private final long timeGenerating;
    private final Entity miner;

    public MiningTaskRecord(long magicNumber, String hash, long timeGenerating, Entity miner) {
        this.magicNumber = magicNumber;
        this.hash = hash;
        this.timeGenerating = timeGenerating;
        this.miner = miner;
    }

    public long getMagicNumber() {
        return magicNumber;
    }

    public String getHash() {
        return hash;
    }

    public long getTimeGenerating() {
        return timeGenerating;
    }

    public Entity getMiner() {
        return miner;
    }

    @Override
    public String toString() {
        return String.format("MiningTaskRecord{magic# = %d, hash = %s, time generating = %d, miner = %s}",
                magicNumber, hash, timeGenerating, miner);
    }
}
