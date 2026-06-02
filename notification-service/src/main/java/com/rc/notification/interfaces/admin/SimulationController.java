package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.SimulationService;
import com.rc.notification.interfaces.admin.dto.FullPreviewRequest;
import com.rc.notification.interfaces.admin.dto.FullPreviewResultDto;
import com.rc.notification.interfaces.admin.dto.SimulationRequest;
import com.rc.notification.interfaces.admin.dto.SimulationResultDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSONata 在线仿真端点
 * <p>
 * 提供 JSONata 表达式的在线沙箱仿真能力。
 * 技术人员在管理页面编辑模板时，可实时输入模拟 Payload
 * 并预览四路参数转换结果，阻断错误配置上线。
 */
@RestController
@RequestMapping("/api/v1/admin/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * 单表达式仿真转换
     * <p>
     * 输入: jsonataExpression + mockInputContext
     * 输出: transformedResult 或 error（含错误偏移量）
     */
    @PostMapping("/transform")
    public SimulationResultDto simulate(@Valid @RequestBody SimulationRequest request) {
        return simulationService.simulate(request.getJsonataExpression(), request.getMockInputContext());
    }

    /**
     * 完整请求预览
     * <p>
     * 输入: 完整供应商配置（四路模板）+ mockInputContext
     * 输出: 预览完整 HTTP 请求（resolved URL、Headers、Body），不实际发送网络请求
     */
    @PostMapping("/full-preview")
    public FullPreviewResultDto fullPreview(@Valid @RequestBody FullPreviewRequest request) {
        return simulationService.fullPreview(request);
    }
}
