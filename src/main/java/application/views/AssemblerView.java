package application.views;


//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import application.DisplayStyles;
import application.Assembler;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rtextarea.RTextScrollPane;
import ru.ifmo.cs.bcomp.CPU;
import ru.ifmo.cs.bcomp.ui.GUI;
import ru.ifmo.cs.bcomp.ui.components.ActivateblePanel;
import ru.ifmo.cs.bcomp.ui.components.ComponentManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arthur Kupriyanov
 */
public class AssemblerView extends ActivateblePanel {
    private final GUI gui;
    private final CPU cpu;
    private final ComponentManager cmanager;
    private final Assembler asm;
    private final RSyntaxTextArea text;

    private SyntaxScheme scheme;


    private Image img;

    {
        try {
            String backgroundPath = "/klimenkov.png";

            InputStream in = getClass().getResourceAsStream(backgroundPath);
            if (in != null) img = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AssemblerView(GUI gui) {
        this.gui = gui;
        this.cpu = gui.getCPU();
        this.cmanager = gui.getComponentManager();
        this.text = new RSyntaxTextArea();
        this.asm = new Assembler(this.cpu.getInstructionSet(), this.text);
        setTextArea();

        JButton button = new JButton("Компилировать");

        button.setBounds(640, 40, 200, 30);
        button.setFocusable(false);
        button.addActionListener(e -> {
            if (this.cpu.isRunning()) {
                this.showError("Для компиляции остановите выполняющуюся программу");
            } else {
                this.cmanager.saveDelay();
                boolean clock = AssemblerView.this.cpu.getClockState();
                this.cpu.setClockState(true);

                try {
                    if (!checkORG()) return;
                    asm.compileProgram(text.getText());
                    asm.loadProgram(cpu);
                } catch (Exception var4) {
                    AssemblerView.this.showError(var4.getMessage());
                }

                this.cpu.setClockState(clock);
                this.cmanager.clearActiveSignals();
                this.cmanager.restoreDelay();
            }
        });

        this.add(button);

    }

    public void panelActivate() {
        this.text.requestFocus();
    }

    public void panelDeactivate() {
    }

    public String getPanelName() {
        return "Ассемблер";
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this.gui, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
//        this.setBackground(Color.BLUE);
//        g.setColor(Color.DARK_GRAY);
//        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        if (img != null) g.drawImage(img, 0, 0, this);
        super.paintComponent(g);
    }
    private boolean checkORG(){
        return checkORG(true);
    }
    private boolean checkORG(boolean showErrorMessage) {

        text.removeAllLineHighlights();

        String program = text.getText();
        String[] prog = program.replace("\r", "").toUpperCase().split("\n");
        int currentAddr = 0;
        int lineno = 0;

        List<Integer> reservedAddrs = new ArrayList<>();

        for (String l : prog) {

            ++lineno;
            String[] line = l.trim().split("[#;]+");
            if (line.length != 0 && !line[0].equals("")) {
                line = line[0].trim().split("[ \t]+");
                if (line.length != 0 && !line[0].equals("")) {
                    if (line[0].equals("ORG")) {
                        if (line.length != 2) {
                            setErrorLine(lineno);
                            if (showErrorMessage) showErrorMessage("Неверная длина команды ORG != 2");
                            return false;
                        }
                        try {
                            int addr = Integer.parseInt(line[1], 16);
                            if (addr < 0 || addr > 2047) throw new NumberFormatException();

                            currentAddr = addr;
                            if (reservedAddrs.contains(currentAddr)){
                                if (showErrorMessage) showErrorMessage("Программа перекрывает себя");
                                setErrorLine(lineno);
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            setErrorLine(lineno);
                            if (showErrorMessage) showErrorMessage("Неверный формат адреса");
                            return false;
                        }
                    }else {
                        if (reservedAddrs.contains(currentAddr)){
                            if (showErrorMessage) showErrorMessage("Программа перекрывает себя");
                            setErrorLine(lineno);
                            return false;
                        }
                        reservedAddrs.add(currentAddr);
                        currentAddr++;
                    }
                }
            }

        }
        return true;

    }

    private void setErrorLine(int line){
        try {
            System.out.println("1211");
            text.addLineHighlight(line - 1, DisplayStyles.ERROR_COLOR);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void showErrorMessage(String msg){
        JOptionPane.showMessageDialog(this, msg, "Ошибка компиляции", JOptionPane.ERROR_MESSAGE);
    }

    private void setTextArea(){
        this.text.setFont(DisplayStyles.FONT_COURIER_BOLD_21);
        this.text.setBackground(Color.white);
        this.text.setForeground(Color.black);

//        ((RSyntaxTextArea) this.text).setSyntaxScheme(new SyntaxScheme(new Font("Blogger Sans", Font.PLAIN, 14)));

        this.text.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
        this.text.setCurrentLineHighlightColor(new Color(50, 50, 50, 50));
        this.text.setHighlightCurrentLine(true);

        this.text.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    text.removeAllLineHighlights();
                    try {
                        text.addLineHighlight(text.getCaretLineNumber(), Color.WHITE);
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
                    checkORG(false);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        RTextScrollPane scroll = new RTextScrollPane(this.text);
        scroll.setBounds(10, 10, 600, 540);
        scroll.setForeground(Color.WHITE);

        this.add(scroll);
    }

}

