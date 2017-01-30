package online.pizzacrust.mixinite;

import online.pizzacrust.mixinite.FallbackMain;
import online.pizzacrust.mixinite.Mixin;
import online.pizzacrust.mixinite.launch.Meow2;
import online.pizzacrust.mixinite.transform.InjectorPlugin;
import online.pizzacrust.mixinite.transform.MethodOverlapPlugin;

@Mixin(FallbackMain.class)
public class MixinFallbackMain implements FallbackMain.Meow {

    @MethodOverlapPlugin.IgnoreMethodOverlapping
    @InjectorPlugin.Inject(line = 5)
    public static void main(String[] args, InjectorPlugin.CallbackMetadata callbackMetadata) {
        System.out.println("Fallback mixins have loaded! Mixins are operational!");
        callbackMetadata.setCancelled(true);
        FallbackMain.Meow meow = (FallbackMain.Meow) new FallbackMain();
        System.out.println(meow.meow());
        Meow2 meow2 = (Meow2) new FallbackMain();
        System.out.println(meow2.meow2());
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
