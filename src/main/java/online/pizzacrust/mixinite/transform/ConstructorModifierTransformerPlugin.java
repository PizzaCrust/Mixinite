package online.pizzacrust.mixinite.transform;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConstructorModifierTransformerPlugin extends LoggablePlugin implements
        MixinTransformationPlugin {

    public ConstructorModifierTransformerPlugin() {
        super("ConstructorModifierPlugin");
    }

    private Optional<ConstructorModify> getConstructorModifierMetadata(CtMethod ctMethod) {
        try {
            return Optional.of(Class.forName(ctMethod.getDeclaringClass().getName())
                    .getDeclaredMethod(ctMethod.getName(), InjectorPlugin.from(ctMethod.getParameterTypes()))
                    .getAnnotation
                    (ConstructorModify.class));
        } catch (ClassNotFoundException | NoSuchMethodException | NotFoundException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private List<CtMethod> getConstructorModifiers(CtClass ctClass) {
        List<CtMethod> ctMethods = new ArrayList<>();
        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            if (ctMethod.hasAnnotation(ConstructorModify.class)) {
                ctMethods.add(ctMethod);
            }
        }
        return ctMethods;
    }

    private Optional<CtConstructor> getConstructor(CtMethod ctMethod, CtClass ctClass) {
        String methodDescriptor = InjectorPlugin.removeCallbackDescriptor(ctMethod.getMethodInfo2
                ().getDescriptor());
        for (CtConstructor ctConstructor : ctClass.getDeclaredConstructors()) {
            if (ctConstructor.getMethodInfo2().getDescriptor().equals(methodDescriptor)) {
                return Optional.of(ctConstructor);
            }
        }
        return Optional.empty();
    }

    @Override
    public void handle(CtClass mixin, CtClass ctClass) {
        List<CtMethod> mixinConstructModifiers = getConstructorModifiers(mixin);
        log("Found {} constructor modifiers in {}!", String.valueOf(mixinConstructModifiers.size
                ()), mixin.getSimpleName());
        log("Processing constructor modifiers...");
        for (CtMethod ctMethod : mixinConstructModifiers) {
            Optional<ConstructorModify> modifier = getConstructorModifierMetadata(ctMethod);
            modifier.ifPresent((metadata) -> {
                try {
                    if (metadata.all()) {
                        for (CtConstructor ctConstructor : ctClass.getDeclaredConstructors()) {
                            String methodName = ctMethod.getDeclaringClass().getSimpleName() +
                                    "$" + ctMethod.getName();
                            CtMethod newMethod = CtNewMethod.copy(ctMethod, methodName, ctClass,
                                    null);
                            ctClass.addMethod(newMethod);
                            String callbackClass = InjectorPlugin.CallbackMetadata.class.getName();
                            String code = String.format("%s callbackInfo = new %s(); %s($$, " +
                                    "callbackInfo); if (callbackInfo.isCancelled()) { return; } "
                                    , callbackClass, callbackClass, methodName);
                            ctConstructor.insertAfter(code);
                        }
                    } else {
                        Optional<CtConstructor> constructorOpt = getConstructor(ctMethod, ctClass);
                        if (!constructorOpt.isPresent()) {
                            log("{}#{} doesn't have a equal constructor.", mixin.getSimpleName()
                                    , ctMethod.getName());
                        }
                        constructorOpt.ifPresent((ctConstructor -> {
                            try {
                                String methodName = ctMethod.getDeclaringClass().getSimpleName() +
                                        "$" + ctMethod.getName();
                                CtMethod newMethod = CtNewMethod.copy(ctMethod, methodName, ctClass,
                                        null);
                                ctClass.addMethod(newMethod);
                                String callbackClass = InjectorPlugin.CallbackMetadata.class.getName();
                                String code = String.format("%s callbackInfo = new %s(); %s($$, " +
                                                "callbackInfo); if (callbackInfo.isCancelled()) { return; } "
                                        , callbackClass, callbackClass, methodName);
                                ctConstructor.insertAfter(code);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Constructor modification is only allowed at the end of the constructor.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ConstructorModify {
        boolean all();
    }

}
