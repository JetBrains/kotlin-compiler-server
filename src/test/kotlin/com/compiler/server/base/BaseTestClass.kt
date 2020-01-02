package com.compiler.server.base

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource("classpath:libraries.properties")
class BaseTestClass