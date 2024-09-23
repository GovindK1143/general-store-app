package com.govind.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.govind.client.WelcomeFeignClient;

@RestController
public class GreetRestController {
	
	@Autowired
	private WelcomeFeignClient welcomeClient;
	
	@GetMapping("/greet")
	public String getGreetMsg() {
		
		String weclomeMsg=welcomeClient.getWelcomeMsg();
		String greetMsg = "Good Morning...!";
		
		return greetMsg + weclomeMsg;
	}

}
