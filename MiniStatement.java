package com.ATMSimulator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

public class MiniStatement extends JFrame {
    private String pinnumber;
    private DefaultCategoryDataset dataset;
    private JComboBox<String> filterDropdown;
    private ChartPanel chartPanel;
    private java.util.Map<String, Double> categoryBudgets = new java.util.HashMap<>();
    private JDateChooser startChooser, endChooser;

    public MiniStatement(String pinnumber) {
        this.pinnumber = pinnumber;
        setTitle("Mini Statement - Spending Chart");
        setLayout(null);

        JLabel bank = new JLabel("SRM Bank", SwingConstants.CENTER);
        bank.setFont(new Font("Arial", Font.BOLD, 18));
        bank.setBounds(150, 20, 200, 25);
        add(bank);

        JLabel card = new JLabel();
        card.setBounds(20, 60, 300, 20);
        add(card);

        String[] columns = {"Date", "Type", "Amount"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(20, 90, 450, 150);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        JLabel balance = new JLabel();
        balance.setBounds(20, 270, 400, 25);
        add(balance);

        filterDropdown = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly", "Custom"});
        filterDropdown.setBounds(350, 60, 120, 25);
        add(filterDropdown);

        filterDropdown.addActionListener(e -> updateChart());

        // Custom date pickers
        startChooser = new JDateChooser();
        startChooser.setBounds(150, 250, 120, 25);
        startChooser.setVisible(false);
        add(startChooser);

        endChooser = new JDateChooser();
        endChooser.setBounds(280, 250, 120, 25);
        endChooser.setVisible(false);
        add(endChooser);

        filterDropdown.addItemListener(e -> {
            boolean custom = "Custom".equals(filterDropdown.getSelectedItem());
            startChooser.setVisible(custom);
            endChooser.setVisible(custom);
        });

        JButton setBudgetsBtn = new JButton("Set Budgets");
        setBudgetsBtn.setBounds(20, 660, 130, 30);
        add(setBudgetsBtn);

        setBudgetsBtn.addActionListener(e -> showBudgetDialog());

        try {
            Conn conn = new Conn();
            ResultSet rs = conn.s.executeQuery("SELECT * FROM login WHERE Pin_Number ='" + pinnumber + "'");
            if (rs.next()) {
                String cardNum = rs.getString("Card_Number");
                card.setText("Card Number: " + cardNum.substring(0, 4) + " XXXX XXXX " + cardNum.substring(12));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        double bal = 0;
        try {
            Conn conn = new Conn();
            ResultSet rs = conn.s.executeQuery("SELECT * FROM bank WHERE Pin_Number ='" + pinnumber + "' ORDER BY DateFormatted DESC");

            while (rs.next()) {
                String date = rs.getString("DateFormatted");
                String type = rs.getString("Type");
                String amount = rs.getString("Amount");

                model.addRow(new String[]{date, type, amount});

                double transactionAmount = Double.parseDouble(amount);

                if (type.equalsIgnoreCase("Deposit")) {
                    bal += transactionAmount;
                } else {
                    bal -= transactionAmount;
                }
            }
            balance.setText("Your Current Account Balance: Rs " + bal);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Default budgets
        categoryBudgets.put("Food", 7000.0);
        categoryBudgets.put("Bills", 5000.0);
        categoryBudgets.put("Shopping", 6000.0);
        categoryBudgets.put("Travel", 8000.0);
        categoryBudgets.put("Other", 4000.0);

        dataset = new DefaultCategoryDataset();
        updateChart();

        JFreeChart barChart = ChartFactory.createBarChart(
                "Spending Overview", "Category", "Amount (Rs)", dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL, true, true, false);

        CategoryPlot plot = (CategoryPlot) barChart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesItemLabelGenerator(0, new StandardCategoryItemLabelGenerator());
        renderer.setSeriesItemLabelsVisible(0, true);

        chartPanel = new ChartPanel(barChart);
        chartPanel.setBounds(20, 300, 450, 350);
        add(chartPanel);

        setSize(500, 750);
        setLocation(100, 50);
        getContentPane().setBackground(Color.WHITE);
        setVisible(true);
    }

    private void updateChart() {
        dataset.clear();
        String filter = (String) filterDropdown.getSelectedItem();
        String dateCondition = getDateCondition(filter);

        try {
            Conn conn = new Conn();
            String[] categories = {"Food", "Bills", "Shopping", "Travel", "Other"};

            for (String category : categories) {
                ResultSet rs = conn.s.executeQuery(
                        "SELECT SUM(Amount) FROM bank WHERE Pin_Number='" + pinnumber + "' AND Category='" + category + "' " + dateCondition);

                if (rs.next()) {
                    double sumAmount = rs.getDouble(1);

                    if (sumAmount > 0) {
                        dataset.addValue(sumAmount, "Spending", category);

                        Double categoryBudget = categoryBudgets.get(category);
                        if (categoryBudget != null && sumAmount > categoryBudget) {
                            JOptionPane.showMessageDialog(this,
                                    "⚠️ Warning! Spending in " + category + " exceeded its budget of Rs " + categoryBudget,
                                    "Budget Limit Exceeded, Try to Save more on", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Spending Overview", "Category", "Amount (Rs)", dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL, true, true, false);

        CategoryPlot plot = (CategoryPlot) barChart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesItemLabelGenerator(0, new StandardCategoryItemLabelGenerator());
        renderer.setSeriesItemLabelsVisible(0, true);

        if (chartPanel != null) {
            chartPanel.setChart(barChart);
        }

        revalidate();
        repaint();
    }

    private String getDateCondition(String filter) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        String condition = "";

        if ("Daily".equals(filter)) {
            condition = "AND DATE(DateFormatted) = '" + sdf.format(cal.getTime()) + "'";
        } else if ("Weekly".equals(filter)) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
            condition = "AND DATE(DateFormatted) >= '" + sdf.format(cal.getTime()) + "'";
        } else if ("Monthly".equals(filter)) {
            cal.add(Calendar.MONTH, -1);
            condition = "AND DATE(DateFormatted) >= '" + sdf.format(cal.getTime()) + "'";
        } else if ("Custom".equals(filter)) {
            Date start = startChooser.getDate();
            Date end = endChooser.getDate();
            if (start != null && end != null) {
                String startDate = sdf.format(start);
                String endDate = sdf.format(end);
                condition = "AND DATE(DateFormatted) BETWEEN '" + startDate + "' AND '" + endDate + "'";
            } else {
                JOptionPane.showMessageDialog(this, "Please select both start and end dates.");
            }
        }

        return condition;
    }

    private void showBudgetDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        java.util.Map<String, JTextField> fields = new java.util.HashMap<>();

        for (String category : categoryBudgets.keySet()) {
            panel.add(new JLabel(category + ":"));
            JTextField field = new JTextField(categoryBudgets.get(category).toString());
            fields.put(category, field);
            panel.add(field);
        }

        int result = JOptionPane.showConfirmDialog(null, panel, "Set Category Budgets",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                for (String category : fields.keySet()) {
                    double value = Double.parseDouble(fields.get(category).getText());
                    categoryBudgets.put(category, value);
                }
                JOptionPane.showMessageDialog(this, "Budgets updated successfully.");
                updateChart();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid numbers.");
            }
        }
    }

    public static void main(String[] args) {
        new MiniStatement("1234"); // Use a real PIN when integrating
    }
}
