package com.example.demo.hub;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The single main menu. Every scenario links back here. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
