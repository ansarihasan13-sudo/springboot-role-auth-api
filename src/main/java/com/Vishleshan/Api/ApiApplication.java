package com.Vishleshan.Api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		System.out.println("Before: " + TimeZone.getDefault().getID());

		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		System.out.println("After: " + TimeZone.getDefault().getID());

		SpringApplication.run(ApiApplication.class, args);
	}

}
