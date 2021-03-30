package blockchain;

import java.io.*;

public class SerializationUtils {
    /**
     * Serialize the given object to the file
     */
    public static void serialize(Object obj, String fileName) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))
        ) {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserialize to an object from the file
     */
    public static Object deserialize(String fileName) {
        Object obj;

        try (ObjectInputStream ois =
                      new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)))
        ) {
            obj = ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }
}