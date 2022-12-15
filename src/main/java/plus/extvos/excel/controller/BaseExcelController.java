package plus.extvos.excel.controller;

import com.alibaba.excel.EasyExcel;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import plus.extvos.common.Result;
import plus.extvos.common.ResultCode;
import plus.extvos.common.Validator;
import plus.extvos.common.exception.ResultException;
import plus.extvos.common.utils.SpringContextHolder;
import plus.extvos.excel.dto.CellMapper;
import plus.extvos.excel.dto.MappedResponse;
import plus.extvos.excel.utils.BaseExcelReadListener;
import plus.extvos.logging.annotation.Log;
import plus.extvos.logging.annotation.type.LogAction;
import plus.extvos.logging.annotation.type.LogLevel;
import plus.extvos.restlet.QuerySet;
import plus.extvos.restlet.config.RestletConfig;
import plus.extvos.restlet.controller.BaseController;
import plus.extvos.restlet.service.BaseService;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mingcai SHEN
 */
public abstract class BaseExcelController<T, S extends BaseService<T>> extends BaseController<T, S> {

    private static final Logger log = LoggerFactory.getLogger(BaseExcelController.class);

    /**
     * get the bundled base service
     *
     * @return bundled base service.
     */
    public abstract S getService();

    private List<CellMapper> cellMappers = null;

    public List<CellMapper> defaultCellMappers() {
        if (null == cellMappers) {
            cellMappers = new LinkedList<>();
            for (Field f : getService().getGenericType().getDeclaredFields()) {
                ApiModelProperty anno = f.getAnnotation(ApiModelProperty.class);
                if (anno != null) {
                    if (anno.hidden()) {
                        continue;
                    }
                    cellMappers.add(new CellMapper(f.getName(), anno.value()));
                } else {
                    cellMappers.add(new CellMapper(f.getName(), f.getName()));
                }
            }

        }
        return cellMappers;
    }

    protected List<String> buildRow(T entity, List<CellMapper> cellMappers) {
        List<String> ret = new ArrayList<>();
        try {
            for (CellMapper c : cellMappers) {
                Field f = entity.getClass().getDeclaredField(c.getFieldName());
                f.setAccessible(true);
                ret.add(f.get(entity).toString());
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @ApiOperation(value = "按查询条件导出数据到Excel", notes = "查询条件组织，请参考： https://github.com/extvos/quick-lib-restlet/blob/develop/README.md")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "__page", required = false, defaultValue = ""),
            @ApiImplicitParam(name = "__pageSize", required = false, defaultValue = ""),
            @ApiImplicitParam(name = "__orderBy", required = false, defaultValue = ""),
    })
    @GetMapping("/_excel")
    @Log(action = LogAction.SELECT, level = LogLevel.NORMAL, comment = "Export to excel")
    public ModelAndView exportToExcel(
            @ApiParam(hidden = true) @PathVariable(required = false) Map<String, Object> pathMap,
            @ApiParam(hidden = true) @RequestParam(required = false) Map<String, Object> queryMap,
            @ApiParam(hidden = true) HttpServletResponse response) throws ResultException {
        log.debug("BaseExcelController<{}>::exportToExcel:1 parameters: {} {}", getService().getClass().getName(), queryMap, pathMap);
        RestletConfig config = SpringContextHolder.getBean(RestletConfig.class);
        QuerySet<T> qs = getService().buildQuerySet(config, null, null, pathMap, queryMap);
        String modelName = getService().getGenericType().getSimpleName();
        ApiModel m = getService().getGenericType().getAnnotation(ApiModel.class);
        if (null != m) {
            modelName = m.value();
        }
        selectToExcel(qs, defaultCellMappers(), modelName + ".xlsx", modelName, response);
        return null; //Result.data("OK").success();
    }

    @ApiOperation(value = "从Excel导入以及插入记录")
    @PostMapping("/_excel")
    @Log(action = LogAction.CREATE, level = LogLevel.IMPORTANT, comment = "Import from Excel to CREATE")
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Result<MappedResponse<T>> importFromExcel(
            @ApiParam(hidden = true) @PathVariable(required = false) Map<String, Object> pathMap,
            @RequestBody MultipartFile file) throws ResultException {
        log.debug("importFromExcel:> {}, {}", pathMap, file.getOriginalFilename());
//        EasyExcel.read(file.getInputStream()).sheet().
//        Result<T> ret = Result.data(record).success(ResultCode.CREATED);
//        ret.setCount((long) n);
//        return ret;
        BaseExcelReadListener<T> listener = new BaseExcelReadListener<T>(defaultCellMappers());
        try {
            EasyExcel.read(file.getInputStream())
                    .sheet()
                    .head(buildHead(defaultCellMappers()))
//                    .registerConverter(new BaseExcelConverter<T>(getGenericType(), defaultCellMappers()))
                    .registerReadListener(listener).doRead();
            MappedResponse<T> resp = new MappedResponse<T>(cellMappers, listener.getList());
            return Result.data(resp).success();
        } catch (IOException e) {
            throw ResultException.internalServerError(e.getMessage());
        }
    }

    @ApiOperation(value = "插入EXCEL读取记录", notes = "查询条件组织，请参考： https://github.com/extvos/quick-lib-restlet/blob/develop/README.md")
    @PostMapping("/_excel/import")
    @Log(action = LogAction.CREATE, level = LogLevel.IMPORTANT, comment = "Generic CREATE")
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Result<?> importRecords(
            @ApiParam(hidden = true) @PathVariable(required = false) Map<String, Object> pathMap,
            @RequestBody T[] records) throws ResultException {
        log.debug("importRecords:1> {}", pathMap);
        if (null != records && records.length > 0) {
            log.debug("importRecords:2> {}", records.length);
            if (Validator.notEmpty(pathMap)) {
                for (T rec : records) {
                    for (String k : pathMap.keySet()) {
                        updateFieldValue(rec, k, pathMap.get(k));
                    }
                }
            }
            records = preInsert(records);
            int n = getService().insert(Arrays.asList(records));
            postInsert(records);
            return Result.data(records).setCount((long) n).success(ResultCode.CREATED);
        } else {
            throw ResultException.badRequest("invalid request data");
        }
    }

    public T[] preInsert(T[] entities) throws ResultException {
        if (!creatable()) {
            throw ResultException.forbidden();
        }
        return entities;
    }

    public void postInsert(T[] entities) throws ResultException {

    }

    public List<List<String>> prepareRows(List<T> records) throws ResultException {
        List<List<String>> rows = records.stream().map(e -> buildRow(e, defaultCellMappers())).collect(Collectors.toList());
        return rows;
    }

    public List<List<String>> buildHead(@NotNull List<CellMapper> mappers) {
        return mappers.stream().map(c -> {
            if (c.getGroupName() != null) {
                return Arrays.asList(c.getGroupName(), c.getCellName());
            } else {
                return Collections.singletonList(c.getCellName());
            }
        }).collect(Collectors.toList());
    }

    public void selectToExcel(
            @NotNull QuerySet<T> querySet,
            @NotNull List<CellMapper> mappers,
            @NotNull String depositionFilename,
            @NotNull String sheetName,
            HttpServletResponse response) throws ResultException {

        List<T> records = getService().selectByMap(querySet);
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;" + depositionFilename);
        try {
            EasyExcel.write(response.getOutputStream())
                    .sheet(sheetName)
                    .head(buildHead(defaultCellMappers()))
                    .doWrite(prepareRows(records));
        } catch (IOException e) {
            throw ResultException.internalServerError(e.getMessage());
        }

    }

}
