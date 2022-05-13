package com.custom.postprocessing;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

/**
 * @author kumar.charanswain
 *
 */
@SpringBootApplication
@EnableScheduling
@EnableEncryptableProperties
public class PostProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(PostProcessingApplication.class, args);
	}


	static {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		System.setProperty("current.date.time", dateFormat.format(new Date()));
	}
}
