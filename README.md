**Mixin** is a mixin framework for Java using Javassist and hooking to the runtime class-loading process using Mojang's LaunchWrapper system. It's much more complicated than [PizzaMixin](https://github.com/PizzaCrust/PizzaMixin).
### Maven usage
```xml
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
</repositories>
<dependencies>
        <dependency>
            <groupId>com.github.PizzaCrust</groupId>
            <artifactId>Mixinite</artifactId>
            <version>master-SNAPSHOT</version>
        </dependency>
</dependencies>
```
### Gradle usage
```groovy
repositories {
	maven {
	    name = 'jitpack'
	    url = 'https://jitpack.io'
	}
}
dependencies {
    compile 'com.github.PizzaCrust:Mixinite:master-SNAPSHOT'
}
```

### Version History

**Version** | **Features / Changes** | **Date**
--- | --- | ---
**1.0-SNAPSHOT** | Mixin support | Janurary 2017