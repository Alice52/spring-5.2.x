package top.hubby.editor;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.beans.propertyeditors.CharsetEditor;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.text.SimpleDateFormat;

/**
 * @author asd <br>
 * @create 2022-01-18 3:53 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class PropertyEditorTests {

    @Test
    public void testBooleanEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Boolean.class);
        // BooleanEditor editor = new BooleanEditor();
        editor.setAsText(null);
        Boolean value = (Boolean) editor.getValue();

        editor.setAsText("TRue");
        value = (Boolean) editor.getValue();
    }

    /** org.springframework.core.convert.support.StringToBooleanConverter */
    @Test
    public void testCustomBooleanEditor() {
        PropertyEditor editor = new CustomBooleanEditor(true);

        // 这些都是true，不区分大小写
        editor.setAsText("trUe");
        log.info("trUe == true: {}", editor.getAsText());
        editor.setAsText("on");
        log.info("on == true: {}", editor.getAsText());
        editor.setAsText("yes");
        log.info("yes == true: {}", editor.getAsText());
        editor.setAsText("1");
        log.info("1 == true: {}", editor.getAsText());

        // 这些都是false（注意：null并不会输出为false，而是输出空串）
        editor.setAsText(null);
        log.info("null == true: {}", editor.getAsText());

        editor.setAsText("fAlse");
        log.info("fAlse == true: {}", editor.getAsText());
        editor.setAsText("off");
        log.info("off == true: {}", editor.getAsText());
        editor.setAsText("no");
        log.info("no == true: {}", editor.getAsText());
        editor.setAsText("0");
        log.info("0 == true: {}", editor.getAsText());

        // 报错
        editor.setAsText("2");
        log.info(editor.getAsText());
    }

    @Test
    public void testCharsetEditor() {
        // 虽然都行，但建议你规范书写：UTF-8
        PropertyEditor editor = new CharsetEditor();
        editor.setAsText("UtF-8");
        log.info(editor.getAsText()); // UTF-8
        editor.setAsText("utF8");
        log.info(editor.getAsText()); // UTF-8
    }

    /** SimpleDateFormat 线程不安全, Spring 没有使用 */
    @Test
    public void testCustomDateEditor() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        PropertyEditor editor = new CustomDateEditor(format, true);
        editor.setAsText("2020-11-30 09:10:10");
        // 2020-11-30 09:10:10
        log.info(editor.getAsText());

        // null输出空串
        editor.setAsText(null);
        log.info(editor.getAsText());

        // 报错
        editor.setAsText("2020-11-30");
        log.info(editor.getAsText());
    }
}
