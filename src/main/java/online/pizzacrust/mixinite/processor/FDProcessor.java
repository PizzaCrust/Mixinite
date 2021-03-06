package online.pizzacrust.mixinite.processor;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import online.pizzacrust.mixinite.FallbackMain;
import online.pizzacrust.mixinite.MixinFallbackMain;
import online.pizzacrust.mixinite.transform.LoggablePlugin;

public class FDProcessor extends LoggablePlugin implements Processor{
    private final Mappings mappings = new Mappings();

    public FDProcessor() {
        super("FDProcessor");
    }

    @Override
    public Mappings process(CtClass mixin, CtClass target, Mappings srg) {
        for (CtField mixinField : mixin.getDeclaredFields()) {
            Optional<FieldRef> unmapped = srg.getUnmappedFieldMapping(target, mixinField);
            unmapped.ifPresent((fieldRef) -> {
                log("Found mappings for " + mixinField.getName() + "!");
                try {
                    FieldRef ori = new FieldRef(mixinField.getDeclaringClass().getName().replace
                            ('.', '/'),
                            mixinField.getName());
                    FieldRef remapped = new FieldRef(mixinField.getDeclaringClass().getName()
                            .replace('.', '/'),
                            fieldRef.getName());
                    log(ori.toString() + " to " + remapped.toString());
                    mappings.fieldRefs.put(ori, remapped);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return mappings;
    }

    public static void main(String... args) throws Exception {
        CtClass mixinFallback = ClassPool.getDefault().getCtClass(MixinFallbackMain.class.getName
                ());
        CtClass fallback = ClassPool.getDefault().getCtClass(FallbackMain.class.getName());
        Mappings srg = new Mappings(new File("fallbackSrg.srg"));
        Mappings newSrg = new FDProcessor().process(mixinFallback, fallback, srg);
        File output = new File("newFDSrg.srg");
        Files.write(output.toPath(), newSrg.toLines(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

}
