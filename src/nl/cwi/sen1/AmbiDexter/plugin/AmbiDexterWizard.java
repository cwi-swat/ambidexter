package nl.cwi.sen1.AmbiDexter.plugin;

import org.eclipse.jface.wizard.Wizard;

public class AmbiDexterWizard extends Wizard {

	private WizardPage1 page1;

	public AmbiDexterWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page1 = new WizardPage1();
		addPage(page1);
	}

	@Override
	public boolean performFinish() {

		// just put the result to the console, imagine here much more
		// intelligent stuff.
		System.out.println(page1.getText1());

		return true;
	}
}
