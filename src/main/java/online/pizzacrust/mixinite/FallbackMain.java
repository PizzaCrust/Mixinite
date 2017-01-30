package online.pizzacrust.mixinite;

import online.pizzacrust.mixinite.launch.Meow2;

public class FallbackMain implements Meow2 {
    public static void main(String... args) {
        System.out.println("Hello, world!");
    }

    private void accessChange(){}

    public String meow2() {
        return "meow";
    }

    public interface Meow {
        String meow();
    }

}