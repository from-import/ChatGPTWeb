package com.fromimport.chatgptweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fromimport.chatgptweb.mapper")
public class ChatGptWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatGptWebApplication.class, args);
	}

}
