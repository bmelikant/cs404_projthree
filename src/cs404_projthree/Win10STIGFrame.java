package cs404_projthree;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/*
 * @author Ben Melikant, Zainab Alalshaikh, Alex Way, Gavin Spellmeyer
 */
public class Win10STIGFrame extends javax.swing.JFrame implements Runnable {

    // 
    public static final int PASS = 0;
    public static final int FAIL = 1;
    public static final int INVALID = 2;
    
    // thread item (destroy on close!)
    Thread t;
    
    /*
     * Creates new form Win10STIGFrame
     */
    public Win10STIGFrame() {
        initComponents();
    }
    
    // void checkStigRequirements (): Check to see whether the system meets all STIG guidelines
    // inputs: None
    // returns: none
    private void checkStigRequirements () {

        // set the text for the status bar
        this.jmi_RunReportItem.setEnabled(false);
        jl_StatusBarLabel.setText ("Running STIG checks..");
        
        // build the table data model
        DefaultTableModel tm = new DefaultTableModel (null, new Object [] { "Configuration", "Result" });
        
        // check for NTFS format of C:
        int result = checkForNTFS ();
        tm.addRow (new Object [] { "C:\\ formatted as NTFS", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        result = checkServiceState ("SSDPSRV", "stopped");
        tm.addRow (new Object [] { "SSDP service is stopped", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
    
        result = checkServiceState ("SSDPSRV", "disabled");
        tm.addRow (new Object [] { "SSDP service is disabled", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        result = checkServiceState ("UPNPHOST", "stopped");
        tm.addRow (new Object [] { "uPnP service is stopped", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        result = checkServiceState ("UPNPHOST", "disabled");
        tm.addRow (new Object [] { "uPnp service is disabled", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        result = checkForWindowsVer ();
        tm.addRow (new Object [] { "Windows Version is Win10 Enterprise", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        result = checkForIISInstall ();
        tm.addRow (new Object [] { "IIS is NOT installed", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        result = checkForInactive ();
        tm.addRow (new Object [] { "No inactive user accounts", ((result == PASS) ? "PASS" : ((result == FAIL) ? "FAIL" : "INVALID CMD"))});
        jl_StatusBarLabel.setText (jl_StatusBarLabel.getText() + ".");
        
        this.jt_ReportOutTable.setModel (tm);
        this.jmi_RunReportItem.setEnabled(true);
        jl_StatusBarLabel.setText ("Done running STIG checks!");
    }
    
    //Checks for the windows OS name
    //Must run Windows Enterprise
    private int checkForWindowsVer () {
        try {
            
            byte [] result = executeCommand ("systeminfo");
            
            if (result == null)
                return INVALID;
           
            String cmdOutput = new String (result);
            
            if (cmdOutput.contains ("Windows 10 Enterprise"))
                return PASS;
            else
                return FAIL;
            
        } catch (IOException w) {
            
            return INVALID;
        }
    }

       //Not completed, must find working information on working execution
    private int checkForIISInstall(){
        
        try {
            
            byte [] result = executeCommand ("net start w3svc");
            
            if (result == null)                
                return PASS;
            
            String cmdOutput = new String (result);
            int idx = cmdOutput.indexOf("");
            
            if (cmdOutput.contains ("service name is invalid"))
                return PASS;
            else
                return FAIL;
            
        } catch (IOException e) {
            
            System.err.println ("Service check error: " + e.toString ());
            return INVALID;
        }
    }
    
    //use powershell
    //WN10-00-000065 on the STIG Document
    private int checkForInactive(){
        
        try {
        
            int status = PASS;
            
            Process p = Runtime.getRuntime().exec ("powershell C:\\Users\\Ben\\Documents\\NetBeansProjects\\cs404_projthree\\users.ps1");
            BufferedReader rdr = new BufferedReader (new InputStreamReader (p.getInputStream()));
            
            String line;
            while ((line = rdr.readLine ()) != null) {
            
                // if the line says "True" the account is active
                if (line.contains ("True")) {
                    
                    
                }
                
                System.out.println (line);
            }
            
            rdr.close ();
            p.getOutputStream().close ();
            
            return status;
            
        } catch (IOException e) {
            
            System.out.println ("Error executing command: " + e.toString ());
            return INVALID;
        }
    }
    
    // boolean checkForNTFS ()
    // inputs: none, returns: PASS OR FAIL
    private int checkForNTFS () {
        
        try {
            
            byte [] result = executeCommand ("fsutil fsinfo volumeInfo C:");
            
            if (result == null)                
                return INVALID;
            
            String cmdOutput = new String (result);
            int idx = cmdOutput.indexOf("File System Name : ");
            
            if (idx >= 0) {
                
                String dataLine = cmdOutput.substring(idx+"File System Name : ".length(),
                        cmdOutput.indexOf("\n", idx+"File System Name : ".length())).trim();
                
                if (dataLine.equals ("NTFS"))
                    return PASS;
                else
                    return FAIL;
            }
            
            else
                return INVALID;
            
        } catch (IOException e) {
            
            return INVALID;
        }
    }
    
    // int checkServiceState (): See if the service state matches the requested state
    // inputs: svc - name of service, state - state required
    // returns: PASS, FAIL, INVALID
    private int checkServiceState (String svc, String state) {
        
        try {
            
            byte [] cmdResult = executeCommand ("sc query \"" + svc + "\"");
            
            if (cmdResult == null)
                return INVALID;
            
            String cmdData = new String (cmdResult);
            String [] cmdLines = cmdData.split("\r\n");
            
            // print each line of the service data (testing purposes)
            for (String s : cmdLines) {
                
                if (s.contains ("STATE") && s.contains (state.toUpperCase()))
                    return PASS;   
            }
            
            return FAIL;
            
        } catch (IOException e) {
            
            return INVALID;
        }
    }
    
    // byte [] executeCommand (): Get the stdout data from a command
    // inputs: cmd - command to run
    // returns: stdout from the program, null if no output
    private byte [] executeCommand (String cmd) throws IOException {
        
        try {
        
            // fsutil fsinfo volumeInfo C:
            Process p = Runtime.getRuntime().exec (cmd);
            p.waitFor ();
            
            byte [] pstdin = new byte [p.getInputStream().available()];
            byte [] pstderr = new byte [p.getErrorStream().available()];
            
            p.getInputStream().read(pstdin);
            p.getErrorStream().read(pstderr);
            
            if (p.exitValue () > 0)
                return pstderr;
            else
                return pstdin;
        
        } catch (InterruptedException e) {
         
            JOptionPane.showMessageDialog (null, "Interrupted while waiting for process: " + e.toString(),
                    "Error:", JOptionPane.ERROR_MESSAGE);
        }
        
        return null;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jt_ReportOutTable = new javax.swing.JTable();
        jp_StatusBarPanel = new javax.swing.JPanel();
        jl_StatusBarLabel = new javax.swing.JLabel();
        jmb_MainMenuBar = new javax.swing.JMenuBar();
        jm_FileMenu = new javax.swing.JMenu();
        jmi_ExitItem = new javax.swing.JMenuItem();
        jm_ReportMenu = new javax.swing.JMenu();
        jmi_RunReportItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jt_ReportOutTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Configuration", "Result"
            }
        ));
        jt_ReportOutTable.setRequestFocusEnabled(false);
        jScrollPane2.setViewportView(jt_ReportOutTable);

        javax.swing.GroupLayout jp_StatusBarPanelLayout = new javax.swing.GroupLayout(jp_StatusBarPanel);
        jp_StatusBarPanel.setLayout(jp_StatusBarPanelLayout);
        jp_StatusBarPanelLayout.setHorizontalGroup(
            jp_StatusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jp_StatusBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jl_StatusBarLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_StatusBarPanelLayout.setVerticalGroup(
            jp_StatusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jl_StatusBarLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)
        );

        jm_FileMenu.setText("File");

        jmi_ExitItem.setLabel("Exit");
        jmi_ExitItem.setName(""); // NOI18N
        jmi_ExitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmi_ExitItemActionPerformed(evt);
            }
        });
        jm_FileMenu.add(jmi_ExitItem);

        jmb_MainMenuBar.add(jm_FileMenu);

        jm_ReportMenu.setText("Report");

        jmi_RunReportItem.setText("Run Report");
        jmi_RunReportItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmi_RunReportItemActionPerformed(evt);
            }
        });
        jm_ReportMenu.add(jmi_RunReportItem);

        jmb_MainMenuBar.add(jm_ReportMenu);

        setJMenuBar(jmb_MainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                    .addComponent(jp_StatusBarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jp_StatusBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jmi_ExitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmi_ExitItemActionPerformed
        
        // check if the thread is running
        if (t.isAlive()) {
            
            try {
            
                t.join();
            
            } catch (InterruptedException e) {
            
                System.err.println ("Thread interrupted: " + e.toString());
            }
        }
        System.exit (0);
    }//GEN-LAST:event_jmi_ExitItemActionPerformed

    private void jmi_RunReportItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmi_RunReportItemActionPerformed
        
        // run the checks in a thread!
        t = new Thread(this);
        t.start();
    }//GEN-LAST:event_jmi_RunReportItemActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        if (t.isAlive()) {
            
            try {
            
                t.join();
            
            } catch (InterruptedException e) {
            
                System.err.println ("Thread interrupted: " + e.toString());
            }
        }
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Win10STIGFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel jl_StatusBarLabel;
    private javax.swing.JMenu jm_FileMenu;
    private javax.swing.JMenu jm_ReportMenu;
    private javax.swing.JMenuBar jmb_MainMenuBar;
    private javax.swing.JMenuItem jmi_ExitItem;
    private javax.swing.JMenuItem jmi_RunReportItem;
    private javax.swing.JPanel jp_StatusBarPanel;
    private javax.swing.JTable jt_ReportOutTable;
    // End of variables declaration//GEN-END:variables

    @Override
    public void run() {
        
        checkStigRequirements ();
    }
}