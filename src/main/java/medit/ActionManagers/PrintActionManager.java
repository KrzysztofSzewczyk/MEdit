package medit.ActionManagers;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.swing.RepaintManager;
import javax.swing.WindowConstants;

import medit.Crash;

/**
 *
 * @author User
 */
public class PrintActionManager implements Printable {

	private Component print_component;

	public static void printComponent(Component c) {
		new PrintActionManager(c).doPrint();
	}

	public PrintActionManager(Component comp) {
		this.print_component = comp;
	}

	public void doPrint() {
		PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);
		if (printJob.printDialog()) {
			try {
				printJob.print();
			} catch (PrinterException pe) {
				final Crash dialog2 = new Crash(pe);
				dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog2.setVisible(true);
			}
		}
	}

	@Override
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		if (pageIndex > 0) {
			return (NO_SUCH_PAGE);
		} else {
			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
			disableDoubleBuffering(print_component);
			print_component.paint(g2d);
			enableDoubleBuffering(print_component);
			return (PAGE_EXISTS);
		}
	}

	public static void disableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(false);
	}

	public static void enableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(true);
	}
}