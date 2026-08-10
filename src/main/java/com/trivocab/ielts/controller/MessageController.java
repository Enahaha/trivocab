package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.common.CurrentUserProvider;
import com.trivocab.ielts.dto.MessageCreateRequest;
import com.trivocab.ielts.dto.MessageResponse;
import com.trivocab.ielts.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    private final MessageService messageService;
    private final CurrentUserProvider currentUser;

    public MessageController(MessageService messageService, CurrentUserProvider currentUser) {
        this.messageService = messageService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<List<MessageResponse>> list() {
        return ApiResponse.ok(messageService.list(currentUser.userId()));
    }

    @PostMapping
    public ApiResponse<MessageResponse> create(@Valid @RequestBody MessageCreateRequest request) {
        return ApiResponse.ok(
                messageService.create(currentUser.userId(), request.content()),
                "留言已提交"
        );
    }
}
