package online.pizzacrust.mixinite;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import online.pizzacrust.mixinite.transform.AccessTransformerPlugin;
import online.pizzacrust.mixinite.transform.FieldOverlapPlugin;
import online.pizzacrust.mixinite.transform.InjectorPlugin;
import online.pizzacrust.mixinite.transform.InterfacePlugin;
import online.pizzacrust.mixinite.transform.MethodOverlapPlugin;

/**
 * A bootstrap to integrate all default plugins into a single process.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class MixiniteBootstrap {

    private final List<CtClass> otherClasses = new ArrayList<>();
    private final List<CtClass> mixins = new ArrayList<>();

    public MixiniteBootstrap(List<CtClass> classpath) {
        for (CtClass ctClass : classpath) {
            if (ctClass.hasAnnotation(Mixin.class)) {
                mixins.add(ctClass);
            } else {
                otherClasses.add(ctClass);
            }
        }
    }

    private Optional<CtClass> getCtClass(Class<?> clazz) {
        try {
            return Optional.of(ClassPool.getDefault().getCtClass(clazz.getName()));
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private Optional<Mixin> getMixinMetadata(CtClass ctClass) {
        try {
            return Optional.of((Mixin) Class.forName(ctClass.getName()).getAnnotation(Mixin.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void applyPlugins(ClassLoader classLoader, CtClass mixin, CtClass target, boolean
            load) {
        new AccessTransformerPlugin().handle(mixin, target);
        new FieldOverlapPlugin().handle(mixin, target);
        new MethodOverlapPlugin().handle(mixin, target);
        new InjectorPlugin().handle(mixin, target);
        new InterfacePlugin().handle(mixin, target);
        if (load) {
            if (classLoader != null) {
                try {
                    target.toClass(classLoader);
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    target.toClass();
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void init() throws Exception {
        init(null, true);
    }

    public List<CtClass> init(ClassLoader classLoader, boolean load) throws Exception {
        System.out.println("Initialising Mixinite...");
        ArrayList<CtClass> ctClasses = new ArrayList<>();
        for (CtClass mixin : mixins) {
            Optional<Mixin> mixinMetadataOpt = getMixinMetadata(mixin);
            mixinMetadataOpt.ifPresent((mixinMetadata) -> {
                Optional<CtClass> targetOpt = getCtClass(mixinMetadata.value());
                targetOpt.ifPresent((targetClass) ->  {
                    this.applyPlugins(classLoader, mixin, targetClass, load);
                    ctClasses.add(targetClass);
                });
            });
        }
        return ctClasses;
    }
}