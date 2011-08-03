/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package randomwebwalk.ui;

import randomwebwalk.ui.RandomWebWalkUI;
import java.awt.event.*;

/**
 *
 * @author al
 */
public class StopListener implements ActionListener {
    private final RandomWebWalkUI theUI;

    StopListener(RandomWebWalkUI theNewUI) {
        theUI = theNewUI;
    }

    public void actionPerformed(ActionEvent e) {
        theUI.stop();
    }
}
