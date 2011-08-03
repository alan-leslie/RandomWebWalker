/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package randomwebwalk.ui;

import randomwebwalk.ui.RandomWebWalkUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author al
 */
public class PlayPauseListener implements ActionListener {
    private final RandomWebWalkUI theUI;

    PlayPauseListener(RandomWebWalkUI theNewUI){
        theUI = theNewUI;
    }

    public void actionPerformed(ActionEvent e) {
        if(theUI.isPlaying()){
            theUI.pause();
        } else {
            theUI.play();
        }
    }
}
