package com.geowarin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/toto")
    public String index() {
        return "sub/subPage";
    }
}