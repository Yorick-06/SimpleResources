# Simple Resources
This library mod focuses on simplifying loading of resources from data packs, resource packs and the config directory.
It can be installed client-side only, server-side only or both, but does not provide any content on its own. It allows
loading of not only resources serialized by minecrafts codec system, but also other types of files. It also allows adding
custom ```DynamicOps``` so your new codec-serialized resources can be stored in your favourite format. Also adds the
```ClassFieldsCodec``` which uses reflection to create a codec from fields defined in the class.

# Resource types
- Config
- Reloadable config
- Config tree
- Reloadable config tree
- Data pack resource
- Resource pack resource

# Adding a data pack resource
This registers a new data pack resource using fabrics api.

- The identifiers namespace should be your mod id, the path is the resource name - such as minecrafts "tags" or "advancement"
- The codec is the codec used for loading a single file in the data pack.
- The loaded values are stored as a ```Map``` with ```Identifier``` as a key and the value loaded from that file.
  The identifiers namespace is the namespace under which it was specified in the data pack, the path is the path to the
  file excluding the resource name.
- Gets reloaded every time data packs get reloaded (world load, /reload command).
```
ResourceKey<Map<Identifier, List<Item>>> itemCollections = SimpleResources.datapackResource(Identifier.of(MOD_ID, "item_collections"), Registries.ITEM.getCodec().listOf());
```
It also has an overloaded method for adding a reload listener which gets invoked when a data pack gets reloaded.
```
Consumer<Map<Identifier, List<Item>>> reloadListener = collections -> System.out.println("Loaded " + collections.size() + " item collections.");
ResourceKey<Map<Identifier, List<Item>>> itemCollections = SimpleResources.datapackResource(Identifier.of(MOD_ID, "item_collections"), Registries.ITEM.getCodec().listOf(), reloadListener);
```
The ```ResourceKey``` can be used to get the currently loaded values.
```
Map<Identifier, List<Item> loadedCollections = itemCollections.getValue();
```

# Adding a resourcepack resource
Works the same as adding a data pack resource, but loads from a resource pack and reloads when resource packs reload.
For registering use
```
SimpleResources.resourcepackResource(Identifier, Codec);
```
It also has an overloaded method for adding a reload listener which gets invoked when a resource pack gets reloaded.
```
SimpleResources.resourcepackResource(Identifier, Codec, Consumer);
```

# Loading from the config directory

## A simple config
A simple config can be created just by specifying an id, a default value factory, and a codec.

- The identifier is used for creating the path, the namespace of the identifier should be your mod id, since the file will
  be stored in ```config/identifier_namespace/```. The identifier's path is the name, but it can include ```/``` to act
  as a directory separator. The path does not need to specify the file extension and by default the ```.json``` extension
  is used. Custom extensions can also be used, but a file parser for them needs to be registered. By default, only ```.json```
  files are supported. 
- The default value factory is invoked any time the config cannot be loaded, either due to an exception while loading the
  config, or because the file is missing. If the file is missing it is created and the default value is written into it.
- The codec is just the codec used to serialize the config class.
```
List<Item> specialItems = SimpleResources.config(Identifier.of(MOD_ID, "special_items"), List::of, Registries.ITEM.getCodec().listOf());
```
This config gets loaded once on game launch.

## A reloadable config
This config is very similar to a simple config. It can also be created just by specifying an id, a default value factory,
and a codec. The parameters also work the same as while creating a simple config, but it returns a ```ReloadableResourceKey```.
```
ReloadableResourceKey<List<Item>> specialItems = Configs.simpleReloadable(Identifier.of(MOD_ID, "special_items"), List::of, Registries.ITEM.getCodec().listOf());
```
It also has an overloaded method for adding a reload listener which gets invoked when the data get reloaded.
```
Consumer<List<Item>> reloadListener = items -> System.out.println("Reloaded my special items");
ReloadableResourceKey<List<Item>> specialItems = SimpleResources.simple(Identifier.of(MOD_ID, "special_items"), List::of, Registries.ITEM.getCodec().listOf(), reloadListener);
```
The ```ReloadableResourceKey``` can be used to get the currently loaded value.
```
List<Item> loadedSpecialItems = specialItems.getValue();
```
But it can also be used to force a reload of the config. Reloading requires a ```Consumer<Exception>``` which gets invoked
when an exception is thrown during the reload.
```
Consumer<Exception> errorHandler = exception -> System.out.println("Error while reloading special items " + exception.getMessage());
specialItems.reload(errorHandler);
```
This config gets loaded once on game launch and every time it is forced to reload either by using the ```reload()``` method
or by using the /reloadSimpleResources or /reloadSimpleServerResources based on the environment.

