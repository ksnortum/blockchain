package blockchain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class Miner {
    private static final int NUMBER_OF_TASKS = Runtime.getRuntime().availableProcessors();
    private static final int NUMBER_OF_NEW_BLOCKS = 15;
    private static final int AWAIT_TERMINATION_TIMEOUT = 800;
    private static final int FUTURE_GET_TIMEOUT = 100;
    private static final long MILLISECONDS_TO_WAIT_FOR_PENDING_MESSAGES = 300;
    private static final int DECREMENT_AFTER_SECONDS = 1;
    private static final int INCREMENT_AFTER_SECONDS = 0;
    private static final String FILE_NAME = "blockchain.bin";

    private Blockchain blockchain;

    public void run() {
        setupSecurityKeyPair();
        blockchain = loadFromFile();
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_TASKS);
        executorService.execute(new TransactionTask(blockchain));
        startMinersAndProcess(executorService);
        shutdownExecutor(executorService);

        if (blockchain.validate()) {
            saveToFile();
            blockchain.printLastNBlocks(NUMBER_OF_NEW_BLOCKS);
        } else {
            System.out.println("Blockchain did not validate");
        }
    }

    private void setupSecurityKeyPair() {
        boolean privateKeyExists = Files.exists(Paths.get(SecurityKeyPair.PATH_TO_PRIVATE_KEY));
        boolean publicKeyExists = Files.exists(Paths.get(SecurityKeyPair.PATH_TO_PUBLIC_KEY));

        if (!privateKeyExists || !publicKeyExists) {
            SecurityKeyPair keyPair = new SecurityKeyPair();
            keyPair.writeKeyPairToFiles();
        }
    }

    private Blockchain loadFromFile() {
         if (Files.exists(Paths.get(FILE_NAME))) {
             Blockchain blockchain = (Blockchain) SerializationUtils.deserialize(FILE_NAME);
             blockchain.updateTransactionId();
             blockchain.initializePendingTransactions();

             return blockchain;
         } else {
             return new Blockchain();
         }
    }

    private void saveToFile() {
        SerializationUtils.serialize(blockchain, FILE_NAME);
    }

    private void startMinersAndProcess(ExecutorService executorService) {
        List<Callable<Optional<MiningTaskRecord>>> callableTasks = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_TASKS - 1; i++) {
            callableTasks.add(new MiningTask(blockchain));
        }

        for (int i = 0; i < NUMBER_OF_NEW_BLOCKS; i++) {
            if (blockchain.getSize() == 0) {
                createFirstBlock();
            } else {
                createNextBlock();
            }

            if (!startMinersAndUpdateBlock(executorService, callableTasks)) {
                break;
            }
        }
    }

    private boolean startMinersAndUpdateBlock(ExecutorService executorService,
                                              List<Callable<Optional<MiningTaskRecord>>> callableTasks) {
        List<Future<Optional<MiningTaskRecord>>> futures;

        try {
            futures = executorService.invokeAll(callableTasks);
        } catch (InterruptedException e) {
            return false;
        }

        findFirstTaskWithValidData(futures);
        stopAllTasks(futures);

        return true;
    }

    private void createFirstBlock() {
        long id = 1;
        long timestamp = new Date().getTime();
        String previousHash = "0";
        List<Transaction> transactions = new ArrayList<>();
        hashAndCreateBlock(id, timestamp, previousHash, transactions);
    }

    private void createNextBlock() {
        long id = blockchain.getNextId();
        long timestamp = new Date().getTime();
        String previousHash = blockchain.getLastHash();

        while (!blockchain.isPendingTransactions()) {
            try {
                TimeUnit.MILLISECONDS.sleep(MILLISECONDS_TO_WAIT_FOR_PENDING_MESSAGES);
            } catch (InterruptedException e) {
                return;
            }
        }

        List<Transaction> transactions = blockchain.getAndClearPendingTransactions();
        hashAndCreateBlock(id, timestamp, previousHash, transactions);
    }

    private void hashAndCreateBlock(long id, long timestamp, String previousHash, List<Transaction> transactions) {
        Blockchain.Block block = new Blockchain.Block(id, timestamp, previousHash, transactions);
        blockchain.addBlockToChain(block);
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

        updateLastBlock(record);
        adjustNumberOfZeros(record);

        return true;
    }

    private void updateLastBlock(MiningTaskRecord record) {
        Blockchain.Block lastBlock = blockchain.getLastBlock();
        lastBlock.setHash(record.getHash());
        lastBlock.setMagicNumber(record.getMagicNumber());
        lastBlock.setTimeGenerating(record.getTimeGenerating());
        Entity miner = record.getMiner();
        lastBlock.setMiner(miner);
        lastBlock.setMinerAward(String.format(Blockchain.MINER_AWARD_FORMAT,
                miner.getName(), Blockchain.AWARD_AMOUNT));
    }

    private void adjustNumberOfZeros(MiningTaskRecord record) {
        long secondsGenerating = record.getTimeGenerating();

        if (secondsGenerating > DECREMENT_AFTER_SECONDS) {
            blockchain.decrementNumberOfZeros();
            blockchain.setLastChangeNMessage("N was decreased by 1");
        } else if (secondsGenerating < INCREMENT_AFTER_SECONDS) {
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
