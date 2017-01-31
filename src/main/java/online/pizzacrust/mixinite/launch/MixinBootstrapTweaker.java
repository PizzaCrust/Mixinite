package online.pizzacrust.mixinite.launch;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import online.pizzacrust.mixinite.MixiniteBootstrap;

public class MixinBootstrapTweaker implements ITweaker {
    private List<String> args;
    private String main = "online.pizzacrust.mixinite.FallbackMain";

    @Override
    public void acceptOptions(List<String> list, File file, File file1, String s) {
        this.args = list;
        final boolean[] capture = new boolean[1];
        final String[] possibleMain = {null};
        args.forEach((arg) -> {
            if (capture[0]) {
                possibleMain[0] = arg;
            }
            if (arg.equalsIgnoreCase("--main")) {
                capture[0] = true;
            }
        });
        if (possibleMain[0] != null) {
            System.out.println("Custom main class found: " + possibleMain[0]);
            this.main = possibleMain[0];
        } else {
            try {
                Manifest manifest = new Manifest(getClass().getClassLoader().getResourceAsStream
                        ("META-INF/MANIFEST.MF"));
                String mainClass = manifest.getMainAttributes().getValue("Secondary-Main-Class");
                if (mainClass != null) {
                    System.out.println("Found main class! (" + mainClass + ")");
                    this.main = mainClass;
                }
            } catch (IOException e) {
                System.out.println("Couldn't find main class, using FallbackMain.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
        //triggerTransform(launchClassLoader, FallbackMain.class.getName());
        //List<String> classes = new ArrayList<>();
        //ClassFinder.findClasses((className) -> {
        //    if (!isBanned(className)) {
        //        //System.out.println(className);
        //        classes.add(className);
        //    }
        //    return true;
        //});
        getClassPath(launchClassLoader).ifPresent((classInfos) -> {
            List<String> classpath = new ArrayList<>();
            classInfos.forEach((classInfo) -> classpath.add(classInfo.getName()));
            System.out.println("Discovered " + classpath.size() + " classes!");
            System.out.println("Retrieving CtClasses from classpath...");
            //classpath.forEach((className) -> this.triggerTransform(launchClassLoader, className));
            List<CtClass> ctClasses = new ArrayList<>();
            classpath.forEach((className) -> {
                try {
                    ctClasses.add(ClassPool.getDefault().getCtClass(className));
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            });
            System.out.println("Activating bootstrap...");
            try {
                new MixiniteBootstrap(ctClasses).init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        //launchClassLoader.registerTransformer(MixinBootstrapTransformer.class.getName());
        //System.out.println("Triggering transformations for " + classes.size() + "...");
        //classes.forEach((className) -> this.triggerTransform(launchClassLoader, className));
        //System.out.println("Transforming " + classes.size() + "...");
        //try {
        //    new MixiniteBootstrap(MixinBootstrapTransformer.CLASSPATH).init();
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
    }

    private Optional<Stream<ClassPath.ClassInfo>> getClassPath(LaunchClassLoader
                                                                             launchClassLoader) {
        try {
            return Optional.of(ClassPath.from(launchClassLoader).getAllClasses().stream().filter(
                    (classInfo) -> !classInfo.getPackageName().startsWith("com.google.guava")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void triggerTransform(LaunchClassLoader classLoader, String className) {
        try {
            classLoader.findClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getLaunchTarget() {
        return main;
    }

    @Override
    public String[] getLaunchArguments() {
        return args.toArray(new String[args.size()]);
    }

    public static void main(String... args) {
        List<String> args1 = Arrays.asList("--main", "online.pasda.sdsd.meow");
        final boolean[] capture = {false};
        args1.forEach((arg) -> {
            if (capture[0]) {
                System.out.println(arg);
                System.exit(0);
            }
            if (arg.equalsIgnoreCase("--main")) {
                capture[0] = true;
            }
        });
    }

}
