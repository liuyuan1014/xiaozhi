package com.atcsm.java.ai.langchain4j.service;

import com.atcsm.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atcsm.java.ai.langchain4j.client.MCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    @Autowired
    private MCPClient mcpClient;

    @Autowired
    private XiaozhiAgent aiAgent;

    @Value("${github.token}")
    private String githubToken;

    public void reviewPR(String repo, int prNumber) {
        try {
            log.info("开始审查 {}/PR#{}", repo, prNumber);

            // 1. 获取PR基本信息
            Map<String, Object> prInfoResponse = mcpClient.call(
                    "get_pr_info",
                    Map.of("repo", repo, "pr_number", prNumber),
                    Map.of("github_token", githubToken)
            );

            if (!isSuccess(prInfoResponse)) {
                log.error("获取PR信息失败: {}", prInfoResponse.get("error"));
                return;
            }

            Map<String, Object> prInfo = (Map<String, Object>) prInfoResponse.get("data");

            // 2. 获取代码变更
            Map<String, Object> diffResponse = mcpClient.call(
                    "get_pr_diff",
                    Map.of("repo", repo, "pr_number", prNumber),
                    Map.of("github_token", githubToken)
            );

            if (!isSuccess(diffResponse)) {
                log.error("获取代码变更失败: {}", diffResponse.get("error"));
                return;
            }

            Map<String, Object> diff = (Map<String, Object>) diffResponse.get("data");

            // 3. 获取完整文件内容（增强上下文）
            List<Map<String, Object>> files = (List<Map<String, Object>>) diff.get("files");
            StringBuilder fileContents = new StringBuilder();
            
            for (Map<String, Object> file : files) {
                String filename = (String) file.get("filename");
                String status = (String) file.get("status");
                
                // 只获取 Java 文件的完整内容，且不是删除的文件
                if (filename.endsWith(".java") && !"removed".equals(status)) {
                    try {
                        Map<String, Object> contentResponse = mcpClient.call(
                                "get_file_content",
                                Map.of(
                                        "repo", repo,
                                        "path", filename,
                                        "ref", prInfo.get("head_sha") // 使用 PR 的最新 commit SHA
                                ),
                                Map.of("github_token", githubToken)
                        );

                        if (isSuccess(contentResponse)) {
                            Map<String, Object> data = (Map<String, Object>) contentResponse.get("data");
                            String content = (String) data.get("content");
                            fileContents.append("\n========== 文件: ").append(filename).append(" ==========\n");
                            fileContents.append(content).append("\n");
                        }
                    } catch (Exception e) {
                        log.warn("获取文件内容失败: {}", filename, e);
                    }
                }
            }

            // 4. AI分析
            String prompt = buildReviewPrompt(prInfo, diff, fileContents.toString());
            String aiReview = aiAgent.chat(1L, prompt);

            log.info("AI分析完成，准备提交评审");

            // 4. 提交评审
            Map<String, Object> reviewResponse = mcpClient.call(
                    "create_pr_review",
                    Map.of(
                            "repo", repo,
                            "pr_number", prNumber,
                            "body", aiReview,
                            "event", "COMMENT"
                    ),
                    Map.of("github_token", githubToken)
            );

            if (isSuccess(reviewResponse)) {
                log.info("代码审查完成: {}/PR#{}", repo, prNumber);
            } else {
                log.error("提交评审失败: {}", reviewResponse.get("error"));
            }

        } catch (Exception e) {
            log.error("审查PR失败: {}/#{}", repo, prNumber, e);
        }
    }

    private boolean isSuccess(Map<String, Object> response) {
        return Boolean.TRUE.equals(response.get("success"));
    }

    private String buildReviewPrompt(Map<String, Object> prInfo, Map<String, Object> diff, String fullFileContents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请作为资深代码审查专家，分析以下Pull Request：\n\n");
        prompt.append("PR标题：").append(prInfo.get("title")).append("\n");
        prompt.append("作者：").append(prInfo.get("author")).append("\n");
        prompt.append("描述：").append(prInfo.get("description")).append("\n\n");
        
        if (fullFileContents != null && !fullFileContents.isEmpty()) {
            prompt.append("为了帮助你更好地理解上下文，以下是修改文件的完整内容：\n");
            prompt.append(fullFileContents).append("\n\n");
        }
        
        prompt.append("代码变更统计：\n");
        prompt.append("- 新增行数：").append(diff.get("total_additions")).append("\n");
        prompt.append("- 删除行数：").append(diff.get("total_deletions")).append("\n");
        prompt.append("- 修改文件数：").append(diff.get("changed_files")).append("\n\n");

        // 添加文件变更详情
        List<Map<String, Object>> files = (List<Map<String, Object>>) diff.get("files");
        prompt.append("文件变更详情（Diff）：\n");
        for (Map<String, Object> file : files) {
            prompt.append("\n文件：").append(file.get("filename")).append("\n");
            prompt.append("状态：").append(file.get("status")).append("\n");
            prompt.append("变更内容：\n");
            prompt.append(file.get("patch")).append("\n");
        }

        prompt.append("\n请作为Java资深专家，结合完整文件上下文和Diff，重点从以下Java特定角度进行深度分析：\n");
        prompt.append("1. Java并发安全：\n");
        prompt.append("   - 检查是否存在线程安全问题（如HashMap在多线程环境下的使用）\n");
        prompt.append("   - 检查锁的使用是否合理（死锁风险、锁粒度过大）\n");
        prompt.append("   - 检查ThreadLocal是否正确清理（内存泄漏风险）\n");
        prompt.append("2. Java集合与内存优化：\n");
        prompt.append("   - 检查集合初始化是否指定容量（避免频繁扩容）\n");
        prompt.append("   - 检查是否存在内存泄漏（如静态集合无限增长、未关闭的流）\n");
        prompt.append("   - 检查是否使用了低效的遍历方式\n");
        prompt.append("3. 异常处理规范：\n");
        prompt.append("   - 检查是否捕获了Generic Exception（如catch(Exception e)）\n");
        prompt.append("   - 检查是否吞掉了异常（catch块为空或仅打印日志）\n");
        prompt.append("   - 检查资源关闭是否使用了try-with-resources\n");
        prompt.append("4. 数据库与事务：\n");
        prompt.append("   - 检查@Transactional注解的使用是否正确（如自调用失效问题）\n");
        prompt.append("   - 检查是否存在N+1查询问题\n");
        prompt.append("   - 检查SQL注入风险（MyBatis/JPA参数绑定）\n");
        prompt.append("5. 中间件与框架使用：\n");
        prompt.append("   - Spring Boot：检查Bean的生命周期管理、依赖注入是否合理\n");
        prompt.append("   - Redis：检查缓存穿透/击穿/雪崩风险、Key命名规范、过期时间设置\n");
        prompt.append("   - Kafka/MQ：检查消息丢失风险、重复消费处理（幂等性）、死信队列配置\n");
        prompt.append("   - MongoDB：检查索引使用、大文档查询性能、聚合查询效率\n");
        prompt.append("6. 代码规范（阿里巴巴Java开发手册）：\n");
        prompt.append("   - 命名规范（驼峰、常量大写等）\n");
        prompt.append("   - 注释规范（Javadoc、方法说明）\n");
        prompt.append("   - 魔法值检查（Magic Number）\n\n");
        prompt.append("请给出具体的修改建议，并提供优化后的Java代码示例。");

        return prompt.toString();
    }
}
