package top.hubby.editor.support;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.propertyeditors.UUIDEditor;

/**
 *
 *
 * <pre>
 *   1. UUID类型统一交给 UUIDEditor 处理: 包括Cat里面的UUID类型
 *   2. Person 类里面的 Cat 的 UUID 类型, 需要单独特殊处理, 因此格式不一样需要特殊照顾
 * </pre>
 *
 * @author asd <br>
 * @create 2022-01-19 5:12 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class PersonCatUUIDEditor extends UUIDEditor {

    private static final String SUFFIX = "_YourBatman";

    @Override
    public String getAsText() {
        return super.getAsText().concat(SUFFIX);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        text = text.replace(SUFFIX, "");
        super.setAsText(text);
    }
}