## Creating a resource tree config
A file tree config works similarly to a data pack or resource pack resource - it goes through the specified directory and
all its subdirectories and tries to load every file it finds. The main reason to use this instead of a data
pack or resource pack resource is that it gets loaded immediately after getting created, which is much earlier in the loading
process than a data pack or a resource pack. This allows the values from those files to impact things such as registries.

- The identifier points to a directory, not a file - otherwise the same rules as when creating a simple config apply
- It does not require a default factory, since if the file throws an error, it will just not get added to the loaded files.
- The loaded value gets returned as a ```Map``` where the key is a ```String``` representing the file path and the value
  is the value which was loaded from the file at that path.
```
Map<String, List<Item>> itemCollections = SimpleResources.resourceTree(Identifier.of(MOD_ID, "item_collections"), Registries.ITEM.getCodec().listOf());
```
It gets loaded once on game launch, if the specified directory is missing, it gets created - but no files are created.

## Creating a reloadable resource tree config
The difference between a reloadable resource tree config and a resource tree config is the same as between the simple
config and simple reloadable config.
```
ReloadableResourceKey<Map<String, List<Item>>> itemCollections = SimpleResources.reloadableResourceTree(Identifier.of(MOD_ID, "item_collections"), Registries.ITEM.getCodec().listOf());
```
It also has an overloaded method for adding a reload listener which gets invoked when the data get reloaded. 
```
Consumer<Map<String, List<Item>>> reloadListener = loaded -> System.out.println("Loaded " + loaded.size() + " item collections.");
ReloadableResourceKey<Map<String, List<Item>>> itemCollections = SimpleResources.reloadableResourceTree(Identifier.of(MOD_ID, "itemCollections"), Registries.ITEM.getCodec().listOf(), reloadListener);
```
And it can also be forced to reload, the only difference is that this error handler can get called multiple times.
```
Consumer<Exception> errorHandler = exception -> System.out.println("Error while reloading! " + exception.getMessage());
itemCollections.reload(errorHandler);
```

# Loading non-codec files
In all the previous examples instead of passing a ```Codec``` to the method, you can pass a ```ResourceReadWriter```.
If you pass a codec it gets wrapped with ```CodecResourceReadWriter``` which checks the file extension, selects a
```DynamicOps``` instance based on the extension and tries to parse the content. If you want to parse
some files which do not include data serialized by codecs you need to create a custom ```ResourceReadWriter```.
A great example would be minecrafts .mcfunction files. Here is an example of adding a text ```ResourceReadWriter```,
it won't care about the file type and just try to read it as text.
```
public class TextResourceReadWriter imlements ResourceReadWriter<List<String>> {
   
    @Override
    public List<String> read(String fileExtension, Reader reader) throws Exception {
        List<String> result = new ArrayList<>();
        Scanner scanner = new Scanner(reader);
        while (scanner.hasNext()) {
            result.add(scanner.next());
        }
        
        return result;
    }

    @Override
    public void write(String fileExtension, Writer writer, List<String> data) throws Exception {
        PrintWriter printWriter = new PrintWriter(writer);
        for(String line : data) {
            printWriter.println(line);
        }
    }
}
```
You can also modify it to only read ```txt``` files
```
public class TextResourceReadWriter imlements ResourceReadWriter<List<String>> {
   
    @Override
    public List<String> read(String fileExtension, Reader reader) throws Exception {
        if(!"txt".equals(fileExtension)) {
            throw new IllegalArgumentException("This file does not have the txt estension!");
        }
    
        List<String> result = new ArrayList<>();
        Scanner scanner = new Scanner(reader);
        while (scanner.hasNext()) {
            result.add(scanner.next());
        }
        
        return result;
    }

    @Override
    public void write(String fileExtension, Writer writer, List<String> data) throws Exception {
        if(!"txt".equals(fileExtension)) {
            throw new IllegalArgumentException("This file does not have the txt estension!");
        }
        PrintWriter printWriter = new PrintWriter(writer);
        for(String line : data) {
            printWriter.println(line);
        }
    }
}
```

