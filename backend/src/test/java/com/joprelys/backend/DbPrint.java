package com.joprelys.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbPrint {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/joprelys", "postgres", "postgres");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, result_number, pdf_file_path FROM lab_results");
            System.out.println("=== LAB RESULTS IN LIVE DB ===");
            while (rs.next()) {
                System.out.println("ID: " + rs.getString("id") + 
                                   " | Num: " + rs.getString("result_number") + 
                                   " | Path: " + rs.getString("pdf_file_path"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
