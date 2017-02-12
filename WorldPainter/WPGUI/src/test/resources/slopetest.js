print('WorldPainter version: ' + wp.version);

var heightMap = wp.getHeightMap()
    .fromFile('src/test/resources/heightmap.png')
    .go();

var world = wp.createWorld()
    .fromHeightMap(heightMap)
    .go();

var frostLayer = wp.getLayer()
    .withName('Frost')
    .go();

var slopeFilter = wp.createFilter()
    .aboveDegrees(30)
    .go();
    
wp.applyLayer(frostLayer)
    .toWorld(world)
    .withFilter(slopeFilter)
    .go();

wp.saveWorld(world)
    .toFile('src/test/resources/output.world')
    .go();