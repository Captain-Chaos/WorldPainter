var world = wp.getWorld().fromFile('src/test/resources/Generated World.world').go();

print(wp.getBiomeId().fromWorld(world).withName('minecraft:plains').go());

try {
    print(wp.getBiomeId().fromWorld(world).withName('blah').go());
} catch (e) {
    print(e);
}

print(wp.getBiomeId().fromWorld(world).withName('Warm Ocean').go());

print(wp.getBiomeId().fromWorld(world).withName('minecraft:cherry_grove').go());

print(wp.getBiomeId().fromWorld(world).withName('Mangrove Swamp').go());

// TODO test custom biomes