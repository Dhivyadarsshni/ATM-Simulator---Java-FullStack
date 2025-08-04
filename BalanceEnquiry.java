package com.ATMSimulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;

public class BalanceEnquiry extends JFrame implements ActionListener {
    String pinnumber;
    JButton back;

    BalanceEnquiry(String pinnumber){
        this.pinnumber = pinnumber;
        setLayout(null);

        // ATM Machine Picture
        ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icons/atm.jpg"));
        Image i2 = i1.getImage().getScaledInstance(900, 900, Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel image = new JLabel(i3);
        image.setBounds(0, 0, 900, 900);
        add(image);

        // Back Button
        back = new JButton("Back");
        back.setBounds(355, 520, 150, 30);
        back.addActionListener(this);
        image.add(back);

        // Initialize balance
        double balance = 0;

        // Database connection
        Conn c = new Conn();
        try {
            ResultSet rs = c.s.executeQuery("SELECT * FROM bank WHERE Pin_Number = '" + pinnumber + "'");

            // Iterate through the records and calculate balance
            while (rs.next()){
                if(rs.getString("Type").equals("Deposit")) {
                    balance += Double.parseDouble(rs.getString("Amount"));
                } else {
                    balance -= Double.parseDouble(rs.getString("Amount"));
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        // Transaction Type Text
        JLabel text = new JLabel("Your Current Account Balance is Rs " + balance);
        text.setBounds(170, 300, 400, 30);
        text.setForeground(Color.white);
        text.setFont(new Font("System", Font.BOLD, 16));
        image.add(text);

        // Layouts
        setSize(900, 900);
        setLocation(300, 0);
        setUndecorated(true);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent ae) {
        setVisible(false);
        new Transactions(pinnumber).setVisible(true);
    }

    public static void main(String[] args) {
        new BalanceEnquiry("");
    }
}
