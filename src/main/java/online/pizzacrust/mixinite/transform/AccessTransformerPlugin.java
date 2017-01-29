package online.pizzacrust.mixinite.transform;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Represents a plugin called before the {@link FieldOverlapPlugin} to transform accesses of
 * fields and methods.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class AccessTransformerPlugin extends LoggablePlugin implements MixinTransformationPlugin {

    public AccessTransformerPlugin() {
        super("ATPlugin");
    }

    private Optional<AccessTransform> getClassATMetadata(CtClass ctClass) {
        try {
            return Optional.of((AccessTransform) ctClass.getAnnotation(AccessTransform.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private Optional<CtMember> getMember(AccessTransform.Entry entry, CtClass ctClass) {
        if (entry.type() == AccessTransform.Type.FIELD) {
            for (CtField ctField : ctClass.getDeclaredFields()) {
                if (ctField.getName().equals(entry.name())) {
                    return Optional.of(ctField);
                }
            }
        }
        if (entry.type() == AccessTransform.Type.METHOD) {
            for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
                if (ctMethod.getName().equals(entry.name()) && ctMethod.getMethodInfo2()
                        .getDescriptor().equals(entry.desc())) {
                    return Optional.of(ctMethod);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void handle(CtClass mixin, CtClass ctClass) {
        if (mixin.hasAnnotation(AccessTransform.class)) {
            log("Transforming access of {} from {}...", ctClass.getSimpleName(), mixin.getSimpleName());
            Optional<AccessTransform> metadata = getClassATMetadata(mixin);
            metadata.ifPresent((accessTransformMetadata) -> {
                for (AccessTransform.Entry entry : accessTransformMetadata.entries()) {
                    log("Transforming a TYPE.{} with modifiers of {}...", entry.type().name(),
                            String.valueOf(entry.access()));
                    Optional<CtMember> ctMember = getMember(entry, ctClass);
                    if (!ctMember.isPresent()) {
                        log("Entry [ type={} and mod={} ] is not in the target class.", entry
                                .name(), String.valueOf(entry.access()));
                        continue;
                    }
                    CtMember member = ctMember.get();
                    member.setModifiers(entry.access());
                }
            });
        }
    }

    /**
     * Represents the annotated element will be access transformed.
     *
     * @since 1.0-SNAPSHOT
     * @author PizzaCrust
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AccessTransform {
       Entry[] entries();

       @interface Entry {

           String name();

           String desc() default "";

           Type type();

           int access();

       }

       enum Type {
           METHOD, FIELD
       }
    }

}
