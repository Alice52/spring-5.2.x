package top.hubby.editor.support;

import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyEditorSupport;

/**
 * this is for get-custom-editor between parent and child.
 *
 * @author asd <br>
 * @create 2022-01-19 4:37 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class AnimalPropertyEditor extends PropertyEditorSupport {
    @Override
    public String getAsText() {
        return null;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {}
}
