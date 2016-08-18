package game;

import javax.swing.SwingUtilities;

public class TestFrame {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				CUT cut = new CUT(Player.WHITE);
			}
		});
	}

}//TestFrame