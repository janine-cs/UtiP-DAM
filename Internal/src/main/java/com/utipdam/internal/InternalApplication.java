package com.utipdam.internal;

import com.utipdam.internal.model.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class InternalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternalApplication.class, args);
    }
    public static String token;

    @Value("${utipdam.app.internalApi}")
    private String uri;

    @Component
    public class MyComponent{
        @PostConstruct
        public void initialize(){

//            String url = UriComponentsBuilder
//                    .fromUriString(uri+ "/auth/signin")
//                    .build().toUriString();
//            RestTemplate restTemplate = new RestTemplate();
//            String requestJson = "{\"username\":\"admin\", \"password\":\"corrsys1234\"}";
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<String> entity = new HttpEntity<>(requestJson ,headers);
//            Token tokenObj = restTemplate.exchange(url,
//                    HttpMethod.POST, entity, new ParameterizedTypeReference<Token>() {
//                    }).getBody();
//
//            if (tokenObj != null){
//               token = tokenObj.getToken();
//            }
//            token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcxMDgyOTE1MSwiZXhwIjoxNzEwOTE1NTUxfQ.eEQxNt47bfxzZrWxdgzcOuwL5F-h4q4rbXn6eSuRB0g";
        }
    }

}