# Registering custom dynamic ops
Using different file formats (such as ```toml``` or ```yml```) is not supported by default, but they can be added.
The arguments for this method are:

- T is the base class of your format (json uses ```JsonElement```)
- The file extension of this format
- The DynamicOps instance of your format 
- A Function which receives a reader of the input data and returns T
- A BiConsumer which writes the received T into the File
```
SimpleResources.registerOps(String fileExtension, DynamicOps<T> ops, Function<Reader, T> readerParser, BiConsumer<File, T> writer);
```
Json is registered with
```
registerOps("json", JsonOps.INSTANCE, JsonParser::parseReader, CodecResourceReadWriter::writeJson);
```
The registered extension is supported for anything serialized/deserialized by this mod, this includes files in the config
directory, but also files from data packs and resource packs. It also supports combinations of files - the same data pack
resource can be specified in a .json file or your custom registered ops and both will get loaded.

# Commands

## /reloadConfigs
This is a client command, it reloads all reloadable configs and reloadable resource tree configs present on the
physical client. Usage:

- ```/reloadConfigs all```
- ```/reloadConfigs only <configId>```

## /reloadServerConfigs
This is a command present only on the dedicated server, it reloads all reloadable configs and reloadable resource tree
configs present on the dedicated server. This command requires operator status and can be used by players which do not
have this mod installed on their client. Usage:

- ```/reloadServerConfigs all```
- ```/reloadServerConfigs only <configId>```

# Utils
The ```ResourceUtil``` has a few helper methods
- ```String getFileExtension(Identifier id)``` gets the file extension or null from the path of the identifier
- ```String getFileExtension(String path)``` gets the file extension or null from the path
- ```Identifier removeFileExtension(Identifier id)``` removes the file extension from the path of the identifier
- ```String removeFileExtension(String path)``` removes the file extension from the path

This is useful because the file extensions do not get removed when parsing, since files of the same name but different
extension might be present in the same directory. Removing the extension would give them the same id and only one of
them could get loaded.

While the expected file separator is ```/```, if a different separator is used it will only fail in some edge cases.
Such as ```directory\directory.2\file``` will incorrectly think that ```2\file``` is the extension. But if ```directory/directory.2/file```
is passed to the method the correct result of ```null``` will be returned.

# ClassFieldsCodec
This class can be used to create a codec from fields in a class using reflection. This is useful if you have a class
with a lot of fields which need to be serialized, and would just end up as a really large ```MapCodec```. Only some types
are supported by default, but extra codecs can be easily added using the builder. Types supported by default:
- ```boolean```
- ```byte```
- ```int```
- ```float```
- ```double```
- ```long```
- ```String``` 
- ```Item```
- ```EntityType```
- ```Block```
- ```Identifier```
- ```ItemStack```
- If you think that any type is missing and should be included in the defaults let me know.

If a class does not have a codec specified and is an enum, a default codec using ```Enum.name()``` and
```Enum.valueOf()``` gets created. 

## Creating a simple ClassFieldsCodec
An example class
```
public class MyConfig {
    public final Identifier ID = Identifier.of(MOD_ID, "identifier_1");
    public final String STRING = "my_string";
    public final int NUMBER = 6;
}
```
Can be converted to a codec just by doing
```
Codec<MyConfig> codec = ClassFieldsCodec.of(MyConfig.class);
```
Since this method creates the default value factory using reflection, the class must have a default no-parameter constructor.
If your class must have a constructor, you can use the overloaded method which adds a parameter for a default value factory.
```
Codec<MyConfig> codec = ClassFieldsCodec.of(MyConfig.class, MyConfig::new);
```
Creating a default value is needed, because when deserializing the codec it first creates the default class and then modifies
its values to match the received values.

