package com.atcsm.java.ai.langchain4j.Controller;

import com.alibaba.fastjson2.JSONObject;
import com.atcsm.java.ai.langchain4j.service.CodeReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/webhook")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    @Autowired
    private CodeReviewService codeReviewService;

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload
    ) {
        log.info("收到GitHub Webhook事件: {}", event);

        try {
            if ("pull_request".equals(event)) {
                handlePullRequestEvent(payload);
                return ResponseEntity.ok("Webhook已处理");
            }

            return ResponseEntity.ok("事件已忽略");
        } catch (Exception e) {
            log.error("处理Webhook失败", e);
            return ResponseEntity.status(500).body("错误: " + e.getMessage());
        }
    }

    private void handlePullRequestEvent(String payload) {
        JSONObject json = JSONObject.parseObject(payload);
        String action = json.getString("action");

        // 只处理PR创建和更新事件
        if ("opened".equals(action) || "synchronize".equals(action)) {
            JSONObject pr = json.getJSONObject("pull_request");
            JSONObject repo = json.getJSONObject("repository");

            String repoFullName = repo.getString("full_name");
            int prNumber = pr.getIntValue("number");

            log.info("触发代码审查: {}/PR#{}", repoFullName, prNumber);

            // 异步执行审查
            CompletableFuture.runAsync(() -> {
                codeReviewService.reviewPR(repoFullName, prNumber);
            });
        }
    }
}
