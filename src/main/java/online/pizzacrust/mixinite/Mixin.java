package online.pizzacrust.mixinite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a mixin class.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mixin {

    /**
     * Represents the target class.
     * @return
     */
    Class<?> value();

}
