package plus.extvos.excel.dto;

public class CellMapper {
    private String fieldName;
    private String cellName;

    private String groupName;

    public CellMapper() {

    }

    public CellMapper(String fieldName, String cellName) {
        this.fieldName = fieldName;
        this.cellName = cellName;
    }

    public CellMapper(String fieldName, String cellName, String groupName) {
        this.fieldName = fieldName;
        this.cellName = cellName;
        this.groupName = groupName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public CellMapper setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getCellName() {
        return cellName;
    }

    public CellMapper setCellName(String cellName) {
        this.cellName = cellName;
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
