package blockchain;

import java.io.Serializable;

public class Entity implements Serializable {
    public enum Type { MINER, PERSON, COMPANY, EMPLOYEE }

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Type type;
    private int amount;

    public Entity(String name, Type type, int amount) {
        this.name = name;
        this.type = type;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public void increaseAmountBy(int increase) {
        amount += increase;
    }

    public void decreaseAmountBy(int decrease) {
        amount -= decrease;
    }

    public boolean isMiner() {
        return type == Type.MINER;
    }

    public boolean isPerson() {
        return type == Type.PERSON;
    }

    public boolean isCompany() {
        return type == Type.COMPANY;
    }

    public boolean isEmployee() {
        return type == Type.EMPLOYEE;
    }

    @Override
    public String toString() {
        return String.format("Entity{name = %s, type = %s, amount = %d}", name, type, amount);
    }
}
