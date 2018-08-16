package actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class RecordsBrowserAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        BrowserUtil.browse("https://system.netsuite.com/help/helpcenter/en_US/srbrowser/Browser2018_1/script/record/account.html");
    }
}
