// Load the height map from disk
var heightMap = wp.getHeightMap().fromFile('test_res/heightmap.png').go();

// Create a new WorldPainter world from the height map
var world = wp.createWorld().fromHeightMap(heightMap).go();

// Use the same height map to set the default terrain. For the terrain indices
// to use, see: https://www.worldpainter.net/doc/scripting/legacy/terraintypevalues
wp.applyHeightMap()
        .heightMap(heightMap)
        .world(world)
        .applyToTerrain()
        .fromLevels(0, 64).toTerrain(36) // Beaches
        .fromLevels(65, 96).toTerrain(0) // Grass
        .fromLevels(97, 112).toTerrain(3) // Permadirt
        .fromLevels(113, 255).toTerrain(29) // Rock
        .go();

// Apply the Frost layer above 120, again using the same height map as input
var frostLayer = wp.getLayer().name('Frost').go();
wp.applyHeightMap()
        .heightMap(heightMap)
        .world(world)
        .applyToLayer(frostLayer)
        .fromLevels(0, 119).toLevel(0)   // Make sure to remove any Frost the
        .fromLevels(120, 255).toLevel(1) // height map import might have added
        .go();

// Load the value map for creating the rivers
var riverMap = wp.getHeightMap().fromFile('test_res/rivermask.png').go();

// Load the actual river layer
var riverLayer = wp.getLayer().fromFile('test_res/River.layer').go();

// Paint the river layer onto the world using the value map. The default mapping
// will suffice in this case, since it will apply the layer anywhere the value
// map is not completely black
wp.applyHeightMap()
        .heightMap(riverMap)
        .world(world)
        .applyToLayer(riverLayer)
        .go();

// Save the result to disk
wp.saveWorld()
        .world(world)
        .toFile('test_res/output.world')
        .go();