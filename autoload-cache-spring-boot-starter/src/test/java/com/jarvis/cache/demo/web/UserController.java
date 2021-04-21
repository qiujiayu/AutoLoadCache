package com.jarvis.cache.demo.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jarvis.cache.demo.condition.UserCondition;
import com.jarvis.cache.demo.entity.UserDO;
import com.jarvis.cache.demo.service.UserDAO;
import com.jarvis.cache.demo.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired 
    private UserDAO userDAO;
    
    @GetMapping()
    public List<UserDO> list() {
        UserCondition condition = new UserCondition();
        userDAO.listByCondition(condition);
        return userService.listByCondition(condition);
    }

    @GetMapping("/{id}")
    public UserDO detail(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/add")
    public UserDO add() {
        UserDO user = new UserDO();
        user.setName("name_" + System.currentTimeMillis());
        user.setPassword("11111");
        userService.register(user);
        return user;
    }

    @GetMapping("/update/{id}")
    public void update(@PathVariable Long id) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setName("name:" + id);
        userService.updateUser(user);
    }

}
