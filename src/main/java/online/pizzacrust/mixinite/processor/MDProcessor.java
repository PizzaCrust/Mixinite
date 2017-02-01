package online.pizzacrust.mixinite.processor;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import online.pizzacrust.mixinite.FallbackMain;
import online.pizzacrust.mixinite.MixinFallbackMain;
import online.pizzacrust.mixinite.transform.InjectorPlugin;
import online.pizzacrust.mixinite.transform.MethodOverlapPlugin;

public class MDProcessor implements Processor {

    private final Mappings mappings = new Mappings();

    public static String appendClassToDescriptor(String descriptor, String className) {
        String last = descriptor.substring(0, descriptor.lastIndexOf(';') + 1);
        String returnDesc = descriptor.substring(descriptor.lastIndexOf(')') + 1);
        return last + className + ";)" + returnDesc;
    }

    private String appendCtClassToDescriptor(String descriptor, CtClass ctClass) {
        return appendClassToDescriptor(descriptor, ctClass.getName());
    }

    private boolean hasCallback(String string) {
        return string.contains(InjectorPlugin.CallbackMetadata.class.getName().replace('.', '/'));
    }

    private boolean isReturnableCallback(String string) {
        return string.contains(InjectorPlugin.CallbackMetadataReturnable.class.getName().replace
                ('.', '/'));
    }

    @Override
    public Mappings process(CtClass mixin, CtClass target, Mappings srg) {
        for (CtMethod ctMethod : mixin.getDeclaredMethods()) {
                Optional<MethodRef> unmapped = srg.getUnmappedMethodMapping(target, ctMethod);
                unmapped.ifPresent((methodRef) -> {
                    String targetDescriptor = methodRef.getDesc();
                    String callbackClass = InjectorPlugin.CallbackMetadata.class.getName();
                    if (hasCallback(ctMethod.getMethodInfo2().getDescriptor())) {
                        if (isReturnableCallback(ctMethod.getMethodInfo2().getDescriptor())) {
                            callbackClass = InjectorPlugin.CallbackMetadataReturnable.class
                                    .getName();
                        }
                        targetDescriptor = appendClassToDescriptor(targetDescriptor,
                                callbackClass.replace('.', '/'));
                    }
                    /*
                    System.out.println(new MethodRef(mixin.getName().replace('.', '/'),
                            ctMethod.getName(), ctMethod.getMethodInfo2().getDescriptor()).toString());
                    System.out.println(new
                            MethodRef(
                            mixin
                                    .getName().replace('.', '/'), methodRef.getName(),
                            targetDescriptor).toString());
                    */
                    mappings.methodRefs.put(new MethodRef(mixin.getName().replace('.', '/'),
                            ctMethod.getName(), ctMethod.getMethodInfo2().getDescriptor()), new
                            MethodRef(
                            mixin
                            .getName().replace('.', '/'), methodRef.getName(), targetDescriptor));
                });
        }
        return this.mappings;
    }

    private static CtClass getCtClass(Class<?> name) throws NotFoundException {
        return ClassPool.getDefault().getCtClass(name.getName());
    }

    public static void main(String... args) throws Exception{
        Mappings srg = new Mappings(new File("fallbackSrg.srg"));
        Mappings newSrg = new MDProcessor().process(getCtClass(MixinFallbackMain.class), getCtClass
                (FallbackMain
                .class), srg);
        File newSrgFile = new File("newMDSrg.srg");
        Files.write(newSrgFile.toPath(), newSrg.toLines(), StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE);
    }

}
