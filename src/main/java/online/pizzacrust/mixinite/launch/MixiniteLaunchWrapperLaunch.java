package online.pizzacrust.mixinite.launch;

import net.minecraft.launchwrapper.Launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MixiniteLaunchWrapperLaunch {

    public static void main(String... args) throws Exception {
        List<String> launchWrapperArgs = new ArrayList<>();
        launchWrapperArgs.add("--tweakClass");
        launchWrapperArgs.add(MixinBootstrapTweaker.class.getName());
        launchWrapperArgs.addAll(Arrays.asList(args));
        Launch.main(launchWrapperArgs.toArray(new String[launchWrapperArgs.size()]));
    }

}
