package online.pizzacrust.mixinite.processor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.util.Pair;
import online.pizzacrust.mixinite.FallbackMain;
import online.pizzacrust.mixinite.Mixin;
import online.pizzacrust.mixinite.MixinFallbackMain;
import online.pizzacrust.mixinite.transform.InjectorPlugin;

public class Mappings {

    public final BiMap<MethodRef, MethodRef> methodRefs = HashBiMap.create();
    public final BiMap<FieldRef, FieldRef> fieldRefs = HashBiMap.create();

    public Optional<MethodRef> getUnmappedMethodMapping(CtClass targetClass, CtMethod
            handleMethod) {
        BiMap<MethodRef, MethodRef> methodsInverted = methodRefs.inverse();
        String descriptor = InjectorPlugin.removeCallbackDescriptor(handleMethod.getMethodInfo2()
                .getDescriptor());
        for (Map.Entry<MethodRef, MethodRef> mapping : methodsInverted.entrySet()) {
            MethodRef key = mapping.getKey();
            if (targetClass.getName().replace('.', '/').equals(key.getOwner()) && handleMethod
                    .getName
                    ().equals
                    (key
                    .getName()) && descriptor.equals(key.getDesc())) {
                return Optional.of(mapping.getValue());
            }
        }
        return Optional.empty();
    }

    public Optional<FieldRef> getUnmappedFieldMapping(CtClass targetClass, CtField handleField) {
        BiMap<FieldRef, FieldRef> fieldsInverted = fieldRefs.inverse();
        for (Map.Entry<FieldRef, FieldRef> mapping : fieldsInverted.entrySet()) {
            FieldRef remapped = mapping.getKey();
            if (remapped.getOwner().equals(targetClass.getName().replace('.', '/')) && remapped
                    .getName().equals
                    (handleField.getName())) {
                return Optional.of(mapping.getValue());
            }
        }
        return Optional.empty();
    }


    public Mappings(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        lines.forEach((line) -> {
            if (line.startsWith("FD")) {
                Pair<FieldRef, FieldRef> parsedLine = FieldRef.parseLine(line);
                fieldRefs.put(parsedLine.getKey(), parsedLine.getValue());
            }
            if (line.startsWith("MD")) {
                Pair<MethodRef, MethodRef> parsedLine = MethodRef.parseLine(line);
                methodRefs.put(parsedLine.getKey(), parsedLine.getValue());
            }
        });
    }

    private static Optional<Mixin> getMixinMetadata(CtClass ctClass) {
        try {
            return Optional.of(Class.forName(ctClass.getName()).getAnnotation(Mixin.class));
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static Optional<CtClass> getCtClass(Class<?> clazz) {
        try {
            return Optional.of(ClassPool.getDefault().getCtClass(clazz.getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Mappings genRefMap(File srg, List<CtClass> ctClasses) throws Exception {
        List<CtClass> mixins = new ArrayList<>();
        ctClasses.forEach((ctClass) -> {
            Optional<Mixin> mixinMeta = getMixinMetadata(ctClass);
            mixinMeta.ifPresent((meta) -> mixins.add(ctClass));
        });
        Mappings oriSrg = new Mappings(srg);
        List<Mappings> mappingss = new ArrayList<>();
        mixins.forEach((mixinClass) -> {
            Optional<Mixin> mixinOpt = getMixinMetadata(mixinClass);
            mixinOpt.ifPresent((mixinMeta) -> {
                Optional<CtClass> targetOpt = getCtClass(mixinMeta.value());
                targetOpt.ifPresent((targetClass) -> {
                    Mappings fieldRefMap = new FDProcessor().process(mixinClass, targetClass,
                            oriSrg);
                    Mappings methodRefMap = new MDProcessor().process(mixinClass, targetClass,
                            oriSrg);
                    mappingss.add(fieldRefMap);
                    mappingss.add(methodRefMap);
                });
            });
        });
        return chain(mappingss);
    }

    private static CtClass from(Class<?> clazz) {
        try {
            return ClassPool.getDefault().getCtClass(clazz.getName());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String... args) throws Exception {
        List<CtClass> classpath = Arrays.asList(from(MixinFallbackMain.class), from(FallbackMain
                .class));
        File output = new File("outputRefmap.srg");
        File oriSrg = new File("mixedSrg.srg");
        Mappings newMappings = genRefMap(oriSrg, classpath);
        Files.write(output.toPath(), newMappings.toLines(), StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE);
    }

    public static Mappings chain(List<Mappings> mappingss) {
        return chain(mappingss.toArray(new Mappings[mappingss.size()]));
    }

    public static Mappings chain(Mappings... mappingss) {
        Mappings main = new Mappings();
        for (Mappings mappings : mappingss) {
            mappings.fieldRefs.forEach(main.fieldRefs::put);
            mappings.methodRefs.forEach(main.methodRefs::put);
        }
        return main;
    }

    public Mappings() {}

    public List<String> toLines() {
        ArrayList<String> stringArrayList = new ArrayList<>();
        methodRefs.forEach((ori, remap) -> stringArrayList.add(MethodRef.toSyntax(ori, remap)));
        fieldRefs.forEach((ori, remap) -> stringArrayList.add(FieldRef.toSyntax(ori, remap)));
        return stringArrayList;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String line : toLines()) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

}
