package org.pepsoft.worldpainter.operations;

import org.intellij.lang.annotations.Language;

import javax.swing.*;
import java.awt.*;

import static java.awt.Font.BOLD;
import static java.awt.GridBagConstraints.*;

public class StandardOptionsPanel extends JPanel {
    /**
     * Create a new {@code StandardOptionsPanel}. By default, it shows an operation name and description. The
     * description is in HTML and has some default styles applied.
     *
     * @param name        The name of the operation to show on the panel.
     * @param description The description to show on the panel. This must be an HTML body (without surrounding
     * {@code html} or {@code body} tags.
     */
    public StandardOptionsPanel(String name, @Language("HTML") String description) {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.anchor = FIRST_LINE_START;
        constraints.gridwidth = REMAINDER;
        constraints.fill = HORIZONTAL;
        constraints.insets = new Insets(2, 2, 2, 2);
        JLabel label = new JLabel(name);
        label.setFont(label.getFont().deriveFont(BOLD, (int) (label.getFont().getSize() * 1.1)));
        add(label, constraints);
        if (description != null) {
            addLabel(description, constraints);
        }

        addAdditionalComponents(constraints);

        constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        add(Box.createGlue(), constraints);
    }

    /**
     * Add additional components to the panel. At this point the layout manager is set to a {@link GridBagLayout} and
     * the expectation is for there to be <em>one</em> column of components. After this a glue component will be added
     * with {@code GridBagConstraints.weighty} set to 1.0 in order to push the components up to the top of the panel.
     *
     * @param constraints A constraints object with {@code weightx} set to 1.0, {@code anchor} set to
     * {@code FIRST_LINE_START} and {@code gridwidth} set to {@code REMAINDER}.
     */
    protected void addAdditionalComponents(GridBagConstraints constraints) {
        // Do nothing
    }

    protected final void addLabel(String text, GridBagConstraints constraints) {
        add(new JLabel("<html><style>\n" +
                "    ul {\n" +
                "        margin-left: 15;\n" +
                "    }\n" +
                "</style>" + text + "</html>"), constraints);
    }
}