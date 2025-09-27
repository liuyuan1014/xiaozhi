package com.atcsm.java.ai.langchain4j.Controller;

import com.atcsm.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atcsm.java.ai.langchain4j.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "小智助手")
@RestController
@RequestMapping("/xiaozhi")
public class XiaozhiController {

    @Autowired
    private XiaozhiAgent xiaozhiAgent;


    @Operation(summary = "对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm) {
        return  xiaozhiAgent.chat(chatForm.getMemoryId(),chatForm.getMessage());
    }
}
