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
 * This class is adding print support for MEdit.
 * 
 * @author Krzysztof Szewczyk
 */
public class PrintActionManager implements Printable {

	public static void disableDoubleBuffering(final Component c) {
		final RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(false);
	}

	public static void enableDoubleBuffering(final Component c) {
		final RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(true);
	}

	public static void printComponent(final Component c) {
		new PrintActionManager(c).doPrint();
	}

	private final Component print_component;

	public PrintActionManager(final Component comp) {
		this.print_component = comp;
	}

	public void doPrint() {
		final PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);
		if (printJob.printDialog())
			try {
				printJob.print();
			} catch (final PrinterException pe) {
				final Crash dialog2 = new Crash(pe);
				dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog2.setVisible(true);
			}
	}

	@Override
	public int print(final Graphics g, final PageFormat pageFormat, final int pageIndex) {
		if (pageIndex > 0)
			return Printable.NO_SUCH_PAGE;
		else {
			final Graphics2D g2d = (Graphics2D) g;
			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
			PrintActionManager.disableDoubleBuffering(this.print_component);
			this.print_component.paint(g2d);
			PrintActionManager.enableDoubleBuffering(this.print_component);
			return Printable.PAGE_EXISTS;
		}
	}
}