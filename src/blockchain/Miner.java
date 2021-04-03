package blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Miner {
    private static final int NUMBER_OF_TASKS = Runtime.getRuntime().availableProcessors();
    private static final int MINIMUM_CHAIN_SIZE = 5;
    private static final int AWAIT_TERMINATION_TIMEOUT = 800;
    private static final int FUTURE_GET_TIMEOUT = 100;

    private Blockchain blockchain;

    public void run() {
        blockchain = new Blockchain();
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_TASKS);

        while (blockchain.getSize() < MINIMUM_CHAIN_SIZE) {
            List<Callable<Blockchain.Block>> callableTasks = new ArrayList<>();

            for (int i = 0; i < NUMBER_OF_TASKS; i++) {
                callableTasks.add(new MiningTask(blockchain));
            }

            invokeTasks(executorService, callableTasks);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void invokeTasks(ExecutorService executorService, List<Callable<Blockchain.Block>> callableTasks) {
        List<Future<Blockchain.Block>> futures;

        try {
            futures = executorService.invokeAll(callableTasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        boolean foundValidatedBlock = false;

        while (!foundValidatedBlock) {
            for (var future : futures) {
                if (future.isDone()) {
                    foundValidatedBlock = getAndValidateNewBlock(future);

                    if (foundValidatedBlock) {
                        break;
                    }
                }
            }
        }

        stopAllTasks(futures);
    }

    private boolean getAndValidateNewBlock(Future<Blockchain.Block> future) {
        try {
            Blockchain.Block newBlock = future.get(FUTURE_GET_TIMEOUT, TimeUnit.MILLISECONDS);
            boolean isPreviousHashValid = newBlock.getPreviousHash().equals(blockchain.getLastHash());
            boolean isHashStartsWithNumberOfZeros =
                    StringUtil.doesStringStartWithNumberOfZeros(newBlock.getHash(), blockchain.getNumberOfZeros());

            if (isPreviousHashValid && isHashStartsWithNumberOfZeros) {
                blockchain.addBlockToChain(newBlock);
            } else {
                return false;
            }

            System.out.println(newBlock);
            long secondsGenerating = newBlock.getTimeGenerating();

            if (secondsGenerating > 60) {
                blockchain.decrementNumberOfZeros();
                System.out.println("N was decreased by 1");
            } else if (secondsGenerating < 6) {
                blockchain.incrementNumberOfZeros();
                System.out.println("N was increased to " + blockchain.getNumberOfZeros());
            } else {
                System.out.println("N stays the same");
            }

            System.out.println();

            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void stopAllTasks(List<Future<Blockchain.Block>> futures) {
        for (var future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }
}
