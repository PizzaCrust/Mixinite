package online.pizzacrust.mixinite.transform;

import javassist.CtClass;

/**
 * Represents a handler that handles specific situations for transformations.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public interface MixinTransformationPlugin {

    /**
     * Handles a transformation for mixins to target class.
     * @param mixin
     * @param ctClass
     */
    void handle(CtClass mixin, CtClass ctClass);

}