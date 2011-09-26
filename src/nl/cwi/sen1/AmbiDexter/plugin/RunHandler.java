package nl.cwi.sen1.AmbiDexter.plugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class RunHandler extends AbstractHandler {

	public RunHandler() {
		
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		/*IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "Testplugin", "Yo yo");
		return null;*/
		
		AmbiDexterWizard wizard = new AmbiDexterWizard();
		WizardDialog dialog = new WizardDialog(HandlerUtil.getActiveShell(event), wizard);
		dialog.open();
		
		return null;
	}
}
