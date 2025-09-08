package org.example.secureshare.controller;

import org.example.secureshare.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;
}
