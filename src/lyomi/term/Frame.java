package lyomi.term;

import javax.swing.JFrame;

public class Frame extends JFrame {
	public static final long serialVersionUID = 0L;
	
	Panel panel;
	
	public Frame() {
		super("lyomiTerm");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel = new Panel();
		add(panel);
		
		pack();
		setVisible(true);
	}
}
