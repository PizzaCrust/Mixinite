package online.pizzacrust.mixinite.transform;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A plugin that implements interface overlapping.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class InterfacePlugin implements MixinTransformationPlugin {
    private boolean contains(List<CtClass> interfaces, CtClass interfaceClass) {
        for (CtClass ctClass : interfaces) {
            if (ctClass.getName().equals(interfaceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    private List<CtClass> getNewInterfaces(CtClass mixin, CtClass ctClass) {
        ArrayList<CtClass> interfaces = new ArrayList<>();
        try {
            interfaces.addAll(Arrays.asList(mixin.getInterfaces()));
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        try {
            for (CtClass regularInterface : ctClass.getInterfaces()) {
                if (contains(interfaces, regularInterface)) {
                    interfaces.remove(regularInterface);
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return interfaces;
    }

    interface A {

    }

    interface B {

    }

    interface C {

    }

    interface D {

    }

    class Lol implements C, D {

    }

    class Mixin implements A, B, D {

    }

    @Override
    public void handle(CtClass mixin, CtClass ctClass) {
         List<CtClass> newInterfaces = getNewInterfaces(mixin, ctClass);
         for (CtClass newInterface : newInterfaces) {
             ctClass.addInterface(newInterface);
         }
    }

    public static void main(String... args) throws Exception {
        CtClass lolClass = ClassPool.getDefault().getCtClass(Lol.class.getName());
        CtClass mixinClass = ClassPool.getDefault().getCtClass(Mixin.class.getName());
        new InterfacePlugin().handle(mixinClass, lolClass);
        for (CtClass face : lolClass.getInterfaces()) {
            System.out.println(face.getName());
        }
    }

}
