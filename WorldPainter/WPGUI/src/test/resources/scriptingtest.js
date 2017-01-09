print('WorldPainter version: ' + wp.version);

if (arguments.length !== 2) {
    print('Usage: wpscript scriptingtest.js <worldfile> <exportdir>');
    exit(1);
}

var filename = arguments[0];
var exportdir = arguments[1];

print('Loading world ' + filename);
var world = wp.world.fromFile(filename).go();

print('Exporting world to ' + exportdir);
wp.exportWorld(world).toDirectory(exportdir).go();