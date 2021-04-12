package blockchain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TransactionTask implements Runnable {
    private static final long MILLISECONDS_BETWEEN_TRANSACTIONS = 200;
    private static final Random random = new Random(new Date().getTime());

    private final Blockchain blockchain;
    private final List<Entity> entities;

    public TransactionTask(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.entities = blockchain.getEntities();
    }

    @Override
    public void run() {
        while(true) {
            Entity sender = getASenderWithMoney();
            Entity receiver = getAnAppropriateReceiver(sender);
            int amount = getAnAmount(sender);
            blockchain.addToPendingTransactions(new Transaction(blockchain.getNextTransactionId(),
                    sender, receiver, amount));
            sender.decreaseAmountBy(amount);
            receiver.increaseAmountBy(amount);

            try {
                TimeUnit.MILLISECONDS.sleep(MILLISECONDS_BETWEEN_TRANSACTIONS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private Entity getASenderWithMoney() {
        List<Entity> entitiesWithMoney = entities.stream()
                .filter(e -> e.getAmount() > 0)
                .collect(Collectors.toList());
        int index = random.nextInt(entitiesWithMoney.size());

        return entitiesWithMoney.get(index);
    }

    private Entity getAnAppropriateReceiver(Entity sender) {
        List<Entity> receivers = new ArrayList<>();

        switch (sender.getType()) {
            case MINER:
                receivers = entities.stream()
                        .filter(e -> e.isMiner() || e.isPerson())
                        .collect(Collectors.toList());
                break;
            case PERSON:
                receivers = entities.stream()
                        .filter(e -> e.isPerson() || e.isCompany())
                        .collect(Collectors.toList());
                break;
            case COMPANY:
                receivers = entities.stream()
                        .filter(e -> e.isCompany() || e.isEmployee())
                        .collect(Collectors.toList());
                break;
            case EMPLOYEE:
                receivers = entities.stream()
                        .filter(e -> e.isCompany() || e.isEmployee() || e.isPerson())
                        .collect(Collectors.toList());
                break;
        }

        int index;

        do {
            index = random.nextInt(receivers.size());
        } while (receivers.get(index).getName().equals(sender.getName()));

        return receivers.get(index);
    }

    private int getAnAmount(Entity sender) {
        return random.nextInt(sender.getAmount()) + 1;
    }
}
