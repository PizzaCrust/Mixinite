package online.pizzacrust.mixinite;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import online.pizzacrust.mixinite.FallbackMain;
import online.pizzacrust.mixinite.Mixin;
import online.pizzacrust.mixinite.launch.Meow2;
import online.pizzacrust.mixinite.transform.AccessTransformerPlugin;
import online.pizzacrust.mixinite.transform.ConstructorModifierTransformerPlugin;
import online.pizzacrust.mixinite.transform.InjectorPlugin;
import online.pizzacrust.mixinite.transform.MethodOverlapPlugin;

@Mixin(FallbackMain.class)
@AccessTransformerPlugin.AccessTransform(entries = {@AccessTransformerPlugin.AccessTransform
        .Entry(name = "accessChange", desc = "()V", type = AccessTransformerPlugin
        .AccessTransform.Type.METHOD, access = Modifier.PUBLIC)})
public class MixinFallbackMain implements FallbackMain.Meow {

    private final String abc = "a";

    @MethodOverlapPlugin.IgnoreMethodOverlapping
    @ConstructorModifierTransformerPlugin.ConstructorModify(all = false)
    public void handleConstructorEnd(String a, InjectorPlugin.CallbackMetadata callbackMetadata) {
        System.out.println("Constructor selection works (" + a + ")");
    }

    @MethodOverlapPlugin.IgnoreMethodOverlapping
    @InjectorPlugin.Inject(line = 5)
    public static void main(String[] args, InjectorPlugin.CallbackMetadata callbackMetadata) {
        System.out.println("Fallback mixins have loaded! Mixins are operational!");
        callbackMetadata.setCancelled(true);
        FallbackMain.Meow meow = (FallbackMain.Meow) new FallbackMain();
        System.out.println(meow.meow());
        Meow2 meow2 = (Meow2) new FallbackMain();
        System.out.println(meow2.meow2());
        System.out.println("Checking access transformer...");
        for (Method method : FallbackMain.class.getDeclaredMethods()) {
            if (method.getName().equals("accessChange")) {
                System.out.println("is_public = " + Modifier.isPublic(method.getModifiers()));
                System.out.println("is_private = " + Modifier.isPrivate(method.getModifiers()));
                assert Modifier.isPublic(method.getModifiers());
            }
        }
        System.out.println("Testing constructor...");
        new FallbackMain("meow");
    }

    @MethodOverlapPlugin.IgnoreMethodOverlapping
    @InjectorPlugin.Inject()
    public String meow2(InjectorPlugin.CallbackMetadataReturnable<String>
                               stringCallbackMetadataReturnable) {
        stringCallbackMetadataReturnable.setCancelled(true);
        stringCallbackMetadataReturnable.setReturnObj("lol2");
        return null;
    }

    @Override
    public String meow() {
        return "lol";
    }

}
