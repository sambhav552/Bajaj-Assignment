package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

import org.springframework.http.HttpHeaders;

@Component
public class ExamService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Bean
    public ApplicationRunner runAtStartup() {
        return args -> {
            
            String regNo = "REG12347"; 
            String name = "Sambhav Gupta";
            String email = "sambhav@example.com";

            // 1. Generate Webhook
            String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("regNo", regNo);
            body.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JSONObject json = new JSONObject(response.getBody());
            String webhook = json.getString("webhook");
            String accessToken = json.getString("accessToken");

            // 2. SQL Query
            int lastDigit = Integer.parseInt(regNo.replaceAll("\\D", "")) % 10;
            boolean isOdd = lastDigit % 2 != 0;

            String finalQuery;
            if (isOdd) {
                
                finalQuery = "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                        "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, d.DEPARTMENT_NAME " +
                        "FROM PAYMENTS p " +
                        "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                        "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                        "WHERE DAY(p.PAYMENT_TIME) != 1 " +
                        "AND p.AMOUNT = ( " +
                        "    SELECT MAX(AMOUNT) FROM PAYMENTS WHERE DAY(PAYMENT_TIME) != 1 " +
                        ") " +
                        "LIMIT 1;";
            } else {
                
                finalQuery = "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
                        "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
                        "FROM EMPLOYEE e1 " +
                        "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
                        "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB " +
                        "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
                        "ORDER BY e1.EMP_ID DESC;";
            }

            
            JSONObject submitBody = new JSONObject();
            submitBody.put("finalQuery", finalQuery);

            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            submitHeaders.setBearerAuth(accessToken);

            HttpEntity<String> submitEntity = new HttpEntity<>(submitBody.toString(), submitHeaders);
            ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhook, submitEntity, String.class);

            System.out.println("Submitted: " + submitResponse.getBody());
        };
    }
}
