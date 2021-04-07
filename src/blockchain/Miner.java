package blockchain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class Miner {
    private static final int NUMBER_OF_TASKS = Runtime.getRuntime().availableProcessors();
    private static final int MINIMUM_CHAIN_SIZE = 5;
    private static final int AWAIT_TERMINATION_TIMEOUT = 800;
    private static final int FUTURE_GET_TIMEOUT = 100;
    private static final long MILLISECONDS_TO_WAIT_FOR_PENDING_MESSAGES = 300;
    private static final String FILE_NAME = "blockchain.bin";

    private Blockchain blockchain;

    public void run() {
        blockchain = loadFromFile();
        createFirstBlock();
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_TASKS);
        executorService.execute(new UserMessageTask(blockchain));
        List<Callable<Optional<MiningTaskRecord>>> callableTasks = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            callableTasks.add(new MiningTask(blockchain));
        }

        while (blockchain.getSize() <= MINIMUM_CHAIN_SIZE) {
            List<Future<Optional<MiningTaskRecord>>> futures;

            try {
                futures = executorService.invokeAll(callableTasks);
            } catch (InterruptedException e) {
                break;
            }

            findFirstTaskWithValidData(futures);
            stopAllTasks(futures);
            createNextBlock();
        }

        shutdownExecutor(executorService);
        saveToFile();
        blockchain.printFirstNBlocks(MINIMUM_CHAIN_SIZE);
    }

    private Blockchain loadFromFile() {
         if (Files.exists(Paths.get(FILE_NAME))) {
             return (Blockchain) SerializationUtils.deserialize(FILE_NAME);
         } else {
             return new Blockchain();
         }
    }

    private void saveToFile() {
        SerializationUtils.serialize(blockchain, FILE_NAME);
    }

    private void createFirstBlock() {
        long id = 1;
        long timestamp = new Date().getTime();
        String previousHash = "0";
        List<String> messages = new ArrayList<>();
        hashAndCreateBlock(id, timestamp, previousHash, messages);
    }

    private void createNextBlock() {
        long id = blockchain.getNextId();
        long timestamp = new Date().getTime();
        String previousHash = blockchain.getLastHash();

        while (!blockchain.isPendingMessages()) {
            try {
                TimeUnit.MILLISECONDS.sleep(MILLISECONDS_TO_WAIT_FOR_PENDING_MESSAGES);
            } catch (InterruptedException e) {
                return;
            }
        }

        List<String> messages = blockchain.getAndClearPendingMessages();
        hashAndCreateBlock(id, timestamp, previousHash, messages);
    }

    private void hashAndCreateBlock(long id, long timestamp, String previousHash, List<String> messages) {
        String hash = StringUtil.applySha256(String.format("%s%s%s%s", id, timestamp, previousHash, messages));
        Blockchain.Block firstBlock = new Blockchain.Block(id, timestamp, previousHash, hash, messages);
        blockchain.addBlockToChain(firstBlock);
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void findFirstTaskWithValidData(List<Future<Optional<MiningTaskRecord>>> futures) {
        boolean foundValidatedBlock = false;

        while (!foundValidatedBlock) {
            for (var future : futures) {
                if (future.isDone()) {
                    foundValidatedBlock = getAndValidateMiningTask(future);

                    if (foundValidatedBlock) {
                        break;
                    }
                }
            }
        }
    }

    private boolean getAndValidateMiningTask(Future<Optional<MiningTaskRecord>> future) {
        MiningTaskRecord record;

        try {
            Optional<MiningTaskRecord> recordOptional = future.get(FUTURE_GET_TIMEOUT, TimeUnit.MILLISECONDS);

            if (recordOptional.isEmpty()) {
                return false;
            }

            record = recordOptional.get();
        } catch (InterruptedException | TimeoutException e) {
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }

        if (!StringUtil.doesStringStartWithNumberOfZeros(record.getHash(), blockchain.getNumberOfZeros())) {
            return false;
        }

        createLastBlock(record);
        adjustNumberOfZeros(record);

        return true;
    }

    private void createLastBlock(MiningTaskRecord record) {
        Blockchain.Block lastBlock = blockchain.getLastBlock();
        lastBlock.setMagicNumber(record.getMagicNumber());
        lastBlock.setTimeGenerating(record.getTimeGenerating());
        lastBlock.setMinerNumber(record.getMinerNumber());
    }

    private void adjustNumberOfZeros(MiningTaskRecord record) {
        long secondsGenerating = record.getTimeGenerating();

        if (secondsGenerating > 60) {
            blockchain.decrementNumberOfZeros();
            blockchain.setLastChangeNMessage("N was decreased by 1");
        } else if (secondsGenerating < 6) {
            blockchain.incrementNumberOfZeros();
            blockchain.setLastChangeNMessage("N was increased to " + blockchain.getNumberOfZeros());
        } else {
            blockchain.setLastChangeNMessage("N stays the same");
        }
    }

    private void stopAllTasks(List<Future<Optional<MiningTaskRecord>>> futures) {
        for (var future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }
}
