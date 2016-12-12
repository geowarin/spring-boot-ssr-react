package com.geowarin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
open class IndexController {

    @GetMapping("/toto")
    fun index(): String {
        return "index"
    }
}