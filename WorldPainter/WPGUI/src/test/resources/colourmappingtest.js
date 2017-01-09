print('WorldPainter version: ' + wp.version);

var world = wp.getWorld()
    .fromFile('src/test/resources/Generated World.world')
    .go();

var colourMap = wp.getHeightMap()
    .fromFile('src/test/resources/colourmap.png')
    .go();
    
var biomesLayer = wp.getLayer()
    .withName('Biomes')
    .go();
    
wp.applyHeightMap(colourMap)
    .shift(-256, -256) // The demo image does not have the origin in the top left corner so we need to shift the colour map
    .toWorld(world)
    .applyToLayer(biomesLayer)
    .fromColour(255, 0, 0).toLevel(4) // Red -> Forest
    .fromColour(0, 255, 0).toLevel(5) // Green -> Taiga
    .fromColour(0, 0, 255).toLevel(6) // Blue -> Swampland
    .go();

wp.saveWorld(world)
    .toFile('src/test/resources/output.world')
    .go();