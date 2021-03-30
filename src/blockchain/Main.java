package blockchain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    private static final String FILE_NAME = "blockchain.bin";

    public static void main(String[] args) {
        Blockchain blockchain;

//        if (Files.exists(Paths.get(FILE_NAME))) {
//            blockchain = (Blockchain) SerializationUtils.deserialize(FILE_NAME);
//
//            if (blockchain.validate()) {
//                blockchain.printAll();
//            } else {
//                System.out.println("Blockchain did not validate");
//                blockchain = new Blockchain(getNumberOfZeros());
//            }
//        } else {
            blockchain = new Blockchain(getNumberOfZeros());
//        }

        for (int i = 0; i < 5; i++) {
            blockchain.newBlock();
            SerializationUtils.serialize(blockchain, FILE_NAME);
        }

        blockchain.printFirst(5);
    }

    private static int getNumberOfZeros() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter how many zeros the hash must start with: ");
        int numberOfZeros;

        do {
            while (!scanner.hasNextInt()) {
                scanner.next();
            }

            numberOfZeros = scanner.nextInt();
        } while (numberOfZeros < 0);

        return numberOfZeros;
    }
}
