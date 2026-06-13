package com.picoding.fish

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FishApplication

fun main(args: Array<String>) {
	runApplication<FishApplication>(*args)
}
