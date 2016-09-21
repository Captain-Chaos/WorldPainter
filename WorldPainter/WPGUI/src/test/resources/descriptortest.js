// -- This is the script descriptor. It must be the at the start of the script,
// -- *before* any non-empty, non-comment lines. Any lines which don't look like
// -- a key-value pair will be ignored, but you can start script descriptor
// -- comments with a -- just to make sure they won't be mistaken for a
// -- key-value pair.
//
// -- The name of the script (optional):
// script.name=Test
//
// -- A short description which will be shown on the screen (optional):
// script.description=Description of test\nwith optional line endings
//
// -- Script parameter descriptors in the form:
// -- script.param.<paramName>.<property>=<value>:
// script.param.firstParam.type=integer
// script.param.firstParam.description=Description of firstParam. Will be shown as tooltip
//
// script.param.param2.type=string
// script.param.param2.description=Parameter names are arbitrary identifiers
//
// script.param.anotherParam.type=percentage
// script.param.anotherParam.description=Parameters can have default values
// script.param.anotherParam.default=50
//
// script.param.file1.type=file
// script.param.file1.description=Parameters can be optional
// script.param.file1.optional=true
//
// script.param.demoParam.type=boolean
// script.param.demoParam.description=Parameters can have different display names
// script.param.demoParam.displayName=Demo parameter
// script.param.demoParam.default=true
//
// script.param.paramFloat.type=float
// script.param.paramFloat.default=12.34
//
// -- The following line is optional; when present the normal command line
// -- parameter text area will be suppressed:
// script.hideCmdLineParams=true

var firstParam = params['firstParam'];
var param2 = params['param2'];
var anotherParam = params['anotherParam'];
var file1 = params['file1'];
var demoParam = params['demoParam'];
var paramFloat = params['paramFloat'];

print('firstParam (integer): ' + firstParam);
print('param2 (string): ' + param2);
print('anotherParam (percentage): ' + anotherParam);
print('file1 (file): ' + ((file1 == null) ? '<not selected>' : file1));
print('demoParam (boolean): ' + demoParam);
print('paramFloat (float): ' + paramFloat);