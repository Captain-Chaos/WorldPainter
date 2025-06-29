# Building WorldPainter
## Installing dependencies
WorldPainter needs some dependencies that are not in public Maven repos and cannot be distributed on WorldPainter's private repo due to their licence. You need to install these dependencies into your local Maven repo manually:
### JIDE Docking Framework
For the docks, WorldPainter uses the [JIDE Docking Framework](https://www.jidesoft.com/products/dock.htm), which is a commercial product. For development, you can download an evaluation version of the product [here](https://www.jidesoft.com/evaluation/), with user ID and password documented [here](https://www.jidesoft.com/forum/viewtopic.php?t=10) (note that you need to create a forum account to access the second link). The evaluation version will expire after two months, but you can keep downloading it again whenever it expires for two more months of development time.

Once you have your copy, install the `jide-common.jar`, `jide-dock.jar` and `jide-plaf-jdk7.jar` files in your local Maven repository. If necessary, update the version numbers in the pom.xml of the WPGUI module if you downloaded a different version!

## Set up Maven toolchain
WorldPainter uses the [Maven toolchain framework](https://maven.apache.org/guides/mini/guide-using-toolchains.html) to find the JDK it needs. You need to follow the instructions on that page to configure a toolchain of type jdk and version 17 pointing to a Java 17 JDK. Note that it has not been tested whether WorldPainter will run correctly on older Java versions if you substitute a newer JDK for version 17, although in theory that should work.

## Build WorldPainter
Once all dependencies are installed and the toolchains set up you can build WorldPainter from the command line or using your favourite IDE by invoking the `install` goal on the `WorldPainter` module. There are some rudimentary tests, but they take a while to run and don't contribute much, so I recommend skipping them by adding `-DskipTests=true`.

## Run WorldPainter
Once it is built, you can run WorldPainter by invoking the `exec:exec` goal on the `WPGUI` module, or by running the main class: `org.pepsoft.worldpainter.Main`.

## Develop WorldPainter
For a few pointers, pitfalls and gotchas about developing WorldPainter, see [this page](https://www.worldpainter.net/trac/wiki/DevelopingWorldPainter).

## More details
For a more detailed description of the build process, see: https://www.worldpainter.net/doc/building.
