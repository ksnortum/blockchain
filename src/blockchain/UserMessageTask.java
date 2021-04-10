package blockchain;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UserMessageTask implements Runnable {
    private static final long MILLISECONDS_BETWEEN_MESSAGES = 200;

    private final List<String> predefinedMessages = List.of(
            "Tom: Hey, I'm first!",
            "Sarah: It's not fair!",
            "Sarah: You always will be first because it is your blockchain!",
            "Sarah: Anyway, thank you for this amazing chat.",
            "Tom: You're welcome :)",
            "Nick: Hey Tom, nice chat",
            "Knute: Have you tried out the T-Mobile internet service?",
            "Amy: Yeah, it's the BOMB!",
            "Sam: where can I get a good plumber?",
            "Kathy: Have you tried Angie's List?",
            "Tom: I just got vaccinated!",
            "Kathy: That's awesome!",
            "Sam: Congratulations!"
    );
    private final Blockchain blockchain;

    public UserMessageTask(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public void run() {
        for (String text : predefinedMessages) {
            try {
                TimeUnit.MILLISECONDS.sleep(MILLISECONDS_BETWEEN_MESSAGES);
            } catch (InterruptedException e) {
                return;
            }

            blockchain.addToPendingMessages(new Message(text, blockchain.getNextMessageId()));
        }
    }
}