## Using the builder
The builder allows you to have greater control over the config. You can create a builder by doing.
```
ClassFieldsCodec.builder(MyConfig.class);
```
Or use the option with a default factory.
```
ClassFieldsCodec.builder(MyConfig.class, MyConfig::new);
```
The builder has two methods used for adding codecs.
```
codecBuilder.withCodec(Codec<V> codec, Class<V> clazz)
```
which allows you to add a codec for a class. It can also be used to overwrite the default codec for a class. Since by default
the codec used for ```String``` is ```Codec.STRING``` it allows empty strings, if your configs values cannot be empty, you can use
```
codecBuilder.withCodec(Codecs.NON_EMPTY_STRING, String.class)
```
This will force all fields of type String in your class to get serialized by the non-empty string codec.
The second method
```
codecBuilder.withCodec(Codec<V> codec, String... fieldIds)
```
Can be used to specify codecs based on the field id, this is useful if fields of the same class use different codecs or
if  The best example is any list codec, since using the previous method would serialize all lists with the same codec.
This works fine if you have lists with the same type parameters
```
public final List<Item> myItems = List.of();
public final List<Item> myItems2 = List.of();
```
but fails if you have lists with different type parameters
```
List<Item> myItems = List.of();
List<ItemStack> myStacks = List.of();
```
This can be fixed by using
```
codecBuilder.withCodec(Registries.ITEM.getCodec().listOf(), "myItems");
codecBuilder.withCodec(ItemStack.CODEC.listOf(), "myStacks");
```
since the ```fieldIds``` parameter is varargs, multiple field ids can easily be specified for one codec.
```
codecBuilder.withCodec(Registries.ITEM.getCodec().listOf(), "myItems");
codecBuilder.withCodec(ItemStack.CODEC.listOf(), "myStacks", "myStacks2", "myStacks3");
```
The id-based method takes priority over the specification for the class which means that if you have a lot of lists of
the same type and only one list of a different type instead of writing them all out you can do
```
//serialize all fields of type list with the item codec
codecBuilder.withCodec(Registries.ITEM.getCodec().listOf(), List.class);
//but the field with the id "myStacks" using a different codec
codecBuilder.withCodec(ItemStack.CODEC.listOf(), "myStacks");
```
Another useful method is specifying the post processor which gets invoked when a values are assigned to a newly created
instance. This can be used to modify the class or check for conflicting values 
```
Function<MyConfig, DataResult<MyConfig>> postProcessor = config -> {
    if(config.STRING.equals("string") && config.NUMBER == 5) {
        return DataResult.error(() -> "Illegal combination of values 'string' and '5'");    
    }
    
    return DataResult.success(config);
};
codecBuilder.postProcessor(postProcessor);
```
To receive the actual codec just use
```
Codec<MyConfig> codec = codecBuilder.build();
```
When creating the codec using ```ClassFieldsCodec.builder(Class, Supplier)``` or ```ClassFieldsCodec.of(Class, Supplier)```
the supplier can provide a subclass of the provided class.

Assuming ```MyClass2 extends MyClass```
```
Codec<MyClass2> codec = ClassFieldsCodec.of(MyClass.class, MyClass2::new);
```
This means that only fields specified in ```MyClass``` will get serialized/deserialized, but the codec type and all received
instances will be of ```MyClass2```.

## Annotations
Annotations can be used in the serialized class itself to further modify some behavior.

### @FieldId
This annotation changes the id of the field, by default the id is the fields name. The id is used when serializing/deserializing
the codec and when specifying the codec with ```ClassFieldsCodec.withCodec(Codec<V> codec, String... fieldIds)```

### @Ignore
This annotation marks the field as ignored, this means that it will get ignored for both serialization and deserialization.

### @Optional
By default, any data loaded by this codec must include values for all the fields. By marking this field as ```@Optional```,
if the key is missing the field in the class won't get modified during the loading process.

### @IncludeParent
This annotation can be used on the class to also serialize/deserialize the fields of its parent. The parent also gets
checked for this annotation.

## Restrictions
- No field in the class can have a null value
- The class cannot be a record
