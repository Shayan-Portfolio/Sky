package engine.logging;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import engine.util.ExceptionUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

public class SkyRuntimeException extends RuntimeException {
    public SkyRuntimeException(String msg, Exception e) {
        super(msg, e);
        String stackTrace = ExceptionUtil.exceptionToString(this);
        Logger.meltdown(SkyRuntimeException.class, "\n" + stackTrace);
        showDialogBox(stackTrace);
    }
    public SkyRuntimeException(Exception e) {
        super("A fatal error occurred and the engine exited", e);
        String stackTrace = ExceptionUtil.exceptionToString(this);
        Logger.meltdown(SkyRuntimeException.class, "\n" + stackTrace);
        showDialogBox(stackTrace);
    }
    public SkyRuntimeException(String msg) {
        super(msg);
        String stackTrace = ExceptionUtil.exceptionToString(this);
        Logger.meltdown(SkyRuntimeException.class, "\n" + stackTrace);
        showDialogBox(stackTrace);
    }

    private static void showDialogBox(String stackTrace) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                FlatDarculaLaf.setup();

                JDialog dialog = new JDialog();
                dialog.setTitle("SkyEngine Error");
                dialog.setModal(true);
                dialog.setLayout(new BorderLayout(10, 10));
                JPanel contentPanel = new JPanel(new BorderLayout(10, 10));

                JTextArea stackTraceArea = new JTextArea(stackTrace);
                stackTraceArea.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(stackTraceArea);
                contentPanel.add(scrollPane, BorderLayout.CENTER);

                if (Logger.getOutputFile() != null) {
                    JLabel logPathLabel = new JLabel("Full log available at: " + Logger.getOutputFile().getAbsolutePath());
                    logPathLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
                    contentPanel.add(logPathLabel, BorderLayout.SOUTH);
                }

                dialog.add(contentPanel, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

                JButton copyButton = new JButton("Copy to Clipboard");
                copyButton.addActionListener(e -> {
                    StringSelection selection = new StringSelection(stackTrace);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    copyButton.setText("Copied!");
                });

                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(e -> System.exit(-1));
                dialog.getRootPane().setDefaultButton(closeButton);

                buttonPanel.add(copyButton);
                buttonPanel.add(closeButton);
                dialog.add(buttonPanel, BorderLayout.SOUTH);

                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(-1);
                    }
                });

                dialog.pack();
                dialog.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
