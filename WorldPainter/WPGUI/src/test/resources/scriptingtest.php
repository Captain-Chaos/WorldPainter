<?php
echo 'WorldPainter version: ' . $wp->version . PHP_EOL;

if ($argc !== 3) {
    exit('Usage: wpscript scriptingtest.js <worldfile> <exportdir>');
}

$filename = $argv[1];
$exportdir = $argv[2];
echo 'Exporting world ' . $filename . ' to ' . $exportdir . PHP_EOL;

$world = $wp->loadWorld($filename);
$wp->exportWorld($world, $exportdir);