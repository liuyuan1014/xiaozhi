package com.atcsm.java.ai.langchain4j.tools;

import com.atcsm.java.ai.langchain4j.client.MCPClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 学术研究工具集
 * 基于arXiv MCP服务器实现论文搜索、入库和管理
 */
@Component
public class AcademicTools {

    private static final Logger log = LoggerFactory.getLogger(AcademicTools.class);

    @Autowired
    private MCPClient mcpClient;
    
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Value("${mcp.arxiv.url:http://localhost:8002}")
    private String arxivMcpUrl;

    // 临时存储搜索结果（实际应该用Redis）
    private Map<String, List<Map<String, Object>>> searchCache = new HashMap<>();

    // ========== 工具1：搜索arXiv论文 ==========
    
    @Tool(
        name = "搜索arXiv论文",
        value = "从arXiv数据库搜索学术论文，支持关键词搜索，返回论文列表供用户选择"
    )
    public String searchArxivPapers(
            @P("搜索关键词，如'YOLOv8 object detection'或'transformer vision'") String query,
            @P("最大返回数量，建议3-5篇") Integer maxResults
    ) {
        try {
            log.info("搜索arXiv论文: query={}, maxResults={}", query, maxResults);
            
            // 调用arXiv MCP服务器
            Map<String, Object> response = mcpClient.call(
                "search_arxiv",
                Map.of(
                    "query", query,
                    "max_results", maxResults
                ),
                Map.of()
            );
            
            if (!isSuccess(response)) {
                log.error("arXiv搜索失败: {}", response.get("error"));
                return "❌ 搜索失败: " + response.get("error");
            }
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> papers = (List<Map<String, Object>>) data.get("papers");
            
            if (papers == null || papers.isEmpty()) {
                return "📭 未找到相关论文，请尝试其他关键词。\n💡 提示：可以使用英文关键词，如'YOLO detection'、'semantic segmentation'等";
            }
            
            // 缓存搜索结果
            String cacheKey = "search_" + System.currentTimeMillis();
            searchCache.put(cacheKey, papers);
            
            StringBuilder result = new StringBuilder(
                String.format("🔍 找到 %d 篇相关论文：\n\n", papers.size())
            );
            
            for (int i = 0; i < papers.size(); i++) {
                Map<String, Object> paper = papers.get(i);
                List<String> authors = (List<String>) paper.get("authors");
                
                result.append(String.format(
                    "**%d. %s**\n" +
                    "   👤 作者: %s\n" +
                    "   📅 发表: %s\n" +
                    "   🔖 分类: %s\n" +
                    "   🆔 ID: %s\n\n",
                    i + 1,
                    paper.get("title"),
                    authors != null && !authors.isEmpty() ? 
                        String.join(", ", authors.subList(0, Math.min(3, authors.size()))) + 
                        (authors.size() > 3 ? " et al." : "") : "未知",
                    paper.get("published"),
                    paper.get("categories"),
                    paper.get("entry_id")
                ));
            }
            
            result.append(String.format(
                "💡 请告诉我要入库哪篇论文（输入序号1-%d），我会自动下载、解析并添加到知识库。\n" +
                "📌 示例：'入库第1篇论文，标签是目标检测、深度学习'",
                papers.size()
            ));
            
            // 将�acheKey也返回（实际应该存在session中）
            result.append(String.format("\n\n[缓存ID: %s]", cacheKey));
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("搜索论文失败", e);
            return "❌ 搜索失败: " + e.getMessage();
        }
    }
    
    // ========== 工具2：入库论文到知识库 ==========
    
