println('WorldPainter version: ' + wp.version);

if (arguments.length !== 2) {
    println('Usage: wpscript scriptingtest.js <worldfile> <exportdir>');
    exit(1);
}

var filename = arguments[0];
var exportdir = arguments[1];

println('Loading world ' + filename);
var world = wp.world.fromFile(filename).go();

println('Exporting world to ' + exportdir);
wp.exportWorld(world).toDirectory(exportdir).go();