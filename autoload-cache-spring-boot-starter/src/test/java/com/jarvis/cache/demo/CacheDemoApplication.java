package com.jarvis.cache.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.PageRequest;

import com.jarvis.cache.demo.condition.UserCondition;
import com.jarvis.cache.demo.mapper.UserMapper;

@SpringBootApplication
@MapperScan("com.jarvis.cache.demo.mapper")
public class CacheDemoApplication {

    
    public static void main(String[] args) {
        try {
            configureApplication(new SpringApplicationBuilder()).run(args);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) {
        ApplicationListener<ApplicationReadyEvent>  readyListener=new ApplicationListener<ApplicationReadyEvent> () {

            @Override
            public void onApplicationEvent(ApplicationReadyEvent event) {
                ConfigurableApplicationContext context = event.getApplicationContext();
                UserMapper userMapper=context.getBean(UserMapper.class);
                userMapper.allUsers();
                
                UserCondition condition = new UserCondition();
                PageRequest page =new PageRequest(1, 10);
                condition.setPageable(page);
                condition.setStatus(1);
                userMapper.listByCondition(condition);
            }
            
        };
        return builder.sources(CacheDemoApplication.class).bannerMode(Banner.Mode.OFF).listeners(readyListener);
    }
}
