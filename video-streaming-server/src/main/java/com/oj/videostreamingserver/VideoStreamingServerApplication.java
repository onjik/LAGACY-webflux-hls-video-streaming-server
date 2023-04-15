package com.oj.videostreamingserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class VideoStreamingServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoStreamingServerApplication.class, args);
		BlockHound.install();
	}

}
