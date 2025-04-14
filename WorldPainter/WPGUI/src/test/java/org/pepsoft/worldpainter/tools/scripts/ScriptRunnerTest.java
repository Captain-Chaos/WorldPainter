package org.pepsoft.worldpainter.tools.scripts;

import org.junit.Test;
import java.util.HashMap;
import java.util.Objects;

public class ScriptRunnerTest  {
    @Test
    public void testChooseScriptDescriptor() {
        HashMap<String, ScriptRunner.ScriptDescriptor> map = new HashMap<>();

        //empty map
        final ScriptRunner.ScriptDescriptor myNewScriptParams = new ScriptRunner.ScriptDescriptor();
        assert ScriptRunner.selectDescriptor(map, myNewScriptParams) == myNewScriptParams; //should be same object

        //map with null key
        map.put(null, new ScriptRunner.ScriptDescriptor());
        assert ScriptRunner.selectDescriptor(map, myNewScriptParams) == myNewScriptParams; //should be same object

        final String descriptorName = "myName";

        myNewScriptParams.name = descriptorName;
        myNewScriptParams.parameterDescriptors.add(new ScriptRunner.IntegerParameterDescriptor("an Integer parameter"));
        assert myNewScriptParams.parameterDescriptors.size() == 1;


        ScriptRunner.ScriptDescriptor oldScriptParams = new ScriptRunner.ScriptDescriptor();
        oldScriptParams.name = descriptorName;
        map.put(descriptorName, oldScriptParams);

        //name match but parameter mismatch
        assert ScriptRunner.selectDescriptor(map, myNewScriptParams) == myNewScriptParams; //should be same object

        //name match and parameter match
        oldScriptParams.parameterDescriptors.add(new ScriptRunner.IntegerParameterDescriptor("an Integer parameter"));
        assert Objects.equals(oldScriptParams.name, myNewScriptParams.name);
        assert Objects.equals(oldScriptParams.parameterDescriptors.get(0).name, myNewScriptParams.parameterDescriptors.get(0).name);
        assert map.get(descriptorName) == oldScriptParams; //should be same object

        assert ScriptRunner.selectDescriptor(map, myNewScriptParams) == oldScriptParams; //should be same object

        //name match, parameter name match, parameter type mismatch
        oldScriptParams.parameterDescriptors.set(0, new ScriptRunner.StringParameterDescriptor("an Integer parameter"));
        assert ScriptRunner.selectDescriptor(map, myNewScriptParams) == myNewScriptParams; //should be same object

    }
}