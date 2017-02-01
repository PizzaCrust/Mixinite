package online.pizzacrust.mixinite.processor;

import javafx.util.Pair;

public class MethodRef {

    private final String owner;
    private final String name;
    private final String desc;

    public MethodRef(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return "MethodRef [owner=" + owner + ", name=" + name + ", desc=" + desc + "]";
    }

    static Pair<String, String> parseOwnerAndName(String ref) {
        String owner = ref.substring(0, ref.lastIndexOf('/'));
        String name = ref.substring(ref.lastIndexOf('/') + 1);
        return new Pair<>(owner, name);
    }

    public static Pair<MethodRef, MethodRef> parseLine(String line) {
        String[] splitted = line.split(" ");
        Pair<String, String> ownerA = parseOwnerAndName(splitted[1]);
        String descA = splitted[2];
        Pair<String, String> ownerB = parseOwnerAndName(splitted[3]);
        String descB = splitted[4];
        MethodRef a = new MethodRef(ownerA.getKey(), ownerA.getValue(), descA);
        MethodRef b = new MethodRef(ownerB.getKey(), ownerB.getValue(), descB);
        return new Pair<>(a, b);
    }

    public static String toSyntax(MethodRef ori, MethodRef remap) {
        return "MD: " + ori.getOwner() + "/" + ori.getName() + " " + ori.getDesc() + " " + remap
                .getOwner() + "/" + remap.getName() + " " + remap.getDesc();
    }

}
