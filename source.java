import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Objects;

public class Main {
    private final JTextPane textPane; // Changed from JTextArea to JTextPane
    private final JFrame frame;
    private final JLabel statusBar;
    private final JLabel wordCountLabel;
    private final JTextArea lineNumberArea;
    private final UndoManager undoManager = new UndoManager();
    private boolean isWordWrapEnabled = true; // Enable word wrap by default
    private boolean isDarkMode = false; // Track current theme state

    public Main() {
        // Create the frame
        frame = new JFrame("Feature-Rich Text Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLayout(new BorderLayout());

        // Create the text pane
        textPane = new JTextPane();
        textPane.setContentType("text/plain");
        textPane.setEditable(true);
        textPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        textPane.setCaretPosition(0); // Set initial caret position

        // Set up the document listener to apply syntax highlighting and update status bar
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCounters();
                updateLineNumbers();
                applySyntaxHighlighting();
                updateStatusBar(); // Update status bar here
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCounters();
                updateLineNumbers();
                applySyntaxHighlighting();
                updateStatusBar(); // Update status bar here
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCounters();
                updateLineNumbers();
                applySyntaxHighlighting();
                updateStatusBar(); // Update status bar here
            }
        });

        // Add caret listener to update the status bar on caret movement
        textPane.addCaretListener(e -> updateStatusBar());

        // Create the line number area
        lineNumberArea = new JTextArea("1 ");
        lineNumberArea.setBackground(Color.LIGHT_GRAY);
        lineNumberArea.setEditable(false);

        // Wrap the text pane in a scroll pane with line numbers
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(lineNumberArea);

        // Status and word count labels
        statusBar = new JLabel("Line: 1, Column: 1");
        wordCountLabel = new JLabel("Words: 0 | Letters: 0");
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusBar, BorderLayout.WEST);
        bottomPanel.add(wordCountLabel, BorderLayout.EAST);

        // Create the menu bar with additional features
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createFormatMenu());
        menuBar.add(createToolsMenu());
        menuBar.add(createHelpMenu());

        // Create a toolbar with icons for quick actions
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(createToolButton("Open", "icons/open.png", e -> openFile()));
        toolBar.add(createToolButton("Save", "icons/save.png", e -> saveFile()));
        toolBar.add(createToolButton("Undo", "icons/undo.png", e -> {
            if (undoManager.canUndo()) undoManager.undo();
        }));
        toolBar.add(createToolButton("Redo", "icons/redo.png", e -> {
            if (undoManager.canRedo()) undoManager.redo();
        }));

        // Font Style Dropdown
        String[] styles = {"Normal", "Bold", "Italic", "Underline"};
        JComboBox<String> fontStyleComboBox = new JComboBox<>(styles);
        fontStyleComboBox.addActionListener(e -> changeFontStyle((String) Objects.requireNonNull(fontStyleComboBox.getSelectedItem())));
        toolBar.add(fontStyleComboBox);

        // Font Size Dropdown
        String[] fontSizes = {"12", "14", "16", "18", "20", "22", "24", "26", "28", "30"};
        JComboBox<String> fontSizeComboBox = new JComboBox<>(fontSizes);
        fontSizeComboBox.setSelectedItem("12");
        fontSizeComboBox.addActionListener(e -> changeFontSize((String) fontSizeComboBox.getSelectedItem()));
        toolBar.add(fontSizeComboBox);

        // Theme Toggle Button
        JButton themeToggleButton = new JButton("Toggle Theme");
        themeToggleButton.addActionListener(e -> toggleTheme());
        toolBar.add(themeToggleButton);

        frame.setJMenuBar(menuBar);
        frame.add(toolBar, BorderLayout.NORTH); // Add the toolbar to the frame
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = createMenuItem("Open", "icons/open.png", e -> openFile());
        JMenuItem saveItem = createMenuItem("Save", "icons/save.png", e -> saveFile());
        JMenuItem exitItem = createMenuItem("Exit", "icons/exit.png", e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        return fileMenu;
    }

    private JMenu createEditMenu() {
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = createMenuItem("Undo", "icons/undo.png", e -> {
            if (undoManager.canUndo()) undoManager.undo();
        });
        JMenuItem redoItem = createMenuItem("Redo", "icons/redo.png", e -> {
            if (undoManager.canRedo()) undoManager.redo();
        });
        JMenuItem findReplaceItem = createMenuItem("Find & Replace", "icons/find.png", e -> showFindReplaceDialog());

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(findReplaceItem);
        return editMenu;
    }

    private JMenu createFormatMenu() {
        JMenu formatMenu = new JMenu("Format");
        JCheckBoxMenuItem wordWrapItem = new JCheckBoxMenuItem("Word Wrap", isWordWrapEnabled);
        wordWrapItem.addActionListener(e -> toggleWordWrap());

        formatMenu.add(wordWrapItem);
        return formatMenu;
    }

    private JMenu createToolsMenu() {
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem changeFontItem = createMenuItem("Change Font", "icons/font.png", e -> changeFont());
        toolsMenu.add(changeFontItem);
        return toolsMenu;
    }

    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = createMenuItem("About", "icons/about.png", e -> showInfoDialog());
        helpMenu.add(aboutItem);
        return helpMenu;
    }

    private JMenuItem createMenuItem(String text, String iconPath, ActionListener action) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setIcon(new ImageIcon(iconPath));
        menuItem.addActionListener(action);
        return menuItem;
    }

    private JButton createToolButton(String text, String iconPath, ActionListener action) {
        JButton button = new JButton(text);
        button.setIcon(new ImageIcon(iconPath));
        button.addActionListener(action);
        return button;
    }

    private void changeFontStyle(String style) {
        int fontStyle = Font.PLAIN;
        switch (style) {
            case "Bold":
                fontStyle = Font.BOLD;
                break;
            case "Italic":
                fontStyle = Font.ITALIC;
                break;
            case "Underline":
                // Swing does not support underline directly; you can simulate this by adding an underline style.
                break;
            default:
                break;
        }
        Font currentFont = textPane.getFont();
        textPane.setFont(currentFont.deriveFont(fontStyle));
    }

    private void changeFontSize(String size) {
        int fontSize = Integer.parseInt(size);
        Font currentFont = textPane.getFont();
        textPane.setFont(currentFont.deriveFont((float) fontSize));
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            textPane.setBackground(Color.DARK_GRAY);
            textPane.setForeground(Color.WHITE);
        } else {
            textPane.setBackground(Color.WHITE);
            textPane.setForeground(Color.BLACK);
        }
    }

    private void toggleWordWrap() {
        isWordWrapEnabled = !isWordWrapEnabled;
        // For JTextPane, manage wrap behavior by overriding the document and using preferred size
        textPane.setPreferredSize(new Dimension(800, 500)); // Manage width
        textPane.revalidate();
    }

    private void updateLineNumbers() {
        int totalLines = textPane.getDocument().getDefaultRootElement().getElementCount();
        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= totalLines; i++) {
            lineNumbers.append(i).append("\n");
        }
        lineNumberArea.setText(lineNumbers.toString());
    }

    private void updateCounters() {
        String text = textPane.getText();
        String[] words = text.split("\\s+");
        int wordCount = text.isEmpty() ? 0 : words.length;
        int letterCount = text.replace(" ", "").length();

        wordCountLabel.setText("Words: " + wordCount + " | Letters: " + letterCount);
    }

    private void updateStatusBar() {
        int caretPosition = textPane.getCaretPosition();
        int line = textPane.getDocument().getDefaultRootElement().getElementIndex(caretPosition) + 1;
        int column = caretPosition - textPane.getDocument().getDefaultRootElement().getElement(line - 1).getStartOffset() + 1;

        statusBar.setText("Line: " + line + ", Column: " + column);
    }

    private void applySyntaxHighlighting() {
        // Implement syntax highlighting logic if needed
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "text"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                textPane.setText(""); // Clear current text
                String line;
                while ((line = reader.readLine()) != null) {

                }
                updateCounters();
                updateLineNumbers();
                updateStatusBar(); // Update status bar after loading file
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error opening file: " + e.getMessage());
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "text"));
        int returnValue = fileChooser.showSaveDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                writer.write(textPane.getText());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error saving file: " + e.getMessage());
            }
        }
    }

    private void showInfoDialog() {
        JOptionPane.showMessageDialog(frame, "Feature-Rich Text Editor\nVersion 1.0\nDeveloped by [Your Name]");
    }

    private void showFindReplaceDialog() {
        JDialog findReplaceDialog = new JDialog(frame, "Find and Replace", true);
        findReplaceDialog.setLayout(new GridLayout(3, 2));

        JLabel findLabel = new JLabel("Find:");
        JTextField findField = new JTextField();
        JLabel replaceLabel = new JLabel("Replace:");
        JTextField replaceField = new JTextField();

        JButton findButton = new JButton("Find");
        JButton replaceButton = new JButton("Replace");
        JButton cancelButton = new JButton("Cancel");

        findReplaceDialog.add(findLabel);
        findReplaceDialog.add(findField);
        findReplaceDialog.add(replaceLabel);
        findReplaceDialog.add(replaceField);
        findReplaceDialog.add(findButton);
        findReplaceDialog.add(replaceButton);
        findReplaceDialog.add(cancelButton);

        findButton.addActionListener(e -> {
            String textToFind = findField.getText();
            String content = textPane.getText();
            int index = content.indexOf(textToFind);

            if (index >= 0) {
                textPane.setCaretPosition(index);
                textPane.select(index, index + textToFind.length());
                JOptionPane.showMessageDialog(findReplaceDialog, "Found at position: " + index);
            } else {
                JOptionPane.showMessageDialog(findReplaceDialog, "Text not found!");
            }
        });

        replaceButton.addActionListener(e -> {
            String textToFind = findField.getText();
            String textToReplace = replaceField.getText();
            String content = textPane.getText();
            if (content.contains(textToFind)) {
                content = content.replace(textToFind, textToReplace);
                textPane.setText(content);
                JOptionPane.showMessageDialog(findReplaceDialog, "Replaced all occurrences of: " + textToFind);
            } else {
                JOptionPane.showMessageDialog(findReplaceDialog, "Text not found to replace!");
            }
        });

        cancelButton.addActionListener(e -> findReplaceDialog.dispose());

        findReplaceDialog.setSize(300, 150);
        findReplaceDialog.setLocationRelativeTo(frame);
        findReplaceDialog.setVisible(true);
    }

    private void changeFont() {
        // Create a dialog for font selection
        JDialog fontDialog = new JDialog(frame, "Choose Font", true);
        fontDialog.setLayout(new GridLayout(4, 2));

        // Font family options
        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JComboBox<String> fontFamilyComboBox = new JComboBox<>(fontFamilies);
        fontFamilyComboBox.setSelectedItem(textPane.getFont().getFamily());

        // Font style options
        String[] fontStyles = {"Plain", "Bold", "Italic", "Bold Italic"};
        JComboBox<String> fontStyleComboBox = new JComboBox<>(fontStyles);
        fontStyleComboBox.setSelectedItem("Plain");

        // Font size options
        Integer[] fontSizes = {12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32};
        JComboBox<Integer> fontSizeComboBox = new JComboBox<>(fontSizes);
        fontSizeComboBox.setSelectedItem(textPane.getFont().getSize());

        // Add components to dialog
        fontDialog.add(new JLabel("Font Family:"));
        fontDialog.add(fontFamilyComboBox);
        fontDialog.add(new JLabel("Font Style:"));
        fontDialog.add(fontStyleComboBox);
        fontDialog.add(new JLabel("Font Size:"));
        fontDialog.add(fontSizeComboBox);

        // Button to apply changes
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            String selectedFontFamily = (String) fontFamilyComboBox.getSelectedItem();
            int selectedFontStyle = fontStyleComboBox.getSelectedIndex(); // 0=Plain, 1=Bold, 2=Italic, 3=Bold Italic
            int selectedFontSize = (int) fontSizeComboBox.getSelectedItem();

            // Create the new font and set it to the text pane
            Font newFont = new Font(selectedFontFamily, selectedFontStyle, selectedFontSize);
            textPane.setFont(newFont);
            fontDialog.dispose(); // Close the dialog after applying
        });

        // Cancel button to close the dialog
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> fontDialog.dispose());

        // Add buttons to the dialog
        fontDialog.add(applyButton);
        fontDialog.add(cancelButton);

        // Set dialog properties
        fontDialog.setSize(300, 200);
        fontDialog.setLocationRelativeTo(frame);
        fontDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
