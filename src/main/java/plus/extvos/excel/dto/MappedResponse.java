package plus.extvos.excel.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MappedResponse<T> {
    private List<T> rows;
    private Map<String, String> columns;

    public MappedResponse(Map<String, String> cells, List<T> data) {
        this.rows = data;
        this.columns = cells;
    }

    public MappedResponse(List<CellMapper> cellMappers, List<T> data) {
        this.rows = data;
        this.setColumns(cellMappers);
    }

    public MappedResponse() {

    }

    public MappedResponse(List<T> data) {
        this.rows = data;
    }

    public MappedResponse(Map<String, String> cells) {
        this.columns = cells;
    }

    public MappedResponse<T> setColumns(List<CellMapper> cellMappers) {
        this.columns = cellMappers.stream().collect(Collectors.toMap(CellMapper::getFieldName, CellMapper::getCellName));
        return this;
    }

    public List<T> getRows() {
        return rows;
    }

    public MappedResponse<T> setRows(List<T> data) {
        this.rows = data;
        return this;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public MappedResponse<T> setColumns(Map<String, String> cells) {
        this.columns = cells;
        return this;
    }
}
