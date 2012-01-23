/*
 */
package randomwebwalk.ui;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import randomwebwalk.RandomWebWalkController;

/*
 * User interface class for random web walker.
 * Simple interface that includes: an input field for the start URL
 * a status display text field
 * two buttons indicating whether the program is running pausec or stopped.
 */

public class RandomWebWalkUI extends JFrame {

    ImageIcon pauseIcon;
    ImageIcon playIcon;
    ImageIcon stopIcon;
    JLabel statusLabel;
    JTextField startPageTextField;
    JTextField idTextField;
    JPasswordField passwordTextField;
    JLabel startPageLabel;
    JLabel idLabel;
    JLabel passwordLabel;
    JButton playPauseButton;
    JButton stopButton;
    Thread taskThread = null;
    PlayPauseListener thePlayPauseListener = null;
    StopListener theStopListener = null;
    String idString = null;
    String passwordString = null;
    URL initialURL = null;
    RandomWebWalkController theController = null;

    // @param images - needs to be three images at least
    public RandomWebWalkUI(BufferedImage[] images) {
        setTitle("Web Walk");
		pauseIcon = new ImageIcon(images[0]);
        playIcon = new ImageIcon(images[1]);
        stopIcon = new ImageIcon(images[2]);
    }

    public void start() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (theController != null
                && theController.needsStartPage()) {
            getContentPane().add(getStartPagePanel(), "First");
        }
        getContentPane().add(getLabel());
        getContentPane().add(getButtonPanel(), "Last");
        setSize(200, 150);
        setLocation(1050, 550);
        setVisible(true);
    }

    public void play() {
        if (!isPlaying()) {
            initialURL = null;
            boolean isValidURL = false;

            if (theController != null) {
                if (theController.needsStartPage()) {
                    String initialURLStr = startPageTextField.getText();

                    try {
                        if (!initialURLStr.isEmpty()) {
                            if(initialURLStr.contains("http://")){
                                initialURL = new URL(initialURLStr);
                            } else {
                                String theFullURLStr = theController.getBaseURL() + initialURLStr;
                                initialURL = new URL(theFullURLStr);                                
                            }
                            isValidURL = true;
                        }
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(RandomWebWalkUI.class.getName()).log(Level.SEVERE, null, ex);
                        statusLabel.setText("Invalid URL");
                    }
                } else {
                    isValidURL = true;
                }

                if (isValidURL) {
                    System.out.println("play - setting icon to pause");
                    playPauseButton.setIcon(pauseIcon);
                    walk();
                }
            }
        }
    }

    public void pause() {
        if (isPlaying()) {
            playPauseButton.setIcon(playIcon);

            if (theController != null) {
                theController.pauseTask();
            }

            System.out.println("pause - setting icon to play");
        }
    }

    public void stop() {
        if (theController != null) {
            theController.stopTask();
        }
        System.out.println("stop - setting icon to play");
        playPauseButton.setIcon(playIcon);
    }

    public boolean isPlaying() {
        if (taskThread != null
                && taskThread.isAlive()) {
            return true;
        } else {
            return false;
        }
    }

    private void walk() {
        if (isPlaying()) {
            System.out.println("Thread is running - not starting new task");
        } else {
            WalkStatusDisplay theStatusDisplay = new JLabelWrapper(statusLabel);
            theController.setNotificationDisplay(theStatusDisplay);
            PlayPauseDisplay thePlayPauseDisplay = new JButtonWrapper(playPauseButton, playIcon);
            theController.setPlayPauseDisplay(thePlayPauseDisplay);
            theController.setInitialURL(initialURL);
            taskThread = new Thread(theController);

            taskThread.setPriority(Thread.NORM_PRIORITY);
            taskThread.start();
        }
    }

    private JLabel getLabel() {
        statusLabel = new JLabel();
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        return statusLabel;
    }

    private JPanel getButtonPanel() {
        playPauseButton = new JButton(playIcon);
        thePlayPauseListener = new PlayPauseListener(this);
        playPauseButton.addActionListener(thePlayPauseListener);

        stopButton = new JButton(stopIcon);
        theStopListener = new StopListener(this);
        stopButton.addActionListener(theStopListener);

        JPanel panel = new JPanel();
        panel.add(playPauseButton);
        panel.add(stopButton);
        return panel;
    }

    private JPanel getLoginPanel() {
        idTextField = new JTextField();
        idTextField.setColumns(20);
        idLabel = new JLabel("Id:", JLabel.TRAILING);
        idLabel.setLabelFor(idTextField);

        passwordTextField = new JPasswordField();
        passwordTextField.setColumns(20);
        passwordLabel = new JLabel("Password:", JLabel.TRAILING);
        idLabel.setLabelFor(passwordTextField);

        JPanel panelId = new JPanel();
        panelId.setLayout(new GridLayout(1, 1));
        panelId.add(idLabel);
        panelId.add(idTextField);

        JPanel panelPassword = new JPanel();
        panelPassword.setLayout(new GridLayout(1, 1));
        panelPassword.add(passwordLabel);
        panelPassword.add(passwordTextField);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(panelId);
        panel.add(panelPassword);

        return panel;
    }

    private JPanel getStartPagePanel() {
        startPageTextField = new JTextField();
        startPageTextField.setColumns(30);
        startPageLabel = new JLabel("Start page or user:", JLabel.TRAILING);
        startPageLabel.setLabelFor(startPageTextField);

        JPanel panelId = new JPanel();
        panelId.setLayout(new GridLayout(1, 1));
        panelId.add(startPageLabel);
        panelId.add(startPageTextField);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(panelId);

        return panel;
    }

    public void setController(RandomWebWalkController newController) {
        theController = newController;
    }
}
