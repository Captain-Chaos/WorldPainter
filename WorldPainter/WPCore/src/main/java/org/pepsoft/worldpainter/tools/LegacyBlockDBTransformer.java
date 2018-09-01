package org.pepsoft.worldpainter.tools;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class LegacyBlockDBTransformer {
    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(LegacyBlockDBTransformer.class.getResourceAsStream("mc-blocks.json")))) {
            // Read MC block database
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if ((! line.trim().startsWith("#")) && (! line.trim().startsWith("//"))) {
                    sb.append(line);
                }
            }
            @SuppressWarnings("unchecked") // Guaranteed by contents of file
                    List<Map<String, Object>> blockSpecs = (List< Map<String, Object>>) new JSONParser().parse(sb.toString());

            // Find properties which have only one value
            Map<String, Map<String, Set<String>>> allProperties = new HashMap<>();
            for (Map<String, Object> blockSpec: blockSpecs) {
                String name = ((String) blockSpec.get("name")).intern();
                Map<String, Set<String>> propertiesSeenForBlock = allProperties.computeIfAbsent(name, s -> new HashMap<>());
                if (blockSpec.containsKey("properties")) {
                    for (Map.Entry<String, String> entry: ((Map<String, String>) blockSpec.get("properties")).entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        propertiesSeenForBlock.computeIfAbsent(key, s -> new HashSet<>()).add(value);
                    }
                }
            }

            // Dump results to console
            allProperties.forEach((name, propertiesOfBlock) -> {
                System.out.println(name);
                propertiesOfBlock.forEach((key, values) -> System.out.printf("    %s: %s%n", key, values));
            });

            // Remove properties from the block database for which only one
            // value has been seen (meaning they are not encoded in the legacy
            // block ID or data value)
            for (Map<String, Object> blockSpec: blockSpecs) {
                String name = ((String) blockSpec.get("name")).intern();
                Map<String, Set<String>> propertiesSeenForName = allProperties.get(name);
                if (propertiesSeenForName == null) {
                    continue;
                }
                if (blockSpec.containsKey("properties")) {
                    Map<String, String> blockSpecProperties = (Map<String, String>) blockSpec.get("properties");
                    for (Iterator<Map.Entry<String, String>> i = blockSpecProperties.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry<String, String> entry = i.next();
                        String key = entry.getKey();
                        if (propertiesSeenForName.get(key).size() == 1) {
                            i.remove();
                        }
                    }
                    if (blockSpecProperties.isEmpty()) {
                        blockSpec.remove("properties");
                    }
                }
            }

            // Dump the block database
            try (Writer out = new FileWriter("legacy-mc-blocks.json")) {
                JSONArray.writeJSONString(blockSpecs, out);
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Minecraft block database mc-blocks.json from classpath", e);
        } catch (ParseException e) {
            throw new RuntimeException("JSON parsing error while reading Minecraft block database mc-blocks.json from classpath", e);
        }

    }
}