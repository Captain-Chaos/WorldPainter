print('WorldPainter version: ' + wp.version);

var world = wp.getWorld()
    .fromFile('src/test/resources/Generated World.world')
    .go();

var layer = wp.getLayer()
    .fromFile('src/test/resources/River.layer')
    .go();

var heightMap = wp.getHeightMap()
    .fromFile('src/test/resources/rivermask.png')
    .go();
    
wp.applyHeightMap(heightMap)
    .shift(-256, -256) // The demo image does not have the origin in the top left corner so we need to shift the colour map
    .toWorld(world)
    .applyToLayer(layer)
    .fromLevels(1, 255).toLevel(1)
    .go();

wp.saveWorld(world)
    .toFile('src/test/resources/output.world')
    .go();