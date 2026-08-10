package com.trivocab.ielts;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.trivocab.ielts.mapper")
@SpringBootApplication
public class IeltsVocabularyApplication {

    public static void main(String[] args) {
        SpringApplication.run(IeltsVocabularyApplication.class, args);
    }
}
