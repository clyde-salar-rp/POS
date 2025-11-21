package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ActionPanel extends JPanel {

    public ActionPanel(Runnable onVoidItem, Runnable onQuantityChange,
                       Runnable onExactDollar, Runnable onNextDollar,
                       Runnable onCash, Runnable onCredit) {
        setLayout(new GridLayout(3, 2, 10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Actions"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JButton voidItemBtn = createButton("Void Item", onVoidItem);
        JButton qtyBtn = createButton("Change Qty", onQuantityChange);
        JButton exactBtn = createButton("Exact $", onExactDollar);
        JButton nextBtn = createButton("Next $", onNextDollar);
        JButton cashBtn = createButton("Cash", onCash);
        JButton creditBtn = createButton("Credit", onCredit);

        add(voidItemBtn);
        add(qtyBtn);
        add(exactBtn);
        add(nextBtn);
        add(cashBtn);
        add(creditBtn);
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setFocusable(false);
        button.addActionListener(e -> action.run());
        return button;
    }
}