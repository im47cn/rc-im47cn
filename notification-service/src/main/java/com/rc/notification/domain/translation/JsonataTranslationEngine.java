package com.rc.notification.domain.translation;

import com.dashjoin.jsonata.JException;
import com.dashjoin.jsonata.Jsonata;
import com.dashjoin.jsonata.Timebox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JSONata 沙箱化声明式转换引擎
 * <p>
 * 纯内存闭环计算引擎，作为系统安全防腐底座。
 * 输入上游完整元数据，根据渠道配置的 JSONata 表达式，
 * 精准、无 RCE 风险地输出目标字符串。
 * <p>
 * 安全保障：
 * - jsonata-java 本身不提供文件系统、网络、反射访问原语，天然沙箱隔离
 * - 通过 Timebox 机制限制执行时间（5秒）和递归深度（1000层），防止恶意表达式 DoS
 */
@Component
public class JsonataTranslationEngine {

    private static final Logger log = LoggerFactory.getLogger(JsonataTranslationEngine.class);

    /** 表达式执行超时上限（毫秒） */
    private static final long EXECUTION_TIMEOUT_MS = 5000;

    /** 表达式递归深度上限 */
    private static final int MAX_DEPTH = 1000;

    private final ObjectMapper objectMapper;

    public JsonataTranslationEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 JSONata 表达式转换
     *
     * @param jsonataExpression JSONata 表达式字符串
     * @param inputContext      输入上下文（Map/POJO，将被转为 jsonata-java 可识别的结构）
     * @return 转换结果字符串，null 表达式结果返回空字符串
     * @throws TranslationEngineException 表达式语法错误或计算异常
     */
    public String transform(String jsonataExpression, Object inputContext) {
        return transformWithBindings(jsonataExpression, inputContext, null);
    }

    /**
     * 执行带外部绑定变量的 JSONata 表达式转换
     *
     * @param jsonataExpression JSONata 表达式字符串
     * @param inputContext      输入上下文
     * @param bindings          外部绑定变量（可选，key-value 形式注入到表达式作用域）
     * @return 转换结果字符串
     * @throws TranslationEngineException 表达式语法错误或计算异常
     */
    @SuppressWarnings("unchecked")
    public String transformWithBindings(String jsonataExpression, Object inputContext,
                                         Map<String, Object> bindings) {
        if (jsonataExpression == null || jsonataExpression.isBlank()) {
            return "";
        }

        try {
            // 解析表达式
            Jsonata expr = Jsonata.jsonata(jsonataExpression);

            // 创建 Frame 并注入 Timebox 超时保护
            Jsonata.Frame frame = expr.createFrame();
            new Timebox(frame, EXECUTION_TIMEOUT_MS, MAX_DEPTH);

            // 注入外部绑定变量
            if (bindings != null && !bindings.isEmpty()) {
                for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                    expr.assign(entry.getKey(), entry.getValue());
                }
            }

            // 将输入上下文统一转为 Map 结构供 jsonata-java 消费
            Object input = normalizeInput(inputContext);

            // 执行表达式
            Object result = expr.evaluate(input, frame);

            // 结果序列化
            if (result == null) {
                return "";
            }
            if (result instanceof String) {
                return (String) result;
            }
            return objectMapper.writeValueAsString(result);

        } catch (JException e) {
            log.warn("JSONata 表达式执行失败: expression=[{}], offset={}, error={}",
                    jsonataExpression, e.getLocation(), e.getMessage());
            throw new TranslationEngineException(
                    "JSONata 表达式执行失败: " + e.getDetailedErrorMessage(),
                    e.getLocation(), e);
        } catch (TranslationEngineException e) {
            throw e;
        } catch (Exception e) {
            log.error("JSONata 引擎内部错误: expression=[{}]", jsonataExpression, e);
            throw new TranslationEngineException(
                    "JSONata 引擎内部错误: " + e.getMessage(), -1, e);
        }
    }

    /**
     * 将输入上下文标准化为 jsonata-java 可消费的 Map/List 结构
     */
    @SuppressWarnings("unchecked")
    private Object normalizeInput(Object inputContext) {
        if (inputContext == null) {
            return null;
        }
        if (inputContext instanceof Map) {
            return inputContext;
        }
        // POJO 或其他类型通过 Jackson 中转为 Map
        return objectMapper.convertValue(inputContext, Map.class);
    }
}
