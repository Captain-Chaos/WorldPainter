package org.pepsoft.worldpainter.panels;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class SearchableJComboBox extends JComboBox<String> {
    public SearchableJComboBox(String[] entries) {
        super(entries);
        setup();
    }

    public SearchableJComboBox() {
        super();
        setup();
    }

    private void setup() {
        setEditable(true);
        setEditor(new SearchEditor (this));
    }

    public String[] getEntries() {
        int size = this.getItemCount();
        String[] entries = new String[size];

        for (int index = 0; index < size; index++) {
            String data = getItemAt(index);
            entries[index] = data;
        }

        return entries;
    }

    private static class SearchEditor extends BasicComboBoxEditor {
        public SearchEditor (final SearchableJComboBox searchableJComboBox) {
            KeyAdapter listener = new KeyAdapter() {
                public void keyReleased(KeyEvent event) {
                    String[] entries = searchableJComboBox.getEntries();

                    if ((event.getKeyChar() >= 'a' && event.getKeyChar() <= 'z') ||
                            (event.getKeyChar() >= 'A' && event.getKeyChar() <= 'Z') ||
                            (event.getKeyChar() == KeyEvent.VK_UNDERSCORE)
                    ) {
                        String searchText = editor.getText();

                        Predicate<String> matchesStart = entry -> entry.startsWith(searchText);
                        Optional<String> firstMatch = Arrays.stream(entries).filter(matchesStart).findFirst();

                        if (!firstMatch.isPresent()) return;
                        String finalText = firstMatch.get();

                        if (finalText.equals("")) finalText = searchText;

                        if (!finalText.equals(searchText)) {
                            editor.setText(finalText);
                            editor.setSelectionStart(searchText.length());
                            editor.setSelectionEnd(finalText.length());
                        }

                        searchableJComboBox.setSelectedItem(finalText);
                    }
                }
            };

            editor.addKeyListener(listener);

            ActionListener actionListener = e -> {
                if (searchableJComboBox.getSelectedItem() != null && ! editor.getText().equals(searchableJComboBox.getSelectedItem().toString())) {
                    editor.setText(searchableJComboBox.getSelectedItem().toString());
                }
            };
            searchableJComboBox.addActionListener(actionListener);
        }
    }
}


