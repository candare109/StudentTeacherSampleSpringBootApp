package com.codeWithJeff.SampleSpringBootApplication.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class SpringRestController {
    @GetMapping("/test")
    public String message(){
        return "this is a test";
    }
}
