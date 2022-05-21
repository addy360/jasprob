package com.addy360.jasbrob.controllers;

import com.addy360.jasbrob.dto.Message;
import com.addy360.jasbrob.dto.SystemData;
import com.addy360.jasbrob.dto.Welcome;
import com.addy360.jasbrob.models.Comment;
import com.addy360.jasbrob.models.Post;
import com.addy360.jasbrob.models.Student;
import com.addy360.jasbrob.tasks.TasksCronjob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.format.DateTimeFormatter;
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

    @GetMapping(value = "/report/{type}", produces = MediaType.APPLICATION_PDF_VALUE)
    public @ResponseBody
    byte[] getReports(@PathVariable String type) {
        switch (type) {
            case "students":
                return studentReport();
            case "posts":
                return postReport();
            default:
                return null;
        }

    }

    @GetMapping("/comments")
    public ResponseEntity<byte[]> getCommentReport() {
        byte[] bytes = commentsReport();
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    private byte[] studentReport() {
        try {
            List<Student> students = fetchStudents();
            return generateReport(students);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] commentsReport() {
        List<Comment> comments;
        try {
            comments = fetchComments();
        } catch (Exception e) {
            log.info("Error occurred while fetching comments : {}", e.getMessage());
            return null;
        }
        log.info("Comments {} loaded", comments.size());

        String dir = "reports/";
        String file = dir.concat("comments.jrxml");

        InputStream stream = getClass().getClassLoader().getResourceAsStream(file);
        JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(comments);
        JasperReport report;
        JasperPrint print;
        try {
            report = JasperCompileManager.compileReport(stream);
            Map<String, Object> params = new HashMap<>();
            params.put("comments", source);
            print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            log.info("Failed to compile report with error : {}", e.getMessage());
            return null;
        }

    }

    private byte[] postReport() {
        try {
            List<Post> posts = fetchPosts();
            return generatePostReport(posts);
        } catch (Exception e) {
            log.info("Error while loading posts report: {}", e.getMessage());
            return null;
        }
    }

    private byte[] generatePostReport(List<Post> posts) throws JRException {
        return getReportPostsBytes(posts);
    }


    private byte[] generateReport(List<Student> students) {
        try {
            return getReportStudentBytes(students);

        } catch (Exception e) {
            log.info("Error while generating report : {}", e.getMessage());
        }
        return new byte[0];
    }

    private byte[] getReportStudentBytes(List<Student> objectList) throws JRException {
        String dir = "reports/";
        String file = dir.concat("students").concat(".jrxml");
        InputStream stream = getClass().getClassLoader().getResourceAsStream(file);
        log.info("Resource loaded file : {} : {}", file, stream);
        JRBeanCollectionDataSource data = new JRBeanCollectionDataSource(objectList);

        JasperReport report = JasperCompileManager.compileReport(stream);

        Map<String, Object> params = new HashMap<>();
        params.put("students", data);
        log.info("Compiled file {}", report);

        JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    private byte[] getReportPostsBytes(List<Post> posts) throws JRException {
        String dir = "reports/";
        String file = dir.concat("posts").concat(".jrxml");
        InputStream stream = getClass().getClassLoader().getResourceAsStream(file);
        JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(posts);

        JasperReport report = JasperCompileManager.compileReport(stream);

        Map<String, Object> params = new HashMap<>();
        params.put("posts", source);
        params.put("pageTitle", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
        JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    private List<Student> fetchStudents() throws IOException, InterruptedException {
        String url = "https://jsonplaceholder.typicode.com/users";
        log.info("Response for \n{}", url);
        HttpResponse<String> response = getResponse(url);
        return handleResponse(response);
    }

    private List<Comment> fetchComments() throws Exception {
        String url = "https://jsonplaceholder.typicode.com/comments";
        List<Comment> comments = new ArrayList<>();

        HttpResponse<String> response = getResponse(url);
        log.info("Response for : {} is : {}", url, response.statusCode());
        ObjectMapper om = new ObjectMapper();
        JsonNode jsonNode = om.readTree(response.body());
        jsonNode.forEach(cm -> {
            Comment comment = new Comment();
            comment.setBody(cm.get("body").asText());
            comment.setName(cm.get("name").asText());
            comment.setEmail(cm.get("email").asText());
            comments.add(comment);
        });

        return comments;
    }

    List<Post> fetchPosts() throws IOException, InterruptedException {
        String url = "https://jsonplaceholder.typicode.com/posts";
        log.info("Sending request to : {}", url);
        HttpResponse<String> response = getResponse(url);
        return handlePostResponse(response);
    }

    private List<Post> handlePostResponse(HttpResponse<String> response) {
        List<Post> posts = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = om.readTree(response.body());
            jsonNode.forEach(jn -> {
                String id = jn.get("id").asText();
                String title = jn.get("title").asText();
                String body = jn.get("body").asText();
                Post post = new Post();
                post.setBody(body);
                post.setId(Long.parseLong(id));
                post.setTitle(title);
                posts.add(post);
            });
            log.info("Posts fetched are : {}", posts);
        } catch (JsonProcessingException e) {
            log.info("Error while loading response : {}", e.getMessage());
            return posts;
        }
        return posts;
    }

    private HttpResponse<String> getResponse(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        HttpRequest request = builder.GET().build();
        HttpClient httpClient = HttpClient.newHttpClient();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
