import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@FunctionalInterface
public interface KeyPressedListener extends KeyListener {
	@Override
	default void keyTyped(KeyEvent e) {}

	@Override
	default void keyReleased(KeyEvent e) {}

	@Override
	abstract void keyPressed(KeyEvent e);
	
}
