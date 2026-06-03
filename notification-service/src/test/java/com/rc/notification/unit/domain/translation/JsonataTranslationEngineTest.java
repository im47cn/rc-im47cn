package com.rc.notification.unit.domain.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.translation.JsonataTranslationEngine;
import com.rc.notification.domain.translation.TranslationEngineException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonataTranslationEngine 单元测试
 * <p>
 * 验证 JSONata 表达式求值、错误处理、边界条件
 */
class JsonataTranslationEngineTest {

    private JsonataTranslationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new JsonataTranslationEngine(new ObjectMapper());
    }

    @Test
    @DisplayName("简单字段提取: $.eventId")
    void transform_simpleFieldExtraction() {
        Map<String, Object> input = Map.of("eventId", "EVT-001", "type", "order");

        String result = engine.transform("eventId", input);

        assertEquals("EVT-001", result);
    }

    @Test
    @DisplayName("嵌套对象转换")
    void transform_nestedObjectTransform() {
        Map<String, Object> input = Map.of(
                "user", Map.of("name", "Alice", "age", 30),
                "action", "login"
        );

        // 提取嵌套字段
        String result = engine.transform("user.name", input);
        assertEquals("Alice", result);
    }

    @Test
    @DisplayName("对象构造表达式")
    void transform_objectConstruction() {
        Map<String, Object> input = Map.of(
                "firstName", "John",
                "lastName", "Doe"
        );

        // JSONata 对象构造
        String result = engine.transform("{ \"fullName\": firstName & \" \" & lastName }", input);
        assertTrue(result.contains("John Doe"));
    }

    @Test
    @DisplayName("语法错误应抛出 TranslationEngineException")
    void transform_syntaxError_throwsException() {
        Map<String, Object> input = Map.of("key", "value");

        TranslationEngineException ex = assertThrows(
                TranslationEngineException.class,
                () -> engine.transform("{{invalid syntax}}", input)
        );
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("null 表达式应返回空字符串")
    void transform_nullExpression_returnsEmpty() {
        assertEquals("", engine.transform(null, Map.of("key", "value")));
    }

    @Test
    @DisplayName("空白表达式应返回空字符串")
    void transform_blankExpression_returnsEmpty() {
        assertEquals("", engine.transform("   ", Map.of("key", "value")));
    }

    @Test
    @DisplayName("访问不存在的字段应返回空字符串")
    void transform_missingField_returnsEmpty() {
        Map<String, Object> input = Map.of("existing", "value");

        // JSONata 访问不存在的字段返回 null -> engine 返回空字符串
        String result = engine.transform("nonExistent", input);
        assertEquals("", result);
    }

    @Test
    @DisplayName("null 输入上下文应正常处理")
    void transform_nullInput_returnsEmpty() {
        // 对 null 输入访问字段，JSONata 返回 null -> 空字符串
        String result = engine.transform("someField", null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("数值结果应序列化为字符串")
    void transform_numericResult_serialized() {
        Map<String, Object> input = Map.of("a", 10, "b", 20);

        String result = engine.transform("a + b", input);
        assertEquals("30", result);
    }

    @Test
    @DisplayName("布尔结果应序列化")
    void transform_booleanResult_serialized() {
        Map<String, Object> input = Map.of("count", 5);

        String result = engine.transform("count > 3", input);
        assertEquals("true", result);
    }

    @Test
    @DisplayName("带绑定变量的转换")
    void transformWithBindings_injectsVariables() {
        Map<String, Object> input = Map.of("base", "hello");
        Map<String, Object> bindings = Map.of("suffix", " world");

        String result = engine.transformWithBindings("base & $suffix", input, bindings);
        assertEquals("hello world", result);
    }

    @Test
    @DisplayName("字符串拼接表达式")
    void transform_stringConcatenation() {
        Map<String, Object> input = Map.of("greeting", "Hello", "name", "World");

        String result = engine.transform("greeting & \", \" & name & \"!\"", input);
        assertEquals("Hello, World!", result);
    }
}
