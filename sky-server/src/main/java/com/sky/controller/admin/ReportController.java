package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额数据统计：{},{}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户数据统计：{},{}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单数据统计：{},{}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }

    /**
     * 销量排名Top10
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("销量排名Top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("销量排名Top10统计：{},{}", begin, end);
        return Result.success(reportService.getTop10(begin, end));
    }

    /**
     * 导出Excel报表
     * @param response
     */
    @GetMapping("/export")
    @ApiOperation("导出Excel报表")
    public void export(HttpServletResponse response) {
        try {
            // 1. 查询最近30天的运营数据
            LocalDate end = LocalDate.now();
            LocalDate begin = end.minusDays(30);
            
            // 获取最近30天的营业额、订单、用户数据
            TurnoverReportVO turnoverReport = reportService.getTurnoverStatistics(begin, end);
            OrderReportVO orderReport = reportService.getOrderStatistics(begin, end);
            SalesTop10ReportVO top10Report = reportService.getTop10(begin, end);
            
            // 获取今日运营数据
            BusinessDataVO businessData = workspaceService.getBusinessData();

            // 2. 通过POI将数据写入Excel
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
            
            XSSFWorkbook excel;
            if (inputStream != null) {
                excel = new XSSFWorkbook(inputStream);
                XSSFSheet sheet = excel.getSheetAt(0);
                
                // 填充今日运营数据到模板
                fillBusinessDataToExcel(sheet, businessData);
                
                // 填充历史统计数据到模板
                fillHistoryDataToExcel(sheet, turnoverReport, orderReport, top10Report);
                
            } else {
                excel = new XSSFWorkbook();
                XSSFSheet sheet = excel.createSheet("运营数据报表");
                
                // 创建表头
                XSSFRow row1 = sheet.createRow(0);
                row1.createCell(0).setCellValue("统计项");
                row1.createCell(1).setCellValue("统计结果");
                
                // 填充数据
                XSSFRow row2 = sheet.createRow(1);
                row2.createCell(0).setCellValue("营业额");
                row2.createCell(1).setCellValue(businessData.getTurnover());
                
                XSSFRow row3 = sheet.createRow(2);
                row3.createCell(0).setCellValue("有效订单数");
                row3.createCell(1).setCellValue(businessData.getValidOrderCount());
                
                XSSFRow row4 = sheet.createRow(3);
                row4.createCell(0).setCellValue("订单完成率");
                row4.createCell(1).setCellValue(businessData.getOrderCompletionRate());
                
                XSSFRow row5 = sheet.createRow(4);
                row5.createCell(0).setCellValue("平均客单价");
                row5.createCell(1).setCellValue(businessData.getUnitPrice());
                
                XSSFRow row6 = sheet.createRow(5);
                row6.createCell(0).setCellValue("新增用户数");
                row6.createCell(1).setCellValue(businessData.getNewUsers());
            }

            // 3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=operation_data.xlsx");
            
            excel.write(out);
            
            // 4. 关闭资源
            out.flush();
            out.close();
            excel.close();
            
            log.info("Excel报表导出成功");
        } catch (Exception e) {
            log.error("Excel报表导出失败：", e);
        }
    }

    /**
     * 填充今日运营数据到Excel模板
     * @param sheet
     * @param businessData
     */
    private void fillBusinessDataToExcel(XSSFSheet sheet, BusinessDataVO businessData) {
        // 根据模板结构填充数据，这里需要根据实际模板调整单元格位置
        
        // 示例填充逻辑（需要根据实际模板调整）：
        // 营业额 - 假设在B2单元格
        if (sheet.getRow(1) != null && sheet.getRow(1).getCell(1) != null) {
            sheet.getRow(1).getCell(1).setCellValue(businessData.getTurnover());
        }
        
        // 有效订单数 - 假设在B3单元格
        if (sheet.getRow(2) != null && sheet.getRow(2).getCell(1) != null) {
            sheet.getRow(2).getCell(1).setCellValue(businessData.getValidOrderCount());
        }
        
        // 订单完成率 - 假设在B4单元格
        if (sheet.getRow(3) != null && sheet.getRow(3).getCell(1) != null) {
            sheet.getRow(3).getCell(1).setCellValue(businessData.getOrderCompletionRate());
        }
        
        // 平均客单价 - 假设在B5单元格
        if (sheet.getRow(4) != null && sheet.getRow(4).getCell(1) != null) {
            sheet.getRow(4).getCell(1).setCellValue(businessData.getUnitPrice());
        }
        
        // 新增用户数 - 假设在B6单元格
        if (sheet.getRow(5) != null && sheet.getRow(5).getCell(1) != null) {
            sheet.getRow(5).getCell(1).setCellValue(businessData.getNewUsers());
        }
    }

    /**
     * 填充历史统计数据到Excel模板
     * @param sheet
     * @param turnoverReport
     * @param orderReport
     * @param top10Report
     */
    private void fillHistoryDataToExcel(XSSFSheet sheet, TurnoverReportVO turnoverReport, 
                                      OrderReportVO orderReport, SalesTop10ReportVO top10Report) {
        // 根据模板结构填充历史统计数据
        // 这里需要根据实际模板调整单元格位置
        
        // 示例：填充最近30天总营业额
        if (sheet.getRow(7) != null && sheet.getRow(7).getCell(1) != null && turnoverReport.getTurnoverList() != null) {
            // 将逗号分隔的字符串转换为Double数组，然后求和
            Double totalTurnover = Arrays.stream(turnoverReport.getTurnoverList().split(","))
                    .mapToDouble(Double::parseDouble)
                    .sum();
            sheet.getRow(7).getCell(1).setCellValue(totalTurnover);
        }
        
        // 示例：填充最近30天总订单数
        if (sheet.getRow(8) != null && sheet.getRow(8).getCell(1) != null && orderReport.getOrderCountList() != null) {
            // 将逗号分隔的字符串转换为Integer数组，然后求和
            Integer totalOrders = Arrays.stream(orderReport.getOrderCountList().split(","))
                    .mapToInt(Integer::parseInt)
                    .sum();
            sheet.getRow(8).getCell(1).setCellValue(totalOrders);
        }
        
        // 示例：填充销量Top10数据
        if (top10Report != null && top10Report.getNameList() != null && top10Report.getNumberList() != null) {
            // 将逗号分隔的字符串转换为列表
            List<String> nameList = Arrays.asList(top10Report.getNameList().split(","));
            List<String> numberList = Arrays.asList(top10Report.getNumberList().split(","));
            
            for (int i = 0; i < Math.min(nameList.size(), 10); i++) {
                int rowIndex = 10 + i;
                if (sheet.getRow(rowIndex) != null) {
                    if (sheet.getRow(rowIndex).getCell(0) != null) {
                        sheet.getRow(rowIndex).getCell(0).setCellValue(nameList.get(i));
                    }
                    if (sheet.getRow(rowIndex).getCell(1) != null) {
                        sheet.getRow(rowIndex).getCell(1).setCellValue(Integer.parseInt(numberList.get(i)));
                    }
                }
            }
        }
    }
}