# Simple Resources
A small library focused on simlifying the loading of resources and creation of data driven content.
Simplifies loading of resources from data packs, resource packs and the config directory.
It can be installed client-side only, server-side only or both, but does not provide any content on its own. It allows
loading of not only resources serialized by minecrafts codec system, but also other types of files. It also allows adding
custom ```DynamicOps``` so your new codec-serialized resources can be stored in your favourite format. Also adds other
utilities for easing the development of data driven content

You can add it as a dependency by adding
```
repositories {
	maven { url 'https://maven.nucleoid.xyz' }
}

dependencies {
	modImplementation "maven.modrinth:simple-resources:1.0.0"
}
```
Since it is a small library, you can include it in your mods jar file
```
dependencies {
	modImplementation include("maven.modrinth:simple-resources:1.0.0")
}
```

## [Check out the wiki!](https://github.com/Yorick-06/SimpleResources/wiki)