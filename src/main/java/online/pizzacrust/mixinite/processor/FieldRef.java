package online.pizzacrust.mixinite.processor;

import javafx.util.Pair;

public class FieldRef {

    private final String owner;
    private final String name;

    public FieldRef(String owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return "FieldRef [owner= " + owner + ", name= " + name + "]";
    }

    public static Pair<FieldRef, FieldRef> parseLine(String fieldLine) {
        String[] splitted = fieldLine.split(" ");
        Pair<String, String> fieldA = MethodRef.parseOwnerAndName(splitted[1]);
        Pair<String, String> fieldB = MethodRef.parseOwnerAndName(splitted[2]);
        return new Pair<>(new FieldRef(fieldA.getKey(), fieldA.getValue()), new FieldRef(fieldB
                .getKey(), fieldB.getValue()));
    }

    public static String toSyntax(FieldRef ori, FieldRef remap) {
        return "FD: " + ori.getOwner() + "/" + ori.getName() + " " + remap.getOwner() + "/" +
                remap.getName();
    }


}
