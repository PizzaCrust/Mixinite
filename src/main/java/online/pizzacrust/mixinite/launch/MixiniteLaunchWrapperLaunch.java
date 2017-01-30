package online.pizzacrust.mixinite.launch;

import net.minecraft.launchwrapper.Launch;

import java.util.ArrayList;
import java.util.List;

public class MixiniteLaunchWrapperLaunch {

    public static void main(String... args) throws Exception {
        List<String> launchWrapperArgs = new ArrayList<>();
        launchWrapperArgs.add("--tweakClass");
        launchWrapperArgs.add(MixinBootstrapTweaker.class.getName());
        Launch.main(launchWrapperArgs.toArray(new String[launchWrapperArgs.size()]));
    }

}
