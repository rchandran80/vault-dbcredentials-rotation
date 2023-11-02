package com.example.springbootvaultdemo.dao;

import com.example.beans.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.*;

@Component
@Service
@Repository
public class DBConnectionService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${spring.cloud.vault.database.role}")
    private String databaseRole;

    @Value("${spring.datasource.url}")
    private String databaseURL;

    public void connectToPostgres(String vaultGenUserName, String vaultGenPassword) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager
                    .getConnection(databaseURL,
                            vaultGenUserName, vaultGenPassword);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select * from employee_salary");
            while (rs.next()) {
                Employee emp = new Employee();
                emp.setEmployeeNo(rs.getString("emp_no"));
                emp.setSalary(rs.getLong("salary"));
                emp.setEmployeeName(rs.getString("name"));
                emp.setHireDate(rs.getString("hire_date"));

                logger.info("********************");
                logger.info("Emp No: " + emp.getEmployeeNo());
                logger.info("Employee Name: " + emp.getEmployeeName());
                logger.info("Emp HireDate: " + emp.getHireDate());
                logger.info("Salary: " + emp.getSalary());
                logger.info("********************\n");
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error occured while connecting to DB"+databaseURL, e.getMessage());
            throw new RuntimeException(e);
        }finally {
            try {
                rs.close();
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}