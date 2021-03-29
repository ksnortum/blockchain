package blockchain;

import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        Blockchain blockchain = new Blockchain();
        IntStream.range(0, 10).forEach(index -> blockchain.newBlock());

        if (!blockchain.validate()) {
            System.out.println("Blockchain did not validate");
        }

        blockchain.print(5);
    }
}