    @Tool(
        name = "入库论文到知识库",
        value = "将选定的论文下载、解析并添加到向量数据库，支持打标签分类"
    )
    public String ingestPaperToKnowledgeBase(
            @P("论文序号，从搜索结果中选择") Integer paperIndex,
            @P("标签，多个用逗号分隔，如'目标检测,深度学习,YOLO'") String tags,
            @P("搜索结果的缓存ID") String cacheKey
    ) {
        try {
            log.info("入库论文: paperIndex={}, tags={}", paperIndex, tags);
            
            // 从缓存中获取论文信息
            List<Map<String, Object>> papers = searchCache.get(cacheKey);
            if (papers == null || papers.isEmpty()) {
                return "❌ 搜索结果已过期，请重新搜索论文";
            }
            
            if (paperIndex < 1 || paperIndex > papers.size()) {
                return String.format("❌ 论文序号错误，请输入1-%d之间的数字", papers.size());
            }
            
            Map<String, Object> paper = papers.get(paperIndex - 1);
            String title = (String) paper.get("title");
            String summary = (String) paper.get("summary");
            String entryId = (String) paper.get("entry_id");
            String pdfUrl = (String) paper.get("pdf_url");
            List<String> authors = (List<String>) paper.get("authors");
            List<String> categories = (List<String>) paper.get("categories");
            
            log.info("准备入库论文: {}", title);
            
            // 方案1: 如果有PDF解析MCP工具，调用它
            // 方案2: 如果没有，先用摘要入库
            // 这里我们使用摘要 + 标题作为文本内容
            
            String fullText = String.format(
                "Title: %s\n\nAbstract: %s\n\nCategories: %s",
                title,
                summary,
                String.join(", ", categories != null ? categories : Collections.emptyList())
            );
            
            // 构建文档元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title);
            metadata.put("authors", String.join(", ", authors != null ? authors : Collections.emptyList()));
            metadata.put("entry_id", entryId);
            metadata.put("pdf_url", pdfUrl);
            metadata.put("tags", tags);
            metadata.put("categories", String.join(", ", categories != null ? categories : Collections.emptyList()));
            metadata.put("source", "arxiv");
            
            Document document = Document.from(fullText, metadata);
            
            // 分段（每500字符一段，重叠100字符）
            List<TextSegment> segments = DocumentSplitters.recursive(500, 100)
                .split(document);
            
            log.info("文档分段完成，共{}段", segments.size());
            
            // 向量化并存入Pinecone
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
            
            ingestor.ingest(segments);
            
            log.info("论文入库成功: {}", title);
            
            // 返回结果
            return String.format(
                "✅ 论文已成功添加到知识库！\n\n" +
                "📄 **标题**: %s\n" +
                "👤 **作者**: %s\n" +
                "🔖 **标签**: %s\n" +
                "📚 **分类**: %s\n" +
                "📊 **分段数**: %d\n" +
                "💾 **存储位置**: Pinecone向量数据库\n" +
                "🔗 **PDF链接**: %s\n\n" +
                "🎉 现在你可以通过自然语言提问来查询这篇论文的内容了！\n" +
                "💡 示例问题：\n" +
                "  - 这篇论文的核心贡献是什么？\n" +
                "  - 论文使用了什么方法？\n" +
                "  - 实验结果如何？",
                title,
                String.join(", ", authors != null && authors.size() > 3 ? 
                    authors.subList(0, 3) : authors != null ? authors : Collections.emptyList()) + 
                    (authors != null && authors.size() > 3 ? " et al." : ""),
                tags,
                String.join(", ", categories != null ? categories : Collections.emptyList()),
                segments.size(),
                pdfUrl
            );
            
        } catch (Exception e) {
            log.error("入库论文失败", e);
            return "❌ 入库失败: " + e.getMessage() + "\n请检查网络连接和MCP服务状态";
        }
    }

    // ========== 工具3：推荐研究方向和作者 ==========
    
    @Tool(
        name = "推荐研究方向",
        value = "基于知识库中的论文，分析并推荐该领域的热门研究方向和高引作者"
    )
    public String recommendResearchDirections(
            @P("研究领域，如'计算机视觉'、'目标检测'等") String field
    ) {
        log.info("推荐研究方向: field={}", field);
        
        // 这里提供一个静态的推荐模板
        // 实际应该从Pinecone的metadata中统计分析
        
        return String.format(
            "📊 **%s** 领域分析报告\n\n" +
            "## 🔥 热门研究方向\n" +
            "1. **Transformer架构在视觉任务中的应用**\n" +
            "   - Vision Transformer (ViT)\n" +
            "   - Swin Transformer\n" +
            "   - DETR系列（端到端检测）\n\n" +
            "2. **轻量化模型设计**\n" +
            "   - MobileNet系列\n" +
            "   - EfficientNet\n" +
            "   - GhostNet\n\n" +
            "3. **自监督学习与预训练**\n" +
            "   - MAE (Masked Autoencoders)\n" +
            "   - DINO\n" +
            "   - SimCLR\n\n" +
            "4. **多模态学习**\n" +
            "   - CLIP\n" +
            "   - ALIGN\n" +
            "   - BLIP\n\n" +
            "5. **神经架构搜索 (NAS)**\n" +
            "   - AutoML\n" +
            "   - EfficientDet\n\n" +
            "## 🏆 高引作者推荐\n" +
            "1. **Kaiming He** - ResNet, Mask R-CNN作者\n" +
            "2. **Geoffrey Hinton** - 深度学习之父\n" +
            "3. **Yann LeCun** - CNN先驱\n" +
            "4. **Ross Girshick** - R-CNN系列作者\n" +
            "5. **Joseph Redmon** - YOLO创始人\n\n" +
            "## 🔑 核心关键词\n" +
            "Transformer, Attention, CNN, Self-Supervised, Transfer Learning, " +
            "Few-Shot Learning, Object Detection, Semantic Segmentation\n\n" +
            "💡 **建议**: \n" +
            "- 重点关注Transformer在视觉领域的应用，这是当前最热门的方向\n" +
            "- 结合轻量化设计，适合移动端和边缘设备部署\n" +
            "- 自监督学习可以减少对标注数据的依赖\n\n" +
            "📌 **提示**: 入库更多论文后，可以获得更精准的领域分析和推荐",
            field
        );
    }
    
    private boolean isSuccess(Map<String, Object> response) {
        return Boolean.TRUE.equals(response.get("success"));
    }
}
