package com.geowarin

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class BootWpApplication

fun main(args: Array<String>) {
    SpringApplication.run(BootWpApplication::class.java, *args)
}
