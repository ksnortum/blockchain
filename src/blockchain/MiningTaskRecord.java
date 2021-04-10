package blockchain;

public class MiningTaskRecord {
    private final long magicNumber;
    private final String hash;
    private final long timeGenerating;
    private final long minerNumber;

    public MiningTaskRecord(long magicNumber, String hash, long timeGenerating, long minerNumber) {
        this.magicNumber = magicNumber;
        this.hash = hash;
        this.timeGenerating = timeGenerating;
        this.minerNumber = minerNumber;
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

    public long getMinerNumber() {
        return minerNumber;
    }

    @Override
    public String toString() {
        return String.format("MiningTaskRecord{magic# = %d, hash = %s, time generating = %d, miner# = %d}",
                magicNumber, hash, timeGenerating, minerNumber);
    }
}
