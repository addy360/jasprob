package com.addy360.jasbrob.controllers;

import com.addy360.jasbrob.dto.Message;
import com.addy360.jasbrob.dto.SystemData;
import com.addy360.jasbrob.dto.Welcome;
import com.addy360.jasbrob.models.Student;
import com.addy360.jasbrob.tasks.TasksCronjob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@Slf4j
public class HomeController {
    @Autowired
    TasksCronjob tasksCronjob;

    @MessageMapping("/hi")
    @SendTo("/message/greetings")
    public Welcome index(Message message) {
        return new Welcome("Your message was : " + message.getMessage());
    }

    @MessageMapping("/sys")
    @SendTo("/message/system/info")
    public SystemData systemData() {

        Runtime runtime = Runtime.getRuntime();
        SystemData data = new SystemData(
                runtime.availableProcessors(),
                runtime.freeMemory() / (1024 * 1024),
                runtime.maxMemory() / (1024 * 1024),
                runtime.totalMemory() / (1024 * 1024),
                new Date(System.currentTimeMillis())
        );
        log.info("system data was triggered, sending data : {}", data);
        return data;
    }

    @GetMapping(value = "/report/{format}", produces = MediaType.APPLICATION_PDF_VALUE)
    public @ResponseBody
    byte[] getReports(@PathVariable String format) {
        String url = "https://jsonplaceholder.typicode.com/users";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        HttpRequest request = builder.GET().build();
        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            List<Student> students = fetchStudents();

            return generateReport(students, format);
        } catch (Exception e) {
            return null;
        }


    }


    private byte[] generateReport(List<Student> students, String format) {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("reports/students.jrxml");
            log.info("Resource loaded : {}", stream);
            JRBeanCollectionDataSource data = new JRBeanCollectionDataSource(students);

            JasperReport report = JasperCompileManager.compileReport(stream);

            Map<String, Object> params = new HashMap<>();
            params.put("students", data);
            log.info("Compiled file {}", report);

            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            log.info("Error while generating report : {}", e.getMessage());
        }
        return new byte[0];
    }

    private List<Student> fetchStudents() throws IOException, InterruptedException {
        String url = "https://jsonplaceholder.typicode.com/users";
        log.info("Response for \n{}", url);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        HttpRequest request = builder.GET().build();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    private List<Student> handleResponse(HttpResponse<String> stringHttpResponse) {
        String body = stringHttpResponse.body();
        ObjectMapper om = new ObjectMapper();
        List<Student> students = new ArrayList<>();
        try {
            JsonNode jsonNode = om.readTree(body);
            log.info("Data Node : {}", jsonNode);
            jsonNode.forEach(user -> {
                log.info("User : {}", user);
                String id = user.get("id").asText();
                String[] split = user.get("name").asText().split(" ");
                String firstname = split[0];
                String lastname = split[1];
                String email = user.get("email").asText();
                Student student = new Student();
                student.setEmail(email);
                student.setFirstname(firstname);
                student.setId(Long.parseLong(id));
                student.setLastname(lastname);
                student.setCreatedAt(LocalDateTime.now().toString());
                students.add(student);
            });
            log.info("Students are : {}", students);
        } catch (JsonProcessingException e) {
            log.info("Error while loading response : {}", e.getMessage());
        }

        return students;
    }

}
