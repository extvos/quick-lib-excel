package plus.extvos.excel.utils;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.extvos.excel.dto.CellMapper;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BaseExcelReadListener<T> implements ReadListener<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseExcelReadListener.class);
    private List<T> list;
    private List<CellMapper> cellMappers;

    public BaseExcelReadListener() {
        this.list = new LinkedList<>();
    }

    public BaseExcelReadListener(List<CellMapper> mappers) {
        this.list = new LinkedList<>();
        this.cellMappers = mappers;
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        log.debug("BaseExcelReadListener::invoke> {}", data.getClass().getName());
        log.debug("BaseExcelReadListener::invoke> {}, {}", data, context);
        if (data instanceof Map) {
            log.debug("BaseExcelReadListener::invoke> Map ==>");
            Map<Object, Object> dm = (Map<Object, Object>) data;
            Map<String, Object> m = new LinkedHashMap<>(dm.size());
            for (int i = 0; i < cellMappers.size(); i++) {
                CellMapper cm = cellMappers.get(i);
                String k = cm.getFieldName();
                Object v = dm.getOrDefault(i, null);
                m.put(k, v);
                log.debug("{}, {} = {}", i, k, v);
            }
            list.add((T) m);
        }
//        list.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.debug("BaseExcelReadListener::doAfterAllAnalysed> {}", context);
    }

    public List<T> getList() {
        return list;
    }
}
