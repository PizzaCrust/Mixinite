package online.pizzacrust.mixinite.processor;

import javassist.CtClass;

public interface Processor {

    Mappings process(CtClass mixin, CtClass target, Mappings srg);

}
