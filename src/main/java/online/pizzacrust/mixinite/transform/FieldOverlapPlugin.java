package online.pizzacrust.mixinite.transform;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a field overlapping concept implementation as a plugin.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public class FieldOverlapPlugin implements MixinTransformationPlugin {

    private Optional<CtField> getCtField(CtField ctField, CtClass newClass) {
        for (CtField ctField1 : newClass.getDeclaredFields()) {
            if (ctField1.getName().equals(ctField.getName()) && ctField1.getModifiers() ==
                    ctField.getModifiers()) {
                return Optional.of(ctField1);
            }
        }
        return Optional.empty();
    }

    private List<CtField> getChangedFields(CtClass mixin, CtClass ctClass) {
        List<CtField> changedMembers = new ArrayList<>();
        for (CtField originalMember : ctClass.getDeclaredFields()) {
            Optional<CtField> mixinMember = getCtField(originalMember, mixin);
            mixinMember.ifPresent(changedMembers::add);
        }
        return changedMembers;
    }

    private List<CtField> getNewFields(CtClass mixin, CtClass ctClass) {
        List<CtField> newMembers = new ArrayList<>();
        for (CtField mixinMember : mixin.getDeclaredFields()) {
            Optional<CtField> originalMember = getCtField(mixinMember, ctClass);
            if (!originalMember.isPresent()) {
                newMembers.add(mixinMember);
            }
        }
        return newMembers;
    }

    private void log(String msg) {
        System.out.println("FieldOverlapPlugin: " + msg);
    }

    private void log(String msg, String... format) {
        log(String.format(msg.replace("{}", "%s"), (Object[]) format));
    }

    public static void main(String... args) throws Exception {
        ClassPool classPool = ClassPool.getDefault();
        new FieldOverlapPlugin().log("{} lol {}", "Hi", "bye");
        CtClass mixinClass = classPool.getCtClass(ExampleClass.MixinExampleClass.class.getName());
        CtClass exampleClass = classPool.getCtClass(ExampleClass.class.getName());
        new FieldOverlapPlugin().handle(mixinClass, exampleClass);
        URLClassLoader diffLoader = new URLClassLoader(new URL[0]);
        Class<?> jvmClass = exampleClass.toClass(diffLoader);
        for (Field field : jvmClass.getDeclaredFields()) {
            field.setAccessible(true);
            Constructor classConstructor = jvmClass.getConstructor();
            classConstructor.setAccessible(true);
            System.out.println(field.getName() + ": " + field.getType().getSimpleName() + " = " +
                    field.get(classConstructor.newInstance()));
        }
    }

    static class ExampleClass {

        private int meow = 0;

        public ExampleClass() {}

        static class MixinExampleClass {

            private int meow = 1;
            private int dog = 2;

        }

    }

    private Optional<Object> getInitValue(Field field) {
        try {
            Object classInstance = field.getDeclaringClass().newInstance();
            field.setAccessible(true);
            return Optional.of(field.get(classInstance));
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Field getField(CtField field) {
        try {
            return Class.forName(field.getDeclaringClass().getName()).getDeclaredField(field
                    .getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Optional<CtField.Initializer> inferInitializer(Object initVal) {
        if (initVal.getClass() == int.class || initVal.getClass() == Integer.class) {
            return Optional.of(CtField.Initializer.constant((Integer) initVal));
        }
        if (initVal.getClass() == Double.class || initVal.getClass() == double.class) {
            return Optional.of(CtField.Initializer.constant((Double) initVal));
        }
        if (initVal.getClass() == String.class) {
            return Optional.of(CtField.Initializer.constant((String) initVal));
        }
        if (initVal.getClass() == Boolean.class || initVal.getClass() == boolean.class ) {
            return Optional.of(CtField.Initializer.constant((Boolean) initVal));
        }
        if (initVal.getClass() == Float.class || initVal.getClass() == float.class) {
            return Optional.of(CtField.Initializer.constant((Float) initVal));
        }
        if (initVal.getClass() == Long.class || initVal.getClass() == long.class) {
            return Optional.of(CtField.Initializer.constant((Long) initVal));
        }
        return Optional.empty();
    }

    public void handle(CtClass mixin, CtClass ctClass) {
        log("Finding changed fields from {} into {}...", mixin.getSimpleName(), ctClass.getSimpleName());
        List<CtField> changedFields = getChangedFields(mixin, ctClass);
        log("Found {} changed fields from {} into {}...", String.valueOf(changedFields.size()),
                mixin.getSimpleName(), ctClass.getSimpleName());
        log("Finding new fields from {} into {}...", mixin.getSimpleName(), ctClass.getSimpleName());
        List<CtField> newFields = getNewFields(mixin, ctClass);
        log("Found {} new fields from {} into {}...", String.valueOf(newFields.size()), mixin
                .getSimpleName(), ctClass.getSimpleName());
        log("Adding new fields into {}...", ctClass.getSimpleName());
        for (CtField newField : newFields) {
            log("Adding \"{}\" into {}...", newField.getName(), ctClass.getSimpleName());
            Optional<Object> initValueOpt = getInitValue(getField(newField));
            if (initValueOpt.isPresent()) {
              Object initVal = initValueOpt.get();
              Optional<CtField.Initializer> initializerOpt = inferInitializer(initVal);
              initializerOpt.ifPresent((initializer -> {
                  try {
                      ctClass.addField(new CtField(newField, ctClass), initializer);
                  } catch (CannotCompileException e) {
                      e.printStackTrace();
                  }
              }));
            } else {
                try {
                    ctClass.addField(new CtField(newField, ctClass));
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            }
        }
        log("Modifying changed fields into {}...", ctClass.getSimpleName());
        for (CtField changedField : changedFields) {
            Optional<Object> initValueOpt = getInitValue(getField(changedField));
            if (initValueOpt.isPresent()) {
                log("Modifying \"{}\" in {}...", changedField.getName(), ctClass.getSimpleName());
                Optional<CtField> targetFieldOpt = getCtField(changedField, ctClass);
                targetFieldOpt.ifPresent((targetField) -> {
                    try {
                        ctClass.removeField(targetField);
                        Optional<CtField.Initializer> initializerOpt = inferInitializer
                                (initValueOpt.get());
                        initializerOpt.ifPresent((initializer -> {
                            try {
                                ctClass.addField(new CtField(changedField, ctClass), initializer);
                            } catch (CannotCompileException e) {
                                e.printStackTrace();
                            }
                            for (CtConstructor declaredConstructor : ctClass
                                    .getDeclaredConstructors()) {
                                try {
                                    declaredConstructor.insertAfter(changedField.getName() + " = " +
                                            initValueOpt.get() + ";");
                                } catch (CannotCompileException e) {
                                    e.printStackTrace();
                                }
                            }
                        }));
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

}
