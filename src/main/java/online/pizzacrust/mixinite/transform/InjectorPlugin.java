package online.pizzacrust.mixinite.transform;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Represents a plugin to inject into specific lines.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class InjectorPlugin extends LoggablePlugin implements MixinTransformationPlugin {
    public InjectorPlugin() {
        super("InjectorPlugin");
    }

    private String removeCallbackDescriptor(String descriptor) {
        String a = descriptor.replace
                ("Lonline/pizzacrust/mixinite/transform/InjectorPlugin$CallbackMetadata;", "");
        String b = a.replace
                ("Lonline/pizzacrust/mixinite/transform" +
                        "/InjectorPlugin$CallbackMetadataReturnable;", "");
        return b;
    }

    private Optional<CtMethod> getCtMethod(CtClass newClass, CtMethod method) {
        for (CtMethod ctMethod : newClass.getDeclaredMethods()) {
            if (ctMethod.getName().equals(method.getName()) && removeCallbackDescriptor(ctMethod.getMethodInfo2()
                    .getDescriptor()).equals(removeCallbackDescriptor(method.getMethodInfo2()
                    .getDescriptor()))) {
                return Optional.of(ctMethod);
            }
        }
        return Optional.empty();
    }

    private List<CtMethod> getMethodInjects(CtClass mixin, CtClass ctClass) {
        ArrayList<CtMethod> methods = new ArrayList<>();
        for (CtMethod ctMethod : mixin.getDeclaredMethods()) {
            CtMethod mixinMember = ctMethod;
            if (mixinMember.hasAnnotation(Inject.class)) {
                if (!mixinMember.hasAnnotation(MethodOverlapPlugin.IgnoreMethodOverlapping
                        .class)) {
                    log("Please remember to use @MethodOverlapPlugin.IgnoreMethodOverlapping" +
                            " on @Inject methods. (" + mixinMember.getDeclaringClass()
                            .getName() + "#" + mixinMember.getName() + ")");
                }
                methods.add(mixinMember);
            }
        }
        return methods;
    }

    private Class<?> fromArray(CtClass ctClass) throws ClassNotFoundException {
        String className = ctClass.getPackageName() + "." + ctClass.getSimpleName().replace("[]", "");
        Class<?> clazz = Class.forName(className);
        return Array.newInstance(clazz, 0).getClass();
    }

    private Class<?>[] from(CtClass... ctClasses) {
        List<Class<?>> reflectClasses = new ArrayList<>();
        for (CtClass ctClass : ctClasses) {
            try {
                if (ctClass.isArray()) {
                    reflectClasses.add(fromArray(ctClass));
                } else {
                    reflectClasses.add(Class.forName(ctClass.getName()));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return reflectClasses.toArray(new Class[reflectClasses.size()]);
    }

    private Optional<Inject> getInjectMetadata(CtMethod ctMethod) {
        try {
            return Optional.of((Inject) Class.forName(ctMethod.getDeclaringClass().getName())
                    .getDeclaredMethod(ctMethod.getName(), from(ctMethod.getParameterTypes()))
                    .getAnnotation(Inject.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void handle(CtClass mixin, CtClass ctClass) {
        List<CtMethod> methods = getMethodInjects(mixin, ctClass);
        log("Found {} @Inject methods in {} for {}!", String.valueOf(methods.size()), mixin.getSimpleName(),
                ctClass.getSimpleName());
        methods.forEach((method) -> {
            Optional<Inject> injectMetadataOpt = getInjectMetadata(method);
            injectMetadataOpt.ifPresent((injectMetadata) -> {
                Optional<CtMethod> targetMethod = getCtMethod(ctClass, method);
                if (!targetMethod.isPresent()) {
                    log("{}#{} has no equal target in target class.", method.getDeclaringClass()
                            .getName(), method.getName());
                } else {
                    log("Injecting method #{} into {}...", method.getName(), targetMethod.get().getName());
                    CtMethod tMethod = targetMethod.get();
                    try {
                        CtMethod newMethod = new CtMethod(method, ctClass, null);
                        //System.out.println(newMethod.getName());
                        String methodName = method.getDeclaringClass().getSimpleName() + "$" +
                                method.getName();
                        newMethod.setName(methodName);
                        ctClass.addMethod(newMethod);
                        String callbackClass = CallbackMetadata.class.getName();
                        if (tMethod.getReturnType() != CtClass.voidType) {
                            callbackClass = CallbackMetadataReturnable.class.getName();
                        }
                        if (callbackClass.equals(CallbackMetadata.class.getName())) {
                            String code = String.format("%s callbackInfo = new %s(); %s" +
                                    "($$, callbackInfo); if (callbackInfo.isCancelled()) { return; " +
                                    "}", callbackClass, callbackClass, methodName);
                            if (injectMetadata.line() != -1) {
                                tMethod.insertAt(injectMetadata.line(), code);
                            } else {
                                switch (injectMetadata.pos()) {
                                    case HEAD:
                                        tMethod.insertBefore(code);
                                        break;
                                    case RETURN:
                                        tMethod.insertAfter(code);
                                        break;
                                }
                            }
                        } else if (callbackClass.equals(CallbackMetadataReturnable.class.getName())) {
                            String code = String.format("%s callbackInfo = new %s();" +
                                            "%s($$, callbackInfo); if (callbackInfo.isCancelled()) { " +
                                            "return (" + newMethod.getReturnType().getName() + ")" +
                                            " " +
                                            "callbackInfo" +
                                            ".getReturnObj(); }",
                                    callbackClass,
                                    callbackClass, methodName);
                            if (injectMetadata.line() != -1) {
                                tMethod.insertAt(injectMetadata.line(), code);
                            } else {
                                switch (injectMetadata.pos()) {
                                    case HEAD:
                                        tMethod.insertBefore(code);
                                        break;
                                    case RETURN:
                                        tMethod.insertAfter(code);
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Inject {
        int line() default -1;

        Pos pos() default Pos.HEAD;

        enum Pos {
            HEAD,
            RETURN
        }
    }

    public static class CallbackMetadata {
        private boolean cancelled = false;

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    public static class CallbackMetadataReturnable<RETURN_TYPE> extends CallbackMetadata {
        private RETURN_TYPE returnObj;

        public RETURN_TYPE getReturnObj() {
            return returnObj;
        }

        public void setReturnObj(RETURN_TYPE returnObj) {
            this.returnObj = returnObj;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(String[].class.getName());
        CtClass ctClass = ClassPool.getDefault().getCtClass(InjectorPlugin.class.getName());
        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            System.out.println(ctMethod.getName());
            if (ctMethod.getName().equals("main")) {
                for (CtClass parameter : ctMethod.getParameterTypes()) {
                    System.out.println(parameter.getName());
                    System.out.println(parameter.getPackageName());
                    System.out.println(parameter.getSimpleName().replace("[]", ""));
                    System.out.println("[L" + parameter.getPackageName() + "." + parameter
                            .getSimpleName
                            ().replace("[]", ""));
                }
            }
        }
    }

}
