package online.pizzacrust.mixinite.transform;

/**
 * Represents a transformation plugin that logs.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class LoggablePlugin {

    private final String prefix;

    public LoggablePlugin(String prefix) {
        this.prefix = prefix;
    }

    protected void log(String msg) {
        System.out.println(prefix + ": " + msg);
    }

    protected void log(String msg, String... format) {
        log(String.format(msg.replace("{}", "%s"), (Object[]) format));
    }

}
