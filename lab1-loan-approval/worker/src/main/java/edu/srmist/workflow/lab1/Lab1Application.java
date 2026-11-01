package edu.srmist.workflow.lab1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Runs the Lab 1 job worker and the mock credit-assessment API in one Spring Boot app. */
@SpringBootApplication
public class Lab1Application {

    public static void main(String[] args) {
        SpringApplication.run(Lab1Application.class, args);
    }
}
