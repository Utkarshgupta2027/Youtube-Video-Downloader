import java.awt.*;
public class Awtex {
public static void main(String[] args) {
Frame f = new Frame("AWT Example");
Button b = new Button("Click Me");
b.setBounds(500, 100, 80, 300);
f.add(b);
f.setSize(300, 300);
f.setLayout(null);
f.setVisible(true);
}
}