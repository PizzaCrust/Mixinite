package online.pizzacrust.mixinite.transform;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a method overlapping concept implementation as a plugin.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class MethodOverlapPlugin extends LoggablePlugin implements MixinTransformationPlugin {

    public MethodOverlapPlugin() {
        super("MethodOverlapPlugin");
    }

    private Optional<CtMethod> getCtMethod(CtClass newClass, CtMethod method) {
        for (CtMethod ctMethod : newClass.getDeclaredMethods()) {
            if (ctMethod.getName().equals(method.getName()) && ctMethod.getMethodInfo2()
                    .getDescriptor().equals(method.getMethodInfo2().getDescriptor()) && ctMethod
                    .getModifiers() == method.getModifiers()) {
                return Optional.of(ctMethod);
            }
        }
        return Optional.empty();
    }

    private List<CtMethod> getChangedMethods(CtClass mixin, CtClass ctClass) {
        ArrayList<CtMethod> methods = new ArrayList<>();
        for (CtMethod originalMember : ctClass.getDeclaredMethods()) {
            Optional<CtMethod> mixinMemberOpt = getCtMethod(mixin, originalMember);
            mixinMemberOpt.ifPresent((ctMethod -> {
                if (!ctMethod.hasAnnotation(IgnoreMethodOverlapping.class)) {
                    methods.add(ctMethod);
                }
            }));
        }
        return methods;
    }

    private List<CtMethod> getNewMethods(CtClass mixin, CtClass ctClass) {
        ArrayList<CtMethod> methods = new ArrayList<>();
        for (CtMethod newMember : mixin.getDeclaredMethods()) {
            Optional<CtMethod> originalMemberOpt = getCtMethod(ctClass, newMember);
            if (!originalMemberOpt.isPresent() && !newMember.hasAnnotation
                    (IgnoreMethodOverlapping.class)) {
                methods.add(newMember);
            }
        }
        return methods;
    }

    @Override
    public void handle(CtClass mixin, CtClass ctClass) {
        List<CtMethod> changedMethods = getChangedMethods(mixin, ctClass);
        List<CtMethod> newMethods = getNewMethods(mixin, ctClass);
        log("Found {} new methods from {} to {}...", String.valueOf(newMethods.size()), mixin.getSimpleName(),
                ctClass.getSimpleName());
        log("Found {} overwritten methods from {} to {}...", String.valueOf(changedMethods.size()),
                mixin.getSimpleName(), ctClass.getSimpleName());
        log("Overwriting methods...");
        changedMethods.forEach((newMethod) -> {
            Optional<CtMethod> targetMethodOpt = getCtMethod(ctClass, newMethod);
            targetMethodOpt.ifPresent((targetMethod) -> {
                log("Overwriting {}#{} with {}#{}...", targetMethod.getDeclaringClass()
                        .getSimpleName(), targetMethod.getName(), newMethod.getDeclaringClass()
                        .getSimpleName(), newMethod.getName());
                try {
                    targetMethod.setBody(newMethod, null);
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            });
        });
        log("Adding new methods...");
        newMethods.forEach((newMethod) -> {
            log("Adding #{} to {}...", newMethod.getName(), ctClass.getSimpleName());
            try {
                ctClass.addMethod(CtNewMethod.copy(newMethod, ctClass, null));
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Identifies a method that ignores this plugin.
     *
     * @since 1.0-SNAPSHOT
     * @author PizzaCrust
     */
    public @interface IgnoreMethodOverlapping {

    }

}
